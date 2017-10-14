
package project.engine.slot.slotProcessor.criteriaHelpers;

import project.engine.data.Slot;
import project.engine.data.Window;

/**
 *
 * @author emelyanov
 */
public class MaxAdditiveUrerValuationCriterion implements ICriteriaHelper {

    @Override
    public double getCriteriaValue(Window w) {
        if(!w.squareWindow){
            throw new UnsupportedOperationException("MaxAdditiveUrerValuationCriterion supports only Square windows");
        }
        
        ValuationModel vm = new ValuationModel(null, null);
        
        Double sumValue = 0d;
        
        for(Slot slot : w.slots){
            sumValue += vm.getSlotValue(slot);
        }
        
        return sumValue; /* we are maximizing */
        
    }

    @Override
    public String getDescription() {
        return "Additive Valuation";
    }
    
}
