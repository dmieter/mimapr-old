package project.engine.scheduler.alternativeSolver.v1;

import project.engine.scheduler.SchedulerSettings;

/**
 * Created by IntelliJ IDEA.
 * User: Rookie
 * Date: 23.03.2010
 * Time: 23:29:19
 * To change this template use File | Settings | File Templates.
 */
public class AlternativeSolverSettings extends SchedulerSettings
{
    public static int TIME = 0;
    public static int COST = 1;
    // limited variable  TIME = 0/COST = 1
    public int limitedVar;
    // optimized variable  TIME = 0/COST = 1
    public int optimizedVar;
    // MIN/MAX/FAKE
    public String optType;
    //best only    
    public boolean optimalOnly = false;
    //Random mode
    public boolean random = false;    
    //PARETO
    public boolean usePareto;
    //Pareto settings
    public double C_weight = 1.0;
    public double D_weight = 1.0;
    public double T_weight = 1.0;
    public double I_weight = 1.0;
    //maximum variant number
    public int varMax = 30000;
    //
    public double limitQuotient = 1.0;

    public int limitCalculationType = 0;  // 0 - average, 1 - const from settings, 2 - from related jobs
    public LimitCountData limitCountData;
}
