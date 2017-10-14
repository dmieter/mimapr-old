package project.engine.scheduler.dynamic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;
import project.engine.data.Alternative;
import project.engine.data.ResourceLine;
import project.engine.data.ResourceRequest;
import project.engine.data.UserJob;
import project.engine.data.VOEHelper;
import project.engine.data.VOEnvironment;
import project.engine.former.FormerSettings;
import project.engine.former.deadline.DeadlineFormer;
import project.engine.former.deadline.DeadlineFormerSettings;
import project.engine.scheduler.Scheduler;
import project.engine.scheduler.SchedulerOperations;
import project.engine.scheduler.SchedulerSettings;
import project.engine.scheduler.dynamic.event.Event;
import project.engine.scheduler.dynamic.event.EventComparator;
import project.engine.scheduler.dynamic.event.JobEvent;
import project.engine.scheduler.dynamic.event.NextCycleEvent;
import project.engine.scheduler.dynamic.event.ResourceEvent;
import project.math.utils.MathUtils;

/**
 *
 * @author emelyanov
 */
public class DynamicScheduler {

    protected boolean finish = true;

    /* Local scheduling algorithm */
    protected SchedulerSettings schedulerSettings;
    protected Scheduler scheduler;

    protected int modelTime = 0;

    protected int schedulingIntervalEnd = 600;
    protected int schedulingIntervalLength = 600;   /* will be used as horizon */

    protected SchedulingApproach schedulingApproach = SchedulingApproach.HORIZON;

    /* Initial environment, should be populated with new tasks from completed jobs */
    protected VOEnvironment environment;
    /* Initial job queue, should populate jobs with alternatives */
    protected ArrayList<UserJob> queue;

    /* Process temp variables */
    protected VOEnvironment tempEnv;
    protected ArrayList<UserJob> batch;

    protected ArrayList<UserJob> pendingJobs;
    protected ArrayList<UserJob> runningJobs;
    protected ArrayList<UserJob> completedJobs;

    /* need to use priority queue!!! */
    protected PriorityQueue<Event> events;
    protected ArrayList<Event> processedEvents;
    protected boolean automaticEventsProcessing = true;
    protected boolean reSchedulingAllowed = true;

    public int reSchedulingsNum = 0;
    public int schedulingCyclesNum = 0;
    
    protected int defaultBatchSize = 0;

    public void init(Scheduler scheduler, SchedulerSettings settings, VOEnvironment voenv, ArrayList<UserJob> queue) {
        this.schedulerSettings = settings;
        this.scheduler = scheduler;
        this.environment = voenv;
        this.queue = queue;

        flush();

        pendingJobs.addAll(this.queue); /* mark all jobs as pending at start */

    }

    public void flush() {
        tempEnv = VOEHelper.copyEnvironment(environment);
        batch = new ArrayList<UserJob>();
        /* Do we need to flush job queue and all alternatives found?*/

        modelTime = 0;
        schedulingIntervalEnd = 600;

        events = new PriorityQueue<Event>();
        processedEvents = new ArrayList<Event>();

        runningJobs = new ArrayList<UserJob>();
        completedJobs = new ArrayList<UserJob>();
        pendingJobs = new ArrayList<UserJob>();

        if (scheduler != null) {
            scheduler.flush();
        }
    }

    protected void process() {
        long eventsNum = 0;
        if (automaticEventsProcessing) {
            while (!finish) {
                processNextEvent();
                eventsNum++;
                if (schedulingCyclesNum > 100 || modelTime > 100000) {
                    //throw new RuntimeException("Too many scheduling cycles to process all the jobs!!!");
                    System.out.println("Finishing processing by time "+modelTime+" and cycle " + schedulingCyclesNum);
                    /* finished processing with fail */
                    return;
                }
            }
            System.out.println("Events processed:" + eventsNum);
        }
    }

    public void processNextEvent() {

        /* first we need to check if the processing is already over */
        if (checkIfProcessingFinished()) {
            return;
        }

        if (events.isEmpty()) {
            if (!pendingJobs.isEmpty()) {
                // something for horizon scheduling in case there are no events and possible reschedulings
            }
        }

        Event event = events.poll();
        try {
            modelTime = event.getTime();
        } catch (Exception e) {
            checkIfProcessingFinished();
            System.out.println("wow exception");
        }
        if (event instanceof JobEvent) {
            JobEvent je = (JobEvent) event;
            if (JobEvent.STARTED == je.getStatus()) {
                processJobStartEvent(je);
            } else if (JobEvent.COMPLETED == je.getStatus()) {
                processJobCompletionEvent(je);
            } else {
                System.out.println("Event of class " + je.getClass() + " has strange status " + je.getStatus() + ";");
            }
        } else if (event instanceof ResourceEvent) {
            processResourceEvent((ResourceEvent) event);
        } else if (event instanceof NextCycleEvent) {
            processNextCycleEvent((NextCycleEvent) event);
        } else {
            System.out.println("Event of class " + event.getClass() + " triggered at " + event.getTime() + " model time;");
        }
        processedEvents.add(event);

    }

    protected void formBatch() {   // simplified function

        int schedulingHorizon = this.schedulingIntervalLength; /* Horizon approach */

        if (schedulingApproach == SchedulingApproach.CYCLE) {
            schedulingHorizon = schedulingIntervalEnd - modelTime;
        }

        /* smart */
        /*batch.clear();
        FormerSettings fs = new DeadlineFormerSettings();
        fs.periodStart = modelTime;
        fs.cycleLength = schedulingHorizon;
        DeadlineFormer former = new DeadlineFormer();
        batch.addAll(former.form(pendingJobs, environment, fs));*/

        if (batch.isEmpty() && !pendingJobs.isEmpty()) {
            //System.out.println("Smart batch former can't form a batch");
            /* simple */
            batch.clear();
            batch = simpleBatchForm(pendingJobs, schedulingHorizon);
        }

        System.out.println("scheduling " + batch.size() + " jobs");

    }

    public ArrayList<UserJob> simpleBatchForm(List<UserJob> jobFlow, int schedulingHorizon) {
        int jobsToSchedule = defaultBatchSize;
        if(defaultBatchSize <= 0){
            /* some simple logic */
            jobsToSchedule = (tempEnv.resourceLines.size() * schedulingHorizon * 30) / (20 * 600);
        }
        if (jobsToSchedule > pendingJobs.size()) {
            jobsToSchedule = pendingJobs.size();
        }

        ArrayList<UserJob> batchToSchedule = new ArrayList<>();
        for (int i = 0; i < jobsToSchedule; i++) {
            batchToSchedule.add(pendingJobs.get(i));        // moving jobs to batch
        }

        return batchToSchedule;
    }

    public void start() {
        System.out.println("Dynamic Scheduler Processing started");
        finish = false;

        /* creating event for our next cycle */
        NextCycleEvent nce = new NextCycleEvent(schedulingIntervalEnd);
        events.add(nce);

        /* initial scheduling during the start */
        formBatch();
        //schedulerSettings.setSchedulingInterval(getModelTime(), schedulingIntervalEnd);
        scheduleBatch(tempEnv);
        process();
    }

    protected void finish() {
        System.out.println("Dynamic Scheduler Processing finished");
        finish = true;
    }

    protected void scheduleBatch(VOEnvironment env) {
        if (batch.isEmpty()) {
            System.out.println("Dynamic Scheduler: batch is empty, all jobs are either completed or running. "
                    + "Model Time: " + modelTime);
            
            return;
        }

        if (schedulingApproach == SchedulingApproach.HORIZON) {
            schedulingIntervalEnd = modelTime + schedulingIntervalLength;
        }

        schedulerSettings.setSchedulingInterval(modelTime, schedulingIntervalEnd);
        scheduler.solve(schedulerSettings, env, batch);
        for (UserJob job : batch) {
            if (job.bestAlternative >= 0) {
                JobEvent jse = new JobEvent(job, job.getIntegerStartTime(), JobEvent.STARTED);
                JobEvent jce = new JobEvent(job, job.getRealIntegerCompletionTime(), JobEvent.COMPLETED);  //real completion time
                events.add(jse);
                events.add(jce);
            }
        }
    }

    protected void flushBatch() {

        for (UserJob job : pendingJobs) {
            job.clearAlternatives();
            job.bestAlternative = -1;
        }

        for (Iterator<Event> it = events.iterator(); it.hasNext();) {
            Event event = it.next();
            if (event instanceof JobEvent) {
                JobEvent je = (JobEvent) event;
                if (pendingJobs.contains(je.getJob())) {
                    it.remove();    // Removing events from jobs which need to be flushed
                }
            }
        }

        batch.clear();  /* we will need to form a new batch, all the jobs will remain in pending */

    }

    private void processJobCompletionEvent(JobEvent event) {
        event.perform();
        UserJob completedJob = event.getJob();
        runningJobs.remove(completedJob);
        completedJobs.add(completedJob);

        //already should be removed from batch
        //batch.remove(completedJob); /* We don't need to reschedule completed job */
        // expected completion time
        int completionTime = completedJob.getIntegerCompletionTime();

        if (getModelTime() != completionTime) {    // job completed before expected estimation
            SchedulerOperations.completeJobEarly(completedJob, getModelTime());
        }
        // Job is completed, can apply the final schedule to VOE
        VOEHelper.applyBestAlternativeToVOE(completedJob, environment);

        if (getModelTime() != completionTime) {    // Planned allocation changed, need rescheduling
            reSchedule();
        } else if (schedulingApproach == SchedulingApproach.HORIZON) {   // Try to reschedule on updated interval and with possibly new jobs
            reSchedule();
        }

    }

    public void reSchedule() {
        if (reSchedulingAllowed) {
            //System.out.println("Rescheduling");
            tempEnv = VOEHelper.copyEnvironment(environment);  // copy real environment to temp
            VOEHelper.applyBestAlternativesToVOE(runningJobs, tempEnv); // apply all already running jobs, we can't intersect them

            scheduler.flush();
            flushBatch();   // clearing jobs in the batch
            formBatch();    // forming batch according to current state
            scheduleBatch(tempEnv); // rescheduling batch on the temp environment
            reSchedulingsNum++;
        }
    }

    private void processJobStartEvent(JobEvent event) {
        event.perform();
        UserJob startedJob = event.getJob();
        runningJobs.add(startedJob);
        pendingJobs.remove(startedJob);

    }

    private void processResourceEvent(ResourceEvent resourceEvent) {
        int resourceID = resourceEvent.getResource().id;
        ResourceLine existingResource = VOEHelper.getResourceLineByID(environment, resourceID);

        if (ResourceEvent.ADD == resourceEvent.getType()) {
            if (existingResource == null) {
                environment.resourceLines.add(resourceEvent.getResource());
                reSchedule();
            } else { /* CHANGE */

                /*1. Cancel job which is running on the resource (if any) */
                for (Iterator<UserJob> it = runningJobs.iterator(); it.hasNext();) {
                    UserJob job = it.next();
                    if (VOEHelper.isJobRunsOnResource(job, existingResource, resourceEvent.getTime())) {
                        it.remove();
                        pendingJobs.add(job);
                    }
                }

                /*2. Perform resource lines merge and change procedure */
                SchedulerOperations.changeResource(environment, existingResource, resourceEvent.getResource(), modelTime);

                /*3. rescedule */
                reSchedule();

            }
        } else if (ResourceEvent.STOP == resourceEvent.getType()) {
            if (existingResource != null) {

                /*1. Cancel job which is running on the resource (if any) */
                for (Iterator<UserJob> it = runningJobs.iterator(); it.hasNext();) {
                    UserJob job = it.next();
                    if (VOEHelper.isJobRunsOnResource(job, existingResource, resourceEvent.getTime())) {
                        it.remove();
                        pendingJobs.add(job);
                    }
                    /* Interesting notice: two jobs could both be running and use the same resource!
                     As windows have rough right edge, one job can be finished on resource 1, while continue running on other resources;
                     at the same time some other job already can use this resource 1 and run!
                     So, no BREAKS here!!! */
                    //break; /* only one job can run on one resource - FALSE ASSUMPRION, see explanation above */
                }

                /*2. Create task 'stopped' for this resource ending at infinity */
                SchedulerOperations.stopResource(existingResource, resourceEvent.getTime(), resourceEvent);

                /*3. rescedule */
                reSchedule();
            }
        }

        resourceEvent.perform();
    }

    protected void processNextCycleEvent(NextCycleEvent event) {

        /* used only in cycled approach */
        if (schedulingApproach == SchedulingApproach.HORIZON) {
            return;
        }

        schedulingCyclesNum++;
        schedulingIntervalEnd = modelTime + schedulingIntervalLength;
        NextCycleEvent nce = new NextCycleEvent(schedulingIntervalEnd);
        events.add(nce);

        /* initial scheduling at the cycle start */
        if (schedulingApproach == SchedulingApproach.CYCLE) {
            reSchedule();
        }
    }

    /**
     * @param automaticEventsProcessing the automaticEventsProcessing to set
     */
    public void setAutomaticEventsProcessing(boolean automaticEventsProcessing) {
        this.automaticEventsProcessing = automaticEventsProcessing;
    }

    /**
     * @return the modelTime
     */
    public int getModelTime() {
        return modelTime;
    }

    /**
     * @return the environment
     */
    public VOEnvironment getEnvironment() {
        return environment;
    }

    /**
     * @return the queue
     */
    public ArrayList<UserJob> getQueue() {
        return queue;
    }

    /**
     * @return the batch
     */
    public ArrayList<UserJob> getBatch() {
        return batch;
    }

    /**
     * @return the runningJobs
     */
    public ArrayList<UserJob> getRunningJobs() {
        return runningJobs;
    }

    /**
     * @return the completedJobs
     */
    public ArrayList<UserJob> getCompletedJobs() {
        return completedJobs;
    }

    /**
     * @return the events
     */
    public PriorityQueue<Event> getEvents() {
        return events;
    }

    public ArrayList<Event> getProcessedEvents() {
        return processedEvents;
    }

    /**
     * @param reSchedulingAllowed the reSchedulingAllowed to set
     */
    public void setReSchedulingAllowed(boolean reSchedulingAllowed) {
        this.reSchedulingAllowed = reSchedulingAllowed;
    }

    public void setSchedulingInterval(int startInterval, int endInterval) {
        modelTime = startInterval;
        schedulingIntervalEnd = endInterval;
    }

    private boolean checkIfProcessingFinished() {

        if (pendingJobs.isEmpty() && batch.isEmpty() && runningJobs.isEmpty()) {
            finish();
            return true;
        } else if (events.isEmpty()) {
            //throw new RuntimeException("The process is somehow stucked at time " + modelTime
            //        + " with " + pendingJobs.size() + " jobs still waiting in the queue");
            System.out.println("The process is somehow stucked at time " + modelTime
                    + " with " + pendingJobs.size() + " jobs still waiting in the queue");
            
            /* finished processing queue with fail */
            finish();
            return true;
        } else {
            return false;
        }
    }
    
    public void setCycleProcessing(){
        schedulingApproach = SchedulingApproach.CYCLE;
    }
    
    public void setHorizonProcessing(){
        schedulingApproach = SchedulingApproach.HORIZON;
    }

    /**
     * @param schedulingIntervalLength the schedulingIntervalLength to set
     */
    public void setSchedulingIntervalLength(int schedulingIntervalLength) {
        this.schedulingIntervalLength = schedulingIntervalLength;
        schedulingIntervalEnd = modelTime + schedulingIntervalLength;
    }

    /**
     * @param defaultBatchSize the defaultBatchSize to set
     */
    public void setDefaultBatchSize(int defaultBatchSize) {
        this.defaultBatchSize = defaultBatchSize;
    }
    
    public double getLastJobCompletionTime(){
        double finishTime = 0;
        if(completedJobs != null){
            finishTime = completedJobs.get(completedJobs.size()-1).getCompletionTime();
        }
        
        return finishTime;
    }

}
