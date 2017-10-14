/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package project.engine.scheduler.batchSlicer;

import project.engine.scheduler.Scheduler;
import project.engine.scheduler.SchedulerSettings;
import project.engine.scheduler.alternativeSolver.v1.AlternativeSolver;
import project.engine.scheduler.alternativeSolver.v1.AlternativeSolverSettings;
import project.engine.slot.slotProcessor.criteriaHelpers.ICriteriaHelper;

/**
 *
 * @author Magica
 */
public class BatchSlicerSettings  extends SchedulerSettings {

    public static final int defaultOrder = 0;
    public static final int maxPriceOrder = 1;
    public static final int maxSizeOrder = 2;
    public static final int criteriaBased = 3;

    public int slicesNum = 1;

    public int sliceAlgorithm = defaultOrder;     //0-default simple, 1- simple by Price, 2 - simple by size, 3 - simple by quotient

    public String spConceptType = "COMMON";
    public String spAlgorithmType = "MODIFIED";
    public ICriteriaHelper spCriteriaHelper;
    public boolean shiftAlternatives = false;
    public boolean alternativesAlreadyFound = false;
    public boolean findAllPossibleAlternativesForJob = false;
    
     
    /* backward compatibility */
    @Deprecated
    public AlternativeSolverSettings asSettings;    //How to solve every single subBatch
    
    /* New general variables */
    public Scheduler localScheduler;
    public SchedulerSettings localSchedulerSettings;
    
    @Deprecated
    public BatchSlicerSettings(){
        /* default value to support old API for AlternativeSolverV1 */
        localScheduler = new AlternativeSolver();
    }
    
    /* Generally any scheduler can be used here... AlternativeSolver2 for example */
    public BatchSlicerSettings(Scheduler scheduler, SchedulerSettings settings){
        localScheduler = scheduler;
        localSchedulerSettings = settings;
    }
    
    public SchedulerSettings getLocalSchedulerSettings(){
        if(localSchedulerSettings != null){
            return localSchedulerSettings;
        }else{
            /* supporting old API for AlternativeSolverV1 */
            return asSettings;
        }
    }
}
