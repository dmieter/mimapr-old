package project.engine.slot.slotProcessor.criteriaHelpers;

import project.engine.data.Alternative;
import project.engine.data.Slot;
import project.engine.data.Window;

/**
 *
 * @author magica
 */
public class ExecutionSimilarityCriteria implements ICriteriaHelper {

    public static boolean spaceDistance = true;
    public static double timeWeight = 1;
    public static double costWeight = 1;
    public static double startTimeWeight = 1;
    public static double finishTimeWeight = 1;

    protected PatternObject patternObject;
    protected Class previousCriterionClass;

    public ExecutionSimilarityCriteria() {

    }

    @Override
    public double getCriteriaValue(Window w) {

        double compareVal = compareExecutions(w);
        return compareVal;
    }

    protected double compareExecutions(Window w) {

        MinRunTimeCriteria mrt = new MinRunTimeCriteria();
        MinSumCostCriteria minsc = new MinSumCostCriteria();
        MaxSumCostCriteria maxsc = new MaxSumCostCriteria();

        /* getting best windows by different criteria */
        Window timeW = w.clone();
        mrt.getCriteriaValue(timeW);

        Window minCostW = w.clone();
        minsc.getCriteriaValue(minCostW);

        Window maxCostW = w.clone();
        maxsc.getCriteriaValue(maxCostW);

        double timeCriteria = getComparisonValue(timeW);
        double maxCostCriteria = getComparisonValue(maxCostW);
        double minCostCriteria = getComparisonValue(minCostW);

        /* outer algorithms performs maximization, criteria are calculated so that maximum corresponds to the minimum comparison error */
        if (timeCriteria > maxCostCriteria) {
            if (timeCriteria > minCostCriteria) {
                mrt.getCriteriaValue(w);
                return timeCriteria;
            } else {
                minsc.getCriteriaValue(w);
                return minCostCriteria;
            }
        } else {
            if (maxCostCriteria > minCostCriteria) {
                maxsc.getCriteriaValue(w);
                return maxCostCriteria;
            } else {
                minsc.getCriteriaValue(w);
                return minCostCriteria;
            }
        }

    }

    /* returns distance of input window from pattern object */
    protected double getComparisonValue(Window w) {

        double sumCost = 0;
        double sumTime = 0;
        double startTime = 0;
        double finishTime = 0;

        for (int i = 0; i < w.slotsNeed; i++) {
            Slot s = w.slots.get(i);
            if (s.start > startTime) {
                startTime = s.start;
            }
            sumCost += s.getVolumeCost(w);
            sumTime += s.getVolumeTime(w);
        }

        for (int i = 0; i < w.slotsNeed; i++) {
            Slot s = w.slots.get(i);
            double slotFinishTime = startTime + s.getVolumeTime(w);
            if (slotFinishTime > finishTime) {
                finishTime = slotFinishTime;
            }

        }

        double timeError = getTimeError(sumTime)*timeWeight;
        double costError = getCostError(sumCost)*costWeight;
        double finishError = getFinishTimeError(finishTime)*finishTimeWeight;
        double startError = getStartTimeError(startTime)*startTimeWeight;


        /* negative result as we need to minimize this error and alternative search performs maximization*/
        if (spaceDistance) {
            return -getSpaceDistance(timeError, costError, startError, finishError);
        } else {
            return -getModulDistance(timeError, costError, startError, finishError);
        }
    }

    protected double getTimeError(double sumTime) {
        double timeError = (sumTime - patternObject.getProctime()) / patternObject.getProctime();
        if (timeError < 0) {
            timeError *= -1;
        }

        //System.out.println("timeError: "+timeError);
        return timeError;
    }

    protected double getCostError(double sumCost) {
        double costError = (sumCost - patternObject.getCost()) / patternObject.getCost();
        if (costError < 0) {
            costError *= -1;
        }

        //System.out.println("costError: "+costError);
        return costError;
    }

    protected double getFinishTimeError(double finishTime) {
        double finishTimeError = (finishTime - patternObject.getFinishTime()) / patternObject.getRuntime();
        /* divide by finishTime can be confusing as finish time is rising */

        if (finishTimeError < 0) {
            finishTimeError *= -1;
        }

        //System.out.println("timeError: "+timeError);
        return finishTimeError;
    }

    protected double getStartTimeError(double startTime) {
        double startTimeError = (startTime - patternObject.getStartTime()) / patternObject.getRuntime();
        /* divide by finishTime can be confusing as finish time is rising */

        if (startTimeError < 0) {
            startTimeError *= -1;
        }

        //System.out.println("startTimeError: "+ startTimeError);
        return startTimeError;
    }

    @Override
    public String getDescription() {
        return "Similarity";
        //return "Returns value based on current window execution parameters likeness to the base predefined execution parameters";
    }

    public double getModulDistance(double l1, double l2) {
        return Math.abs(l1) + Math.abs(l2);
    }

    public double getModulDistance(double l1, double l2, double l3, double l4) {
        return getModulDistance(getModulDistance(l1, l2), getModulDistance(l3, l4));
    }

    public double getSpaceDistance(double l1, double l2) {
        return Math.sqrt(l1 * l1 + l2 * l2);
    }

    public double getSpaceDistance(double l1, double l2, double l3, double l4) {
        return getSpaceDistance(getSpaceDistance(l1, l2), getSpaceDistance(l3, l4));
    }

    /**
     * @param patternObject the patternObject to set
     */
    public void setPatternObject(PatternObject patternObject) {
        this.patternObject = patternObject;
    }

    public void setBaseAlternative(Alternative a) {
        patternObject = new PatternObject(a);
    }

    /**
     * @return the previousCriterionClass
     */
    public Class getPreviousCriterionClass() {
        return previousCriterionClass;
    }

    /**
     * @param previousCriterionClass the previousCriterionClass to set
     */
    public void setPreviousCriterionClass(Class previousCriterionClass) {
        this.previousCriterionClass = previousCriterionClass;
    }

    public PatternObject getPatternObject() {
        return patternObject;
    }
}
