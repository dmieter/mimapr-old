
package project.engine.slot.slotProcessor.criteriaHelpers;

import project.engine.data.Slot;
import project.engine.data.UserJob;
import project.engine.data.VOEnvironment;
import project.math.utils.MathUtils;

/**
 *
 * @author emelyanov
 */
public class ValuationModel {
    
    protected UserJob job;
    protected VOEnvironment env;
    
    public ValuationModel(UserJob job, VOEnvironment env){
        this.job = job;
        this.env = env;
    }
    
    public Double getSlotValue(Slot s){
        return s.resourceLine.getSpeed();
    }
    
    public Double getSlotWeight(Slot s, double length){
        return s.getLengthCost(length);
    }
    
    public Double getSlotWeight(Slot s){
        return s.getLengthCost(job.resourceRequest.getVolume());
    }
    
    public int getSlotWeightInt(Slot s, double length){
        return (int)MathUtils.nextUp(getSlotWeight(s, length));
    }
    
    public int getSlotWeightInt(Slot s){
        return (int)MathUtils.nextUp(getSlotWeight(s));
    }
}
