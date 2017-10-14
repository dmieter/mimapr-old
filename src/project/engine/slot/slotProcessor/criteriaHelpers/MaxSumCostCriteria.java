/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package project.engine.slot.slotProcessor.criteriaHelpers;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import project.engine.data.Slot;
import project.engine.data.Window;

/**
 *
 * @author Magica
 */
public class MaxSumCostCriteria implements ICriteriaHelper {

    @Override
    public double getCriteriaValue(Window w) {
        double sum = 0;
        
        if(w.slotsNeed < w.slots.size()){
            w.sortSlotsByCost();
            List<Slot> expensiveSlots = getExtraSlots(w);
            
            int cyclesNum = 0;
            while(tryToMakeChange(w, expensiveSlots)){
                cyclesNum++;
                if(cyclesNum > 1000){
                    throw new RuntimeException("Something wrong when checking for max cost window");
                }
            }
        }
        
        for (int i = 0; i < w.slotsNeed; i++) {
            sum += w.slots.get(i).getVolumeCost(w);
        }
        return sum;
    }

    @Override
    public String getDescription() {
        return "max Cost";
    }

    protected List<Slot> getExtraSlots(Window w){
        List<Slot> extraSlots = new ArrayList<>();
        int i = 0;
        for (Iterator<Slot> it = w.slots.iterator();it.hasNext();) {
            Slot s = it.next();
            if(i >= w.slotsNeed){
                extraSlots.add(s);
                it.remove();
            }
            i++;
        }
        
        return extraSlots;
    }
    
    protected boolean tryToMakeChange(Window w, List<Slot> extraSlots) {
        
        Slot cheapestSlot = w.slots.get(0); /* as slots sorted by cost */
        Double cheapestCost = cheapestSlot.getVolumeCost(w);

        Double currentWindowCost = w.getTotalVolumeCost();

        for (Iterator<Slot> it = extraSlots.iterator();it.hasNext();) {
            Slot expensiveSlot = it.next();
            if(currentWindowCost - cheapestCost + expensiveSlot.getVolumeCost(w) <= w.maxCost){
                w.slots.remove(cheapestSlot);
                w.slots.add(expensiveSlot);
                it.remove();
                return true;
            }
        }
        
        return false;
    }
}
