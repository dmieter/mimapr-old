
package project.engine.slot.slotProcessor.criteriaHelpers;

import project.engine.data.Alternative;

/**
 *
 * @author dmieter
 */
public class PatternObject {
    protected Double cost = null;
    protected Double startTime = null;
    protected Double runtime = null;
    protected Double proctime = null;
    
    public PatternObject(){
        
    }
    
    public PatternObject(Alternative a){
        cost = a.getCost();
        startTime = a.getStart();
        runtime = a.getRuntime();
        proctime = a.getLength();
    }

    /**
     * @return the cost
     */
    public Double getCost() {
        return cost;
    }

    /**
     * @param cost the cost to set
     */
    public void setCost(Double cost) {
        this.cost = cost;
    }

    /**
     * @return the startTime
     */
    public Double getStartTime() {
        return startTime;
    }

    /**
     * @param startTime the startTime to set
     */
    public void setStartTime(Double startTime) {
        this.startTime = startTime;
    }

    /**
     * @return the runtime
     */
    public Double getRuntime() {
        return runtime;
    }

    /**
     * @param runtime the runtime to set
     */
    public void setRuntime(Double runtime) {
        this.runtime = runtime;
    }

    /**
     * @return the proctime
     */
    public Double getProctime() {
        return proctime;
    }

    /**
     * @param proctime the proctime to set
     */
    public void setProctime(Double proctime) {
        this.proctime = proctime;
    }

    public Double getFinishTime() {
        return startTime + runtime;
    }
    
}
