/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package project.experiment.processor;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import project.engine.data.UserJob;
import project.engine.data.VOEnvironment;
import project.engine.scheduler.dynamic.DynamicScheduler;
import project.engine.scheduler.dynamic.event.Event;
import project.engine.scheduler.dynamic.event.JobEvent;

/**
 *
 * @author magica
 */
public class DynamicProcessorHelper {
    protected DynamicScheduler scheduler;
    protected String debugData;
    protected VOEnvironment environment;
    protected ArrayList<UserJob> batch;
    protected int modelTime;

    public DynamicProcessorHelper(){
        
    }
    
    public boolean next(){
        
        scheduler.processNextEvent();
        actualizeData();
        
        return true;
    }
    
    protected void actualizeData(){
        
        batch = scheduler.getQueue();
        environment = scheduler.getEnvironment();
        
        modelTime = scheduler.getModelTime();
        
        debugData = getDebugData(scheduler);
    }
    
    /**
     * @param scheduler the scheduler to set
     */
    public void setScheduler(DynamicScheduler scheduler) {
        this.scheduler = scheduler;
        actualizeData();
    }

    /**
     * @return the debugData
     */
    public String getDebugData() {
        return debugData;
    }

    /**
     * @return the environment
     */
    public VOEnvironment getEnvironment() {
        return environment;
    }

    /**
     * @return the batch
     */
    public ArrayList<UserJob> getBatch() {
        return batch;
    }

    private String getDebugData(DynamicScheduler scheduler) {
        String data = "DYNAMIC SCHEDULER DATA \n";
        
        data += "Model Time: "+getModelTime()+"\n";
        
        
        List<Event> events = scheduler.getProcessedEvents();
        if(!events.isEmpty()){
            Event event = events.get(events.size()-1);      // last processed event
            data += "Previous event: "+event.getClass().getSimpleName();
            if(event instanceof JobEvent){
                JobEvent jevent = (JobEvent)event;
                data += " Job "+ jevent.getJob().name + " is " + (jevent.getStatus()==JobEvent.STARTED? "started":"finished");
                
                if(jevent.getStatus()==JobEvent.COMPLETED 
                        && jevent.getJob().getRealIntegerCompletionTime() < jevent.getJob().getCompletionTime()){
                    data += " early";
                }
            }
            data += " at " + event.getTime() + "\n";
        }
        
        PriorityQueue<Event> curEvents = scheduler.getEvents();
        if(!curEvents.isEmpty()){
            Event event = curEvents.peek();       // next event to process
            data += "Next event: "+event.getClass().getSimpleName();
            if(event instanceof JobEvent){
                JobEvent jevent = (JobEvent)event;
                data += " Job "+ jevent.getJob().name + " is " + (jevent.getStatus()==JobEvent.STARTED? "starting":"finishing");
                
                if(jevent.getStatus()==JobEvent.COMPLETED 
                        && jevent.getJob().getRealIntegerCompletionTime() < jevent.getJob().getCompletionTime()){
                    data += " early";
                }
            }
            data += " at " + event.getTime() + "\n";
        }
        
        if(!scheduler.getCompletedJobs().isEmpty()){
            data += "Completed Jobs: ";
            for(UserJob job : scheduler.getCompletedJobs()){
                data+=job.name+" ";
            }
            data +="\n";
        }
        
        if(!scheduler.getRunningJobs().isEmpty()){
            data += "Running Jobs: ";
            for(UserJob job : scheduler.getRunningJobs()){
                data+=job.name+" ";
            }
            data +="\n";
        }
        
        if(!scheduler.getBatch().isEmpty()){
            data += "Scheduled Jobs: ";
            for(UserJob job : scheduler.getBatch()){
                data+=job.name+" ";
            }
            data +="\n";
        }
        
        return data;
    }

    /**
     * @return the modelTime
     */
    public int getModelTime() {
        return modelTime;
    }
    
}
