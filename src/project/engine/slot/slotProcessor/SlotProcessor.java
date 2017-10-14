/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package project.engine.slot.slotProcessor;

import java.util.*;
import java.util.ArrayList;

import project.engine.data.ResourceRequest;
import project.engine.data.ResourceLine;
import project.engine.data.*;
import project.engine.alternativeStats.AlternativeStats;

public class SlotProcessor
{

    //retrieve sorted slots from the environment
    protected ArrayList<Slot> getSortedSlots(VOEnvironment environment, SlotProcessorSettings settings)
    {
        ArrayList<Slot> slots = new ArrayList<Slot>();
        for (ResourceLine rLine: environment.resourceLines)
        {
           slots.addAll(prepareSlots(rLine.getSlots(), settings));
        }
        //sort by start time
        Collections.sort(slots,new Comparator<Slot>()
       {
            public final int compare ( Slot a, Slot b )
            {
               return Double.compare(a.start, b.start);
            }
        });
        return slots;
    }

    //trim slots by current cycle
    protected ArrayList<Slot> prepareSlots(ArrayList<Slot> slots, SlotProcessorSettings settings)
    {
        ArrayList<Slot> _slots = new ArrayList<Slot>();
        for (Slot slot: slots)
        {
            if(slot.getLength()<=0 ||
               slot.end <= settings.cycleStart ||
               slot.start >= settings.cycleStart + settings.cycleLength)
            {
                continue;
            }
            Slot _slot = slot.clone();
            if(_slot.start <= settings.cycleStart)
            {
                _slot.start = settings.cycleStart;
            }
            if(_slot.end > settings.cycleStart + settings.cycleLength)
            {
                _slot.end = settings.cycleStart + settings.cycleLength;
            }
            _slots.add(_slot);
        }
        return _slots;
    }

    protected boolean checkSlotForWindow(Slot slot, Window window, SlotProcessorSettings settings)
    {
        if (settings.algorithmType.equals("NORMAL"))
        {
            return slot.getLength() >= slot.getVolumeTime(window.volume) &&
                slot.getPrice() <= window.priceMax &&
                slot.getSpeed() >= window.speed;
        }
        if (settings.algorithmType.equals("MODIFIED"))
        {
             return slot.getLength() >= slot.getVolumeTime(window.volume) && slot.getSpeed() >= window.speed;
        }
        else
            return false;
    }


    protected boolean checkCostForWindow(Window window)
    {
        if (window.slots.size() < window.slotsNeed)
            return false;
        //сортировка слотов по цене - побочное действие всей функции
        window.sortSlotsByCost();
        double s = 0;
        for (int i=0; i<window.slotsNeed; i++)
        {
            s += window.slots.get(i).getVolumeCost(window.volume);
        }
        return s <= window.maxCost;
    }

    protected ArrayList<Slot> takeSublist(ArrayList<Slot> source, int count)
    {
        ArrayList<Slot> result = new ArrayList<Slot>();
        for (int i=0; i<count; i++)
        {
            result.add(source.get(i));
        }
        return result;
    }


    //find satisfying window
    protected Window FindNextWindow(ResourceRequest rt, ArrayList<Slot> slots, SlotProcessorSettings settings)
    {
        Window window = initializeWindow(rt);
        double _start;
        for (int i=0; i<slots.size(); i++)
        {
            Slot slot = slots.get(i);
            if (checkSlotForWindow(slot, window, settings))
            {
                window.slots.add(slot.clone());
                _start = slot.start;
                //ex-Checkwindow
                int j = 0;
                while (j < window.slots.size())
                {
                    Slot checkedSlot = window.slots.get(j);
                    if (checkedSlot.end - _start < checkedSlot.getVolumeTime(window.volume))
                        window.slots.remove(j);
                    else
                        j++;
                }               
            }
            //проверка на достижение числа слотов и стоимости окна (в случае мод. алгоритма)
            if ( (window.slots.size() >= window.slotsNeed) && settings.algorithmType.equals("NORMAL") ||
                settings.algorithmType.equals("MODIFIED") && checkCostForWindow(window))
                break;
        }
        //в случае мод. алгоритма отрезаем лишние слоты
        if (settings.algorithmType.equals("MODIFIED") && window.slots.size()>window.slotsNeed)
        {
           //window.slots = (ArrayList<Slot>)window.slots.subList(0,window.slotsNeed);
            window.slots = takeSublist(window.slots, window.slotsNeed);
        }
        //установка старта окна
        double maxStart = -1;
        for (int i =0; i<window.slots.size(); i++)
        {
           if (window.slots.get(i).start > maxStart)
               maxStart = window.slots.get(i).start;
        }
        window.start = maxStart;
       //окно не найдено, если:
       //1)обычный и мод. алгоритм - не хватает слотов
       //2)мод. алгоритм - слишком дорогое окно
       //число слотов в окне в этой точке всегда меньше или равно требуемому
       if (window.slots.size() < window.slotsNeed ||
          settings.algorithmType.equals("MODIFIED") && window.getTotalVolumeCost() > window.maxCost)
       {
           window = null;
       }
       return window;
    }
    
    protected Slot findSlotById(ArrayList<Slot> slots, long id)
    {
        for (Slot s: slots)
        {
            if (s.id == id)
                return s;
        }
        return null;
    }


     protected void subtractWindowFromSlots(Window window, ArrayList<Slot> slots)
     {
        for(Slot windowSlot: window.slots)
        {
            Slot listSlot = findSlotById(slots, windowSlot.id);
            if(listSlot != null)
            {
               //var1: a slot is shifted to the right of the source slot
                if (windowSlot.start > listSlot.start &&
                    windowSlot.start < listSlot.end &&
                    windowSlot.end == listSlot.end)
                {
                    listSlot.end = windowSlot.start;
                    listSlot.refreshId();
                }
                //var 2: a slot is within the boundary of the source slot
                else if (windowSlot.start> listSlot.start &&
                        windowSlot.start < listSlot.end &&
                        windowSlot.end < listSlot.end)
                {
                    double _end = listSlot.end;
                    listSlot.end = windowSlot.start;
                    listSlot.refreshId();
                    Slot tailSlot = new Slot(windowSlot.end, _end, listSlot.resourceLine);
                    slots.add(tailSlot);
                }
                //var 3: a slot is shorter than the source one
                else if (windowSlot.start == listSlot.start && windowSlot.end < listSlot.end)
                {
                    listSlot.start = windowSlot.end;
                    listSlot.refreshId();
                }
                //var 4: slots coincide
                else if (windowSlot.start == listSlot.start && windowSlot.end == listSlot.end)
                {
                    slots.remove(listSlot);
                }
            }
         }
         //sort by start time
         Collections.sort(slots,new Comparator<Slot>()
         {
            public final int compare ( Slot a, Slot b )
            {
               return Double.compare(a.start, b.start);
            }
         });
    }

    protected Window initializeWindow(ResourceRequest rt)
    {
        return new Window(rt);
    }


    protected int deleteIdenticalAlternatives(UserJob job)
    {
        int c = 0;
        int i = 0;
        while (i<job.alternatives.size())
        {
            Alternative a = job.alternatives.get(i);
            for (int j=i+1; j<job.alternatives.size(); j++)
            {
                Alternative a1 = job.alternatives.get(j);
                if (a1.getCost() == a.getCost() &&
                    a1.getLength() == a.getLength() &&
                    a1.getUserRating() == a.getUserRating()
                        /* &&
                    a.getStart() <= a1.getStart()*/)
                {
                    job.alternatives.remove(j);
                    j--;
                    c++;
                }
            }
            i++;
        }
        for (i=0; i<job.alternatives.size(); i++)
        {
            job.alternatives.get(i).num = i;
        }
        return c;
    }

    protected void prepareRequests(ArrayList<UserJob> jobs)
    {
        for (UserJob job: jobs)
        {
            job.alternatives.clear();
        }
    }
    //serach for all suitable alternatives
    public SlotProcessorResult findAlternatives(ArrayList<UserJob> jobs, VOEnvironment environment, SlotProcessorSettings settings)
    {
        return findAlternatives(jobs, environment, settings, Integer.MAX_VALUE);
    }

    public SlotProcessorResult findAlternatives(ArrayList<UserJob> jobs, ArrayList<Slot> slots, SlotProcessorSettings settings)
    {
        return findAlternatives(jobs, slots, settings, Integer.MAX_VALUE);
    }
        //returns total count of found alternatives
    //searches not more than maxAlternatives alternatives for a single job
    public SlotProcessorResult findAlternatives(ArrayList<UserJob> jobs, VOEnvironment environment, SlotProcessorSettings settings, int maxAlternatives)
    {
        ArrayList<Slot> slots = getSortedSlots(environment, settings);
        return findAlternatives(jobs, slots, settings, maxAlternatives);
    }

    public SlotProcessorResult findAlternatives(ArrayList<UserJob> jobs, ArrayList<Slot> slots, SlotProcessorSettings settings, int maxAlternatives)
    {
        boolean foundone = true;           // if we found at least one window inside cycle
        prepareRequests(jobs);
        HashSet<Integer> excludeSet = new HashSet<Integer>();
        SlotProcessorResult result = new SlotProcessorResult();
        result.slotsProcessed = slots.size();
        int searchCycleCounter = 0;
        while(foundone)
        {
            //while we found at least one window inside last cycle
            foundone = false;
            for (UserJob job: jobs)
            {
                //if no window was found at last cycle, stop search
                if (excludeSet.contains(job.id))
                    continue;
                Window w = FindNextWindow(job.resourceRequest, slots, settings);
                result.FindNextWindowCalls++;
                if(w != null)
                {
                    prettyCutWindow(w);
                    subtractWindowFromSlots(w, slots);  //updating slots
                    job.addAlternative(new Alternative(w));
                    result.alternativesFound++;
                    foundone = true;
                }
                else
                    excludeSet.add(job.id);
            }

            searchCycleCounter++;       //another searhc cycle passed
            if(searchCycleCounter==maxAlternatives)
                break;              //we got the needed number of alternatives, break
        }
        result.alternativesCleaned = 0;
        if (settings.clean)
        {
            for (UserJob job: jobs)
            {
                int c = deleteIdenticalAlternatives(job);
                result.alternativesCleaned += c;
            }
        }
        if (settings.countStats)
        {
            AlternativeStats aStats = new AlternativeStats();
            aStats.blankStats();
            aStats.processResults(jobs);
            aStats.addBatchToLog(jobs);
            //aStats.addEnvironmentToLog(environment, settings.cycleStart, settings.cycleStart+settings.cycleLength);
            result.altStats = aStats;
        }
        return result;
    }
    
    
    protected void prettyCutWindow(Window w) {
        /* 1. get only the first slotsneed slots */
        if (w != null) {
            w.slots = takeSublist(w.slots, w.slotsNeed);     //getting the needed count of slots (first slotsneed slots will represent the best combination)
            //установка старта окна
            double maxStart = Double.NEGATIVE_INFINITY;
            for (int i = 0; i < w.slots.size(); i++) {
                if (w.slots.get(i).start > maxStart) {
                    maxStart = w.slots.get(i).start;
                }
            }
            w.start = maxStart;
        }

        /* 2. cut slots according to the volume */
        //slot alignment
        double maxSlotLength = Integer.MIN_VALUE;
        for (Slot s : w.slots) {
            s.start = w.start;
            double perfomanceTime = s.getVolumeTime(w.volume);
            s.end = w.start + perfomanceTime;
            if (perfomanceTime > maxSlotLength) {
                maxSlotLength = perfomanceTime;
            }
        }
        w.length = maxSlotLength;
    }

}
