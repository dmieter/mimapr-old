/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package project.engine.scheduler.alternativeSolver.v2;

import java.util.ArrayList;
import project.engine.data.UserJob;

/**
 *
 * @author Magica
 */
public class LimitSettings {

    public static final int LIMIT_TYPE_CONST = 0;
    public static final int LIMIT_TYPE_AVERAGE = 1;
    public static final int LIMIT_TYPE_EXTERNAL = 2;
    public static final int LIMIT_TYPE_CONST_PROPORTIONAL = 4; /* proportional constlimit for one job... useful for BatchSlicer */
    
    public int limitType = LIMIT_TYPE_AVERAGE;
    public double limitQuotient = 1.0d; /* Coefficient for limit additional tuning */
    
    /* We have a double limit now, need to have some steps for the table.. 
    Generally the result precision inversly dependent on the step value */
    public double constLimit = 0d;
    public Double limitStep = null;
    public int limitStepQuotient = 100;  /* Increasing intermediate limits in steps of (TotalLimit/limitStepQuotient) */
    public boolean roundLimitUp = false;
    
    public ArrayList<UserJob> externalJobs;

    public LimitSettings(int limitType){
        this.limitType = limitType;
    }
    public LimitSettings(){

    }
}
