
package project.engine.scheduler;

/**
 *
 * @author emelyanov
 */
public class SchedulerSettings {

    public int periodStart, periodEnd;
    
    public void setSchedulingInterval(int startInterval, int endInterval) {
        periodStart = startInterval;
        periodEnd = endInterval;
    }
}
