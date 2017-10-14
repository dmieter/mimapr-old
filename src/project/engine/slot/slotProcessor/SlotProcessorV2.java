package project.engine.slot.slotProcessor;

import java.util.ArrayList;
import java.util.HashSet;
import project.engine.alternativeStats.AlternativeStats;
import project.engine.data.Alternative;
import project.engine.data.Slot;
import project.engine.data.UserJob;
import project.engine.data.Window;
import project.engine.slot.slotProcessor.criteriaHelpers.ExecutionSimilarityCriteria;
import project.engine.slot.slotProcessor.criteriaHelpers.ICriteriaHelper;
import project.engine.slot.slotProcessor.criteriaHelpers.MaxSumCostCriteria;
import project.engine.slot.slotProcessor.criteriaHelpers.MinFinishTimeCriteria;
import project.engine.slot.slotProcessor.criteriaHelpers.MinRunTimeCriteria;
import project.engine.slot.slotProcessor.criteriaHelpers.MinSumCostCriteria;

/**
 *
 * @author Magica
 */
public class SlotProcessorV2 extends SlotProcessor {

    protected Window bestWindow = null;
    protected double bestCriteriaValue = Double.NEGATIVE_INFINITY;

    protected double getCriteria(Window w, ICriteriaHelper requestCriteria, SlotProcessorSettings settings) {         //could be window with extended number of slots
        if (requestCriteria != null) {
            return requestCriteria.getCriteriaValue(w);
        }
        if (settings.criteriaHelper != null) {
            return settings.criteriaHelper.getCriteriaValue(w);
        } else {
            return 0;
        }
    }

    @Override
    public SlotProcessorResult findAlternatives(ArrayList<UserJob> jobs, ArrayList<Slot> slots, SlotProcessorSettings settings, int maxAlternatives) {
        if (settings.findAllPossibleAlternativesForJob) {
            System.out.println("Warning. Searching all possible intersecting alternatives. Be careful.");
            maxAlternatives = 1; /* One iteration is enough to find all alternatives */
        }

        boolean foundone = true;           // if we found at least one window inside cycle
        prepareRequests(jobs);
        HashSet<Integer> excludeSet = new HashSet<Integer>();
        SlotProcessorResult result = new SlotProcessorResult();
        result.slotsProcessed = slots.size();
        int searchCycleCounter = 0;
        while (foundone) {
            //while we found at least one window inside last cycle
            foundone = false;
            for (UserJob job : jobs) {
                //if no window was found at last cycle, stop search
                if (excludeSet.contains(job.id)) {
                    continue;
                }
                Window w = null;

                if ("EXTREME".equalsIgnoreCase(settings.algorithmConcept)) {
                    w = FindExtremeWindow(job, slots, settings);
                }
                if ("COMMON".equalsIgnoreCase(settings.algorithmConcept)) {
                    w = FindNextWindow(job, slots, settings);
                }

                result.FindNextWindowCalls++;
                if (w != null) {
                    /* formatting window according to size, volume*/
                    prettyCutWindow(w);

                    if (!settings.findAllPossibleAlternativesForJob) { /* if we don't need all alternatives intersected */
                        /* we need to substract the window we found from the environment resources */

                        subtractWindowFromSlots(w, slots);  //updating slots in the environment
                        
                    } else {
                        /* one cycle for each job is enough to find all possible alternatives */
                        excludeSet.add(job.id);
                    }

                    job.addAlternative(new Alternative(w));
                    result.alternativesFound++;
                    foundone = true;
                } else {
                    excludeSet.add(job.id);
                }
            }

            searchCycleCounter++;       //another searhc cycle passed
            if (searchCycleCounter == maxAlternatives) {
                break;              //we got the needed number of alternatives, break
            }
            if (searchCycleCounter > 1000) {      //FAIL
                int b = 0;
            }
        }
        result.alternativesCleaned = 0;
        if (settings.clean) {
            for (UserJob job : jobs) {
                int c = deleteIdenticalAlternatives(job);
                result.alternativesCleaned += c;
            }
        }
        if (settings.countStats) {
            AlternativeStats aStats = new AlternativeStats();
            aStats.blankStats();
            aStats.processResults(jobs);
            aStats.addBatchToLog(jobs);
            //aStats.addEnvironmentToLog(environment, settings.cycleStart, settings.cycleStart+settings.cycleLength);
            result.altStats = aStats;
        }
        return result;
    }

    protected Window FindExtremeWindow(UserJob job, ArrayList<Slot> slots, SlotProcessorSettings settings) {
        bestWindow = null;
        bestCriteriaValue = Double.NEGATIVE_INFINITY;

        Window window = initializeWindow(job.resourceRequest);
        double _start;
        for (int i = 0; i < slots.size(); i++) {
            //System.out.println("checking slot "+(i+1)+" of " + slots.size() + " for job" + job.name);
            Slot slot = slots.get(i);
            if (checkSlotForWindow(slot, window, settings)) {
                window.slots.add(slot.clone());
                _start = slot.start;
                //ex-Checkwindow
                int j = 0;
                while (j < window.slots.size()) {
                    Slot checkedSlot = window.slots.get(j);
                    if (checkedSlot.end - _start < checkedSlot.getVolumeTime(window.volume)) {
                        window.slots.remove(j);
                    } else {
                        j++;
                    }
                }

                //проверка на достижение числа слотов и стоимости окна (в случае мод. алгоритма)
                if ((window.slots.size() >= window.slotsNeed) && settings.algorithmType.equals("NORMAL")
                        || settings.algorithmType.equals("MODIFIED") && checkCostForWindow(window)) {       //if another window is ready

                    Window tempWindow = window.clone();
                    double curCriteriaValue = getCriteria(tempWindow, job.resourceRequest.criteria, settings);      //checking its criteria, tempWIndow should become best available for now

                    /* adding all possible intersecting alternatives to job */
                    if (settings.findAllPossibleAlternativesForJob) {
                        Window curWindow1 = window.clone();
                        ICriteriaHelper criteria = new MinRunTimeCriteria();
                        criteria.getCriteriaValue(curWindow1);
                        prettyCutWindow(curWindow1);
                        job.addAlternative(new Alternative(curWindow1));
                        
                        Window curWindow2 = window.clone();
                        criteria = new MinSumCostCriteria();
                        criteria.getCriteriaValue(curWindow2);
                        prettyCutWindow(curWindow2);
                        job.addAlternative(new Alternative(curWindow2));
                        
                        Window curWindow3 = window.clone();
                        criteria = new MaxSumCostCriteria();
                        criteria.getCriteriaValue(curWindow3);
                        prettyCutWindow(curWindow3);
                        job.addAlternative(new Alternative(curWindow3));
                        
                        //System.out.println("adding possible alternative #"+job.alternatives.size());
                    }

                    if (settings.check4PreviousAlternatives && !isWindowUnique(job, window, settings)) //checking for distance between alternatives
                    {
                        continue;       //next slot and iteration
                    }                    //else or last window with last slot here:
                    if (bestCriteriaValue < curCriteriaValue) {
                        bestWindow = tempWindow;                            // if it's better than current best - we have new current best
                        bestCriteriaValue = curCriteriaValue;
                        //System.out.println("\n NEW BEST CRITERIA VALUE IS "+bestCriteriaValue);
                    }
                }                                                           //continue searching foe windows with potentially better criteria, next cycle of adding slots
            }
        }

        //End of slots, best Window is in bestWindow variable (if it's not null)
        //We need to make it look good
//        see prettyCutWindow
//        if (bestWindow != null) {
//            bestWindow.slots = takeSublist(bestWindow.slots, bestWindow.slotsNeed);     //getting the needed count of slots (first slotsneed slots will represent the best combination)
//            //установка старта окна
//            double maxStart = -1;
//            for (int i = 0; i < bestWindow.slots.size(); i++) {
//                if (bestWindow.slots.get(i).start > maxStart) {
//                    maxStart = bestWindow.slots.get(i).start;
//                }
//            }
//            bestWindow.start = maxStart;
//        }
        return bestWindow;
    }

    //find satisfying window
    protected Window FindNextWindow(UserJob job, ArrayList<Slot> slots, SlotProcessorSettings settings) {
        Window window = initializeWindow(job.resourceRequest);
        double _start;
        for (int i = 0; i < slots.size(); i++) {
            Slot slot = slots.get(i);
            if (checkSlotForWindow(slot, window, settings)) {
                window.slots.add(slot.clone());
                _start = slot.start;
                //ex-Checkwindow
                int j = 0;
                while (j < window.slots.size()) {
                    Slot checkedSlot = window.slots.get(j);
                    if (checkedSlot.end - _start < checkedSlot.getVolumeTime(window.volume)) {
                        window.slots.remove(j);
                    } else {
                        j++;
                    }
                }
            }
            //проверка на достижение числа слотов и стоимости окна (в случае мод. алгоритма)
            if ((window.slots.size() >= window.slotsNeed) && settings.algorithmType.equals("NORMAL")
                    || settings.algorithmType.equals("MODIFIED") && checkCostForWindow(window)) {
                if (settings.check4PreviousAlternatives && !isWindowUnique(job, window, settings)) //checking for distance between alternatives
                {
                    continue;
                } else {
                    break;
                }
            }
        }
        //в случае мод. алгоритма отрезаем лишние слоты
        if (settings.algorithmType.equals("MODIFIED") && window.slots.size() > window.slotsNeed) {
            //window.slots = (ArrayList<Slot>)window.slots.subList(0,window.slotsNeed);
            window.slots = takeSublist(window.slots, window.slotsNeed);
        }
        //установка старта окна
        double maxStart = -1;
        for (int i = 0; i < window.slots.size(); i++) {
            if (window.slots.get(i).start > maxStart) {
                maxStart = window.slots.get(i).start;
            }
        }
        window.start = maxStart;
        //окно не найдено, если:
        //1)обычный и мод. алгоритм - не хватает слотов
        //2)мод. алгоритм - слишком дорогое окно
        //число слотов в окне в этой точке всегда меньше или равно требуемому
        if (window.slots.size() < window.slotsNeed
                || settings.algorithmType.equals("MODIFIED") && window.getTotalVolumeCost() > window.maxCost) {
            window = null;
        }

        if (settings.check4PreviousAlternatives && !isWindowUnique(job, window, settings)) {        //checking for distance between alternatives
            return null;    //final window isn't unique
        }
        return window;
    }

    protected boolean isWindowUnique(UserJob job, Window w, SlotProcessorSettings sps) {
        double length = 0;
        double cost = 0;
        double distance = 0;
        boolean res = true;

        if (job.alternatives.isEmpty()) {
            return true;
        }
        if (w == null) {
            return false;
        }

        for (int i = 0; i < w.slotsNeed; i++) {
            Slot s = w.slots.get(i);
            double performanceTime = s.getVolumeTime(w.volume);
            cost += s.getVolumeCost(w.volume);
            length += performanceTime;                //summarizing cost and length of this temp window

        }

        if (cost != 0 && length != 0) {
            for (Alternative a : job.alternatives) {
                double timeDelta = (length - a.getLength()) / length;
                if (timeDelta < 0) {
                    timeDelta *= -1;
                }

                double costDelta = (cost - a.getCost()) / cost;
                if (costDelta < 0) {
                    costDelta *= -1;
                }

                distance = costDelta + timeDelta;
                if (distance < sps.alternativesMinDistance) {
                    return false;       //found Alternative that is nearer then minimum distance - then we dont need this window
                }
            }
        }
        return res;
    }

}
