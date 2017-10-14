
package project.engine.scheduler;

import java.util.ArrayList;
import project.engine.data.UserJob;
import project.engine.data.VOEnvironment;

/**
 *
 * @author emelyanov
 */
public abstract class Scheduler {
    public abstract void solve(SchedulerSettings settings, VOEnvironment voenv, ArrayList<UserJob> batch);
    public abstract void flush();
}
