/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package project.engine.scheduler.dynamic.event;

import project.engine.data.UserJob;

/**
 *
 * @author magica
 */
public class JobEvent extends Event{
    
    protected UserJob job;
    protected int status;
    
    public static final int STARTED = 1;
    public static final int COMPLETED = 2;
    
    public JobEvent(UserJob job, int time, int status){
        super(time);
        this.job = job;
        this.status = status;
    }

    /**
     * @return the job
     */
    public UserJob getJob() {
        return job;
    }

    /**
     * @return the status
     */
    public int getStatus() {
        return status;
    }
}
