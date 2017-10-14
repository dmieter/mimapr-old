/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package project.engine.slot.slotProcessor.criteriaHelpers;

import java.util.ArrayList;
import java.util.Iterator;
import project.engine.data.Slot;
import project.engine.data.Window;

/**
 *
 * @author Magica
 */
public class MinRunTimeCriteria implements ICriteriaHelper{

    public double getCriteriaValue(Window w) {
        
        if(w.squareWindow){
            return -w.length;
        }
        
        double criteria = 0;
        w.sortSlotsByCost();
        Window extendingWindow = w.clone();
        if(w.slotsNeed < w.slots.size()){

            getInitWindows(w, extendingWindow);

            int cycles = 0;
            while(tryToMakeChange(w, extendingWindow)){         //logic here
                cycles++;
                if(cycles > 1000) throw new RuntimeException("CYCLES!!!");
            }
        }

        int i = getLongestSlot(w);
        Slot longestSlot = w.slots.get(i);

        criteria = -longestSlot.getVolumeTime(w);           // the shorter the time - the higher the criteria!
        
        return criteria;
    }

    protected void getInitWindows(Window resultW, Window extendingW){
        extendingW.slots.clear();
        resultW.sortSlotsByCost();
        
        int i=0;
        for(Iterator<Slot> it = resultW.slots.iterator();it.hasNext();){
            Slot s = it.next();
            if(i >= resultW.slotsNeed){
                extendingW.slots.add(s);
                it.remove();
            }
            i++;
        }

    }

    private boolean tryToMakeChange(Window w, Window extendingW){            //changing longest slot to the cheapiest shorter
        boolean success = false;
        int i1 = getLongestSlot(w);
        Slot s1 = w.slots.get(i1);

        int i2 = getChipestSuitableSlot(extendingW, s1.getVolumeTime(w));
        if(i2<0)
            return false;
        Slot s2 = extendingW.slots.get(i2);

        double possibleNextSum = w.getTotalVolumeCost() - s1.getVolumeCost(w) +
                                s2.getVolumeCost(w);
        if(possibleNextSum <= w.maxCost){
            success = true;
            extendingW.slots.add(s1);
            w.slots.remove(s1);
            w.slots.add(s2);
            extendingW.slots.remove(s2);
        }
        return success;
    }

    private int getLongestSlot(Window w){
        double maxLength = 0;
        int num = 0;
        for(int i=0;i<w.slots.size();i++){
            Slot s = w.slots.get(i);
            double l = s.getVolumeTime(w);
            if(maxLength <= l){
                maxLength = l;
                num = i;
            }
        }
        return num;
    }

    private int getChipestSuitableSlot(Window w, double lengthLimit){
        for(int i=0;i<w.slots.size();i++){
            Slot s = w.slots.get(i);
            if(s.getVolumeTime(w) < lengthLimit)
                return i;
        }
        return -1;
    }
    
    public String getDescription(){
        return "min Runtime";
    }
}
