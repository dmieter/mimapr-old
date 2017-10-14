package project.engine.scheduler.alternativeSolver.v2;

import project.engine.scheduler.SchedulerSettings;
import project.engine.scheduler.alternativeSolver.v2.optimization.OptimizationConfig;

/**
 * Created by IntelliJ IDEA.
 * User: Rookie
 * Date: 23.03.2010
 * Time: 23:29:19
 * To change this template use File | Settings | File Templates.
 */
public class AlternativeSolverSettingsV2 extends SchedulerSettings
{
    public static final int MIN = 0;
    public static final int MAX = 1;
    
    public int optType = MIN;
    public int secondaryOptType = MIN;
    public LimitSettings limitSettings;
    public OptimizationConfig optimizationConfig;
    
    /* The threshold when we shift from combinatorial  to a limited approach of next column generating*/
    public long maxOptionsCombinatorialProcessing = 100000;
    public long maxOptionsProcessing = 100000;
    
    public boolean cleanUpInferiorAlternatives = true;

}
