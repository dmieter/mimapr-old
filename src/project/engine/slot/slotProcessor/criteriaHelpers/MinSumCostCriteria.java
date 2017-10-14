/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package project.engine.slot.slotProcessor.criteriaHelpers;

import project.engine.data.Window;

/**
 *
 * @author Magica
 */
public class MinSumCostCriteria implements ICriteriaHelper {

    public double getCriteriaValue(Window w) {
        double sum = 0;
        w.sortSlotsByCost();
        for(int i=0; i<w.slotsNeed;i++){
            sum+=w.slots.get(i).getVolumeCost(w);
        }
        return -sum;
    }

    public String getDescription(){
        return "min Cost";
    }
}
