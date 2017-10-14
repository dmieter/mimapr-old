package project.engine.scheduler.intersect.similarity;

import java.util.List;
import project.engine.data.UserJob;
import project.engine.scheduler.Scheduler;
import project.engine.scheduler.SchedulerSettings;

/**
 *
 * @author magica
 */
public class CopySchedulerSettings extends SchedulerSettings {

    protected SchedulerSettings schedulerSettings;
    protected Scheduler localScheduler;
    
    public double costMultiplier = 1d;
    public double runtimeMultiplier = 1d;
    public double starttimeMultiplier = 1d;
    public double proctimeMultiplier = 1d;

    protected List<UserJob> costBaseSolution;
    protected List<UserJob> runtimeBaseSolution;
    protected List<UserJob> starttimeBaseSolution;
    protected List<UserJob> proctimeBaseSolution;

    protected boolean useBaseSolutionFromBatch = false;

    public CopySchedulerSettings(Scheduler scheduler, SchedulerSettings settings) {
        this.localScheduler = scheduler;
        this.schedulerSettings = settings;
    }

    /**
     * @return the settings
     */
    public SchedulerSettings getSchedulerSettings() {
        return schedulerSettings;
    }

    /**
     * @param settings the settings to set
     */
    public void setSchedulerSettings(SchedulerSettings settings) {
        this.schedulerSettings = settings;
    }

    /**
     * @return the scheduler
     */
    public Scheduler getLocalScheduler() {
        return localScheduler;
    }

    /**
     * @param scheduler the scheduler to set
     */
    public void setLocalScheduler(Scheduler scheduler) {
        this.localScheduler = scheduler;
    }

    /**
     * @return the useBaseSolutionFromBatch
     */
    public boolean isUseBaseSolutionFromBatch() {
        return useBaseSolutionFromBatch;
    }

    /**
     * @param useBaseSolutionFromBatch the useBaseSolutionFromBatch to set
     */
    public void setUseBaseSolutionFromBatch(boolean useBaseSolutionFromBatch) {
        this.useBaseSolutionFromBatch = useBaseSolutionFromBatch;
    }

    /**
     * @param costBaseSolution the costBaseSolution to set
     */
    public void setCostBaseSolution(List<UserJob> costBaseSolution) {
        this.costBaseSolution = costBaseSolution;
    }

    /**
     * @param runtimeBaseSolution the runtimeBaseSolution to set
     */
    public void setRuntimeBaseSolution(List<UserJob> runtimeBaseSolution) {
        this.runtimeBaseSolution = runtimeBaseSolution;
    }

    /**
     * @param starttimeBaseSolution the starttimeBaseSolution to set
     */
    public void setStartTimeBaseSolution(List<UserJob> starttimeBaseSolution) {
        this.starttimeBaseSolution = starttimeBaseSolution;
    }

    /**
     * @param proctimeBaseSolution the proctimeBaseSolution to set
     */
    public void setProctimeBaseSolution(List<UserJob> proctimeBaseSolution) {
        this.proctimeBaseSolution = proctimeBaseSolution;
    }

}
