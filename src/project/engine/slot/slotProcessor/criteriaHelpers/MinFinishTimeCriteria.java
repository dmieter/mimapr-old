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
public class MinFinishTimeCriteria implements ICriteriaHelper {

    public double getCriteriaValue(Window w) {
        
        if(w.squareWindow){
            return -(w.start+w.length);
        }
        
        MinRunTimeCriteria runtimeCriteria = new MinRunTimeCriteria();
        double runtime = -runtimeCriteria.getCriteriaValue(w); //now w - window with minimum runtime
        double startTime = Double.NEGATIVE_INFINITY;
        for(int i=0;i<w.slotsNeed;i++){
            Slot s = w.slots.get(i);
            if(startTime < s.start)
                startTime = s.start;
        }
        double finishTime = startTime + runtime;

        return -finishTime;
    }

    public String getDescription(){
        return "min Finish";
    }
}
