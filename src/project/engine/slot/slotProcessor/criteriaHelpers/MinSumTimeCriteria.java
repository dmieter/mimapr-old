/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package project.engine.slot.slotProcessor.criteriaHelpers;

import project.engine.data.Slot;
import project.engine.data.Window;

/**
 *
 * @author Magica
 */
public class MinSumTimeCriteria implements ICriteriaHelper{

    public double getCriteriaValue(Window w) {
        double sumTime = 0;
        w.sortSlotsByCost();
        for(int i = 0; i<w.slotsNeed;i++){
            Slot s = (Slot)w.slots.get(i);
            double sLength = s.getVolumeTime(w);
            sumTime+=sLength;
        }

        return -sumTime;
    }

    public String getDescription(){
        return "min ProcTime";
    }
}
