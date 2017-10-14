
package project.experiment;

import java.util.ArrayList;
import project.experiment.processor.DynamicProcessorHelper;
import project.engine.alternativeStats.ResourcePerformanceStats;
import project.engine.alternativeStats.SchedulingResultsStats;
import project.engine.data.Resource;
import project.engine.data.UserJob;
import project.engine.data.VOEHelper;
import project.engine.data.VOEnvironment;
import project.engine.data.environmentGenerator.EnvironmentGenerator;
import project.engine.data.environmentGenerator.EnvironmentGeneratorSettings;
import project.engine.data.environmentGenerator.EnvironmentPricingSettings;
import project.engine.data.jobGenerator.JobGenerator;
import project.engine.data.jobGenerator.JobGeneratorSettings;
import project.engine.data.jobGenerator.RequestGenerator;
import project.engine.scheduler.alternativeSolver.v1.AlternativeSolverSettings;
import project.engine.scheduler.backFill.BackfillSettings;
import project.engine.scheduler.backSliceFill.BackSliceFill;
import project.engine.scheduler.backSliceFill.BackSliceFillSettings;
import project.engine.scheduler.batchSlicer.BatchSlicerSettings;
import project.engine.scheduler.dynamic.DynamicScheduler;
import project.math.distributions.HyperGeometricFacade;
import project.math.distributions.HyperGeometricSettings;

/**
 *
 * @author Magica
 */
public class DynamicSchedulerExperiment {

    public SchedulingResultsStats bsfStats;
    public SchedulingResultsStats dsStats;
    
    //BSF entity
    private BackSliceFill bsf;
    private BackSliceFillSettings bsfs;
    
    protected DynamicScheduler ds;

    public ArrayList<UserJob> batchToShow;
    public VOEnvironment envToShow;

    private int cycleLength = 600;

    public DynamicSchedulerExperiment(){
        bsfStats = new SchedulingResultsStats();
        dsStats = new SchedulingResultsStats();
    }
    
    private void backSliceFillConfiguration(){
        bsf = new BackSliceFill();
        bsfs = new BackSliceFillSettings();

        //bsfs.slicerQuotient = 0.5;

        bsfs.bfs = new BackfillSettings();
        bsfs.bfs.aggressive = true;
        bsfs.bfs.backfillMetric = "COSTMIN";
        bsfs.bfs.periodStart = 0;
        bsfs.bfs.periodEnd = cycleLength;
        bsfs.bfs.policy = "BESTFIT";

        bsfs.bss = new BatchSlicerSettings();
        bsfs.bss.periodStart = 0;
        bsfs.bss.periodEnd = cycleLength;
        bsfs.bss.sliceAlgorithm = 0;
        bsfs.bss.spAlgorithmType = "MODIFIED";
        bsfs.bss.spConceptType = "COMMON";
        bsfs.bss.slicesNum = 1;
        bsfs.bss.shiftAlternatives = true;
        bsfs.bss.asSettings = new AlternativeSolverSettings();
        bsfs.bss.asSettings.usePareto = false;
        bsfs.bss.asSettings.limitedVar = AlternativeSolverSettings.COST;
        bsfs.bss.asSettings.optimizedVar = AlternativeSolverSettings.TIME;
        bsfs.bss.asSettings.optType = "MIN";
        bsfs.bss.asSettings.optimalOnly = true;
        bsfs.bss.asSettings.limitCalculationType = 0;   //average
    }
    
    private void dynamicSchedulerConfiguration(){
        // ds will use bsf configuration
        ds = new DynamicScheduler();
        getDs().setAutomaticEventsProcessing(false);
    }
    
    public void performExperiments(int expNum){

        backSliceFillConfiguration();
        dynamicSchedulerConfiguration();

        for(int i=0; i<expNum;i++){
            System.out.println("--------------Experiment #"+i+" -------------------");
            performSingleExperiment();
        }
    }
    
    private void performSingleExperiment(){
        VOEnvironment env = generateNewEnvironment();
        ArrayList<UserJob> batch = generateJobBatch();        
        boolean success = true;
        
        UserJob j = batch.get(5);
        j.userTimeCorrectiveCoef = 0.8;
        
        //BSF
        VOEnvironment envBSF = VOEHelper.copyEnvironment(env);
        ArrayList<UserJob> batchBSF = VOEHelper.copyJobBatchList(batch);
        bsfs.slicerQuotient = 1;
        bsfs.bss.slicesNum = 2;
        bsf.solve(bsfs, envBSF, batchBSF);
        if(!SchedulingResultsStats.checkBatchForSuccess(batchBSF)){
            success = false;
            bsfStats.addFailExperiment();
            System.out.println("BSF failed to find alternatives");
        }
        
        /* Just a double check */
        if(VOEHelper.checkBatchIntersectionsWithVOE(batchBSF, env)){
            throw new RuntimeException("Alternatives have intersections with environment!!!");
        }
        
        //DS
        VOEnvironment envDS = VOEHelper.copyEnvironment(env);
        ArrayList<UserJob> batchDS = VOEHelper.copyJobBatchList(batch);
        bsf.flush();
        getDs().init(bsf, bsfs, envDS, batchDS);
        getDs().start();
        if(!SchedulingResultsStats.checkBatchForSuccess(batchDS)){
            success = false;
            dsStats.addFailExperiment();
            System.out.println("DS failed to find alternatives");
        }
        
        
        envToShow = env;
        batchToShow = batchDS;

        if(success){
            bsfStats.processResults(batchBSF);
            dsStats.processResults(batchDS);
        }
    }
    
    private VOEnvironment generateNewEnvironment() {
        //Creating resources
        EnvironmentGeneratorSettings envSet = new EnvironmentGeneratorSettings();
        envSet.minResourceSpeed = 2;
        envSet.maxResourceSpeed = 12;
        envSet.resourceLineNum = 8;
        envSet.maxTaskLength = 100;
        envSet.minTaskLength = 10;
        //envSet.occupancyLevel = 1;
        HyperGeometricSettings hgSet = new HyperGeometricSettings(1000, 150, 30, 0, 10, 0, 2);
        envSet.occupGenerator = new HyperGeometricFacade(hgSet);
        envSet.timeInterval = cycleLength*4;
        //envSet.hgPerfSet = new HyperGeometricSettings(1000, 60, 100, 1);   //mean = 6.0 e = 2.254125347242491
        EnvironmentGenerator envGen = new EnvironmentGenerator();
        EnvironmentPricingSettings epc = new EnvironmentPricingSettings();
        epc.priceQuotient = 1;
        epc.priceMutationFactor = 0.6;
        epc.speedExtraCharge = 0.02;

        ArrayList<Resource> lines = envGen.generateResourceTypes(envSet);

        //creating environment
        VOEnvironment env = envGen.generate(envSet, lines);
        env.applyPricing(epc);

        return env;
    }
    
    private void clearSchedullingData(){
        bsf = new BackSliceFill();
    }
    
    private ArrayList<UserJob> generateJobBatch() {
        JobGenerator jg = new JobGenerator();
        JobGeneratorSettings jgs = new JobGeneratorSettings();
        jgs.taskNumber = 10;

        jgs.minPrice = 1.0;
        jgs.maxPrice = 1.6;
        jgs.useSpeedPriceFactor = true;

        jgs.minTime = 100;
        jgs.maxTime = 500;

        jgs.minSpeed = 1;
        jgs.maxSpeed = 1;

        jgs.minCPU = 3;
        jgs.maxCPU = 3;

        return jg.generate(jgs);
    }

    public String getData(){
        String data = "DYNAMIC SCHEDULER EXPERIMENT\n"
                +"BSF Stats\n"
                +this.bsfStats.getData()+"\n"
                +"DS Stats\n"
                +this.dsStats.getData();

        return data;
    }

    /**
     * @return the ds
     */
    public DynamicScheduler getDs() {
        return ds;
    }

}
