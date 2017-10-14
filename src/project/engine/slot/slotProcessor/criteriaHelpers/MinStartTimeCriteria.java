/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package project.engine.slot.slotProcessor.criteriaHelpers;

import project.engine.data.Slot;
import project.engine.data.Window;

/**
 *
 * @author dmieter
 */
public class MinStartTimeCriteria implements ICriteriaHelper {

    @Override
    public double getCriteriaValue(Window w) {
        double startTime = Double.NEGATIVE_INFINITY;
        for(int i=0;i<w.slotsNeed;i++){
            Slot s = w.slots.get(i);
            if(startTime < s.start)
                startTime = s.start;
        }
        
        return -startTime;
    }

    @Override
    public String getDescription() {
        return "min Start";
    }
    
}
