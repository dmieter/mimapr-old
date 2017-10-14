
package project.engine.scheduler.dynamic.event;

import project.engine.data.ResourceLine;

/**
 *
 * @author emelyanov
 * 
 * 
 * Not implemented yet
 * 
 */
public class ResourceEvent extends Event {
    
    protected ResourceLine resource;
    private int effectDuration;  /* for example duration for resource removal */
    private String description;  /* brief description to show on resource line  */
    protected int type;
    
    
    public static final int STOP = 1;
    public static final int ADD = 2;
    //public static final int CHANGE = 3; /* CHANGE IS ADD TO EXISTING LINE */
    
    public static final String TASK_STOPPED = "STOPPED";
    
    public ResourceEvent(int time, ResourceLine resource, int type){
        super(time);
        setParams(time, resource, type, 10000, "WORKS");
    }
    
    public ResourceEvent(int time, ResourceLine resource, int type, int effectDuration, String description){
        super(time);
        setParams(time, resource, type, effectDuration, description);
    }
    
    protected void setParams(int time, ResourceLine resource, int type, int effectDuration, String description){
        this.resource = resource;
        this.type = type;
        this.effectDuration = effectDuration;
        this.description = description;
    }

    /**
     * @return the resource
     */
    public ResourceLine getResource() {
        return resource;
    }

    /**
     * @return the type
     */
    public int getType() {
        return type;
    }

    /**
     * @return the effectDuration
     */
    public int getEffectDuration() {
        return effectDuration;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }
    
    
}
