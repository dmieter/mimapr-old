
package project.experiment;

import java.util.ArrayList;
import java.util.PriorityQueue;
import project.engine.alternativeStats.SchedulingResultsStats;
import project.engine.data.Resource;
import project.engine.data.ResourceLine;
import project.engine.data.UserJob;
import project.engine.data.VOEHelper;
import project.engine.data.VOEnvironment;
import project.engine.data.environmentGenerator.EnvironmentGenerator;
import project.engine.data.environmentGenerator.EnvironmentGeneratorSettings;
import project.engine.data.environmentGenerator.EnvironmentPricingSettings;
import project.engine.data.jobGenerator.JobGenerator;
import project.engine.data.jobGenerator.JobGeneratorSettings;
import project.engine.scheduler.SchedulerOperations;
import project.engine.scheduler.alternativeSolver.v1.AlternativeSolverSettings;
import project.engine.scheduler.alternativeSolver.v1.LimitCountData;
import project.engine.scheduler.alternativeSolver.v2.AlternativeSolverSettingsV2;
import project.engine.scheduler.alternativeSolver.v2.AlternativeSolverV2;
import project.engine.scheduler.alternativeSolver.v2.LimitSettings;
import project.engine.scheduler.alternativeSolver.v2.optimization.ConfigurableLimitedOptimization;
import project.engine.scheduler.alternativeSolver.v2.optimization.OptimizationConfig;
import project.engine.scheduler.backFill.BackfillSettings;
import project.engine.scheduler.batchSlicer.BatchSlicer;
import project.engine.scheduler.batchSlicer.BatchSlicerSettings;
import project.engine.scheduler.dynamic.DynamicScheduler;
import project.engine.scheduler.dynamic.event.Event;
import project.engine.scheduler.dynamic.event.ResourceEvent;
import project.engine.slot.slotProcessor.userRankings.PercentileUserRanking;
import project.engine.slot.slotProcessor.userRankings.UserRanking;
import project.math.distributions.GaussianFacade;
import project.math.distributions.GaussianSettings;
import project.math.distributions.HyperGeometricFacade;
import project.math.distributions.HyperGeometricSettings;

/**
 *
 * @author Magica
 */
public class DynamicSchedulerAndBSCompareExperiment extends Experiment {

    public SchedulingResultsStats oldBSStats;
    public SchedulingResultsStats newBSStats;
    public SchedulingResultsStats dsStats;
    public SchedulingResultsStats dsReStats;
    
    //BS entities
    private BatchSlicer bsOld;
    private BatchSlicerSettings bssOld;
    
    private BatchSlicer bsNew;
    private BatchSlicerSettings bssNew;
    
    private BatchSlicer bs4Dyn;
    private BatchSlicerSettings bss4Dyn;
    
    private BatchSlicer bs4DynRe;
    private BatchSlicerSettings bss4DynRe;
    
    protected DynamicScheduler ds;
    protected DynamicScheduler dsRe;

    public ArrayList<UserJob> batchToShow;
    public VOEnvironment envToShow;

    private int cycleLength = 600;

    public DynamicSchedulerAndBSCompareExperiment(){
        oldBSStats = new SchedulingResultsStats();
        newBSStats = new SchedulingResultsStats();
        dsStats = new SchedulingResultsStats();
        dsReStats = new SchedulingResultsStats();
    }
    
    private void configureOldBatchSlicer(){
        bsOld = new BatchSlicer();
        bssOld = new BatchSlicerSettings();

        bssOld.setSchedulingInterval(0, cycleLength);
        bssOld.sliceAlgorithm = BatchSlicerSettings.defaultOrder;
        bssOld.spAlgorithmType = "MODIFIED";
        bssOld.spConceptType = "EXTREME";
        bssOld.slicesNum = 2;
        bssOld.shiftAlternatives = true;
        bssOld.asSettings = new AlternativeSolverSettings();
        bssOld.asSettings.usePareto = false;
        bssOld.asSettings.limitedVar = 1;      //Cost
        bssOld.asSettings.optimizedVar = 0;    //Time
        bssOld.asSettings.optType = "MIN";
        bssOld.asSettings.optimalOnly = true;
        bssOld.asSettings.limitCalculationType = 0;    //average
        bssOld.asSettings.limitQuotient = 1.0;
        bssOld.asSettings.limitCountData = new LimitCountData();
    }
    
    private void configureNewBatchSlicer(){
        
        AlternativeSolverV2 as2 = new AlternativeSolverV2();
        AlternativeSolverSettingsV2 ass2 = new AlternativeSolverSettingsV2();
        
        ass2.optType = AlternativeSolverSettingsV2.MIN;
        ass2.secondaryOptType = AlternativeSolverSettingsV2.MIN;
        ass2.setSchedulingInterval(0, cycleLength);
        
        
        LimitSettings ls = new LimitSettings();
        ls.limitType = LimitSettings.LIMIT_TYPE_AVERAGE;
        ls.roundLimitUp = true;
        ass2.limitSettings = ls;
        
        OptimizationConfig config = new ConfigurableLimitedOptimization(
                                            ConfigurableLimitedOptimization.TIME,  /* optimization */
                                            ConfigurableLimitedOptimization.START_TIME,  /* secondary optimization */    
                                            ConfigurableLimitedOptimization.COST); /* limit */
        
        ass2.optimizationConfig = config;
        
        bsNew = new BatchSlicer();
        bssNew = new BatchSlicerSettings(as2, ass2);
        
        bssNew.setSchedulingInterval(0, cycleLength);
        bssNew.sliceAlgorithm = BatchSlicerSettings.defaultOrder;
        bssNew.spAlgorithmType = "MODIFIED";
        bssNew.spConceptType = "EXTREME";
        bssNew.slicesNum = 2;
        bssNew.shiftAlternatives = true;
    }
    
    private void configureDynamicBatchSlicer(){
        
        AlternativeSolverV2 as2 = new AlternativeSolverV2();
        AlternativeSolverSettingsV2 ass2 = new AlternativeSolverSettingsV2();
        
        ass2.optType = AlternativeSolverSettingsV2.MIN;
        ass2.secondaryOptType = AlternativeSolverSettingsV2.MIN;
        ass2.setSchedulingInterval(0, cycleLength);
        
        
        LimitSettings ls = new LimitSettings();
        ls.limitType = LimitSettings.LIMIT_TYPE_CONST;
        ls.constLimit = 20;
        ls.roundLimitUp = true;
        ass2.limitSettings = ls;
        
        OptimizationConfig config = new ConfigurableLimitedOptimization(
                                            ConfigurableLimitedOptimization.TIME,  /* optimization */
                                            ConfigurableLimitedOptimization.START_TIME,  /* secondary optimization */    
                                            ConfigurableLimitedOptimization.USER); /* limit */
        
        ass2.optimizationConfig = config;
        
        bs4Dyn = new BatchSlicer();
        bss4Dyn = new BatchSlicerSettings(as2, ass2);
        
        bss4Dyn.setSchedulingInterval(0, cycleLength);
        bss4Dyn.sliceAlgorithm = BatchSlicerSettings.defaultOrder;
        bss4Dyn.spAlgorithmType = "MODIFIED";
        bss4Dyn.spConceptType = "EXTREME";
        bss4Dyn.slicesNum = 2;
        bss4Dyn.shiftAlternatives = true;
    }
    
    private void configureDynamicBatchSlicerRe(){
        
        AlternativeSolverV2 as2 = new AlternativeSolverV2();
        AlternativeSolverSettingsV2 ass2 = new AlternativeSolverSettingsV2();
        
        ass2.optType = AlternativeSolverSettingsV2.MIN;
        ass2.secondaryOptType = AlternativeSolverSettingsV2.MIN;
        ass2.setSchedulingInterval(0, cycleLength);
        
        
        LimitSettings ls = new LimitSettings();
        ls.limitType = LimitSettings.LIMIT_TYPE_CONST;
        ls.roundLimitUp = true;
        ls.constLimit = 20;
        ass2.limitSettings = ls;
        
        OptimizationConfig config = new ConfigurableLimitedOptimization(
                                            ConfigurableLimitedOptimization.TIME,  /* optimization */
                                            ConfigurableLimitedOptimization.START_TIME,  /* secondary optimization */    
                                            ConfigurableLimitedOptimization.USER); /* limit */
        
        ass2.optimizationConfig = config;
        
        bs4DynRe = new BatchSlicer();
        bss4DynRe = new BatchSlicerSettings(as2, ass2);
        
        bss4DynRe.setSchedulingInterval(0, cycleLength);
        bss4DynRe.sliceAlgorithm = BatchSlicerSettings.defaultOrder;
        bss4DynRe.spAlgorithmType = "MODIFIED";
        bss4DynRe.spConceptType = "EXTREME";
        bss4DynRe.slicesNum = 2;
        bss4DynRe.shiftAlternatives = true;
    }
    
    private void configureDynamicScheduler(){
        ds = new DynamicScheduler();
        ds.setAutomaticEventsProcessing(true);
        ds.setReSchedulingAllowed(false);
    }
    private void configureDynamicReScheduler(){
        dsRe = new DynamicScheduler();
        dsRe.setAutomaticEventsProcessing(true);
        dsRe.setReSchedulingAllowed(true);
    }
    
    public void performExperiments(int expNum){

        configureOldBatchSlicer();
        configureNewBatchSlicer();
        configureDynamicBatchSlicer();
        configureDynamicBatchSlicerRe();
        configureDynamicScheduler();
        configureDynamicReScheduler();

        for(int i=0; i<expNum;i++){
            System.out.println("--------------Experiment #"+i+" -------------------");
            flush();
            performSingleExperiment();
        }
    }
    
    private void performSingleExperiment(){
        VOEnvironment env = generateNewEnvironment();
        ArrayList<UserJob> batch = generateJobBatch();        
        boolean success = true;
        
        for(UserJob j : batch){
            j.userTimeCorrectiveCoef = 0.6;
        }
        
        //old BS
        VOEnvironment envBSOld = VOEHelper.copyEnvironment(env);
        ArrayList<UserJob> batchBSOld = VOEHelper.copyJobBatchList(batch);
        bsOld.solve(bssOld, envBSOld, batchBSOld);
        if(!SchedulingResultsStats.checkBatchForSuccess(batchBSOld)){
            success = false;
            oldBSStats.addFailExperiment();
            System.out.println("Old BS failed to find alternatives");
        }else{
            System.out.println("BS finished successfully");
        }
        
        
        //new BS
        VOEnvironment envBSNew = VOEHelper.copyEnvironment(env);
        ArrayList<UserJob> batchBSNew = VOEHelper.copyJobBatchList(batch);
        long tBS = System.nanoTime();
        bsNew.solve(bssNew, envBSNew, batchBSNew);
        tBS = System.nanoTime() - tBS;
        if(!SchedulingResultsStats.checkBatchForSuccess(batchBSNew)){
            success = false;
            newBSStats.addFailExperiment();
            System.out.println("New BS failed to find alternatives");
        }else{
            System.out.println("New BS finished successfully");
        }
        
        /* Just a double check */
        if(VOEHelper.checkBatchIntersectionsWithVOE(batchBSNew, env)){
            throw new RuntimeException("Alternatives have intersections with environment!!!");
        }
        
        //DS
        VOEnvironment envDS = VOEHelper.copyEnvironment(env);
        ArrayList<UserJob> batchDS = VOEHelper.copyJobBatchList(batch);
        bs4Dyn.flush();
        ds.init(bs4Dyn, bss4Dyn, envDS, batchDS);
        long tDS = System.nanoTime();
        //ds.start();
        tDS = System.nanoTime() - tDS;
        if(!SchedulingResultsStats.checkBatchForSuccess(batchDS)){
            success = false;
            dsStats.addFailExperiment();
            System.out.println("DS failed to find alternatives");
        }else{
            System.out.println("DS finished successfully");
        }
        
        //DS with Rescheduling
        VOEnvironment envDSRe = VOEHelper.copyEnvironment(env);
        ArrayList<UserJob> batchDSRe = VOEHelper.copyJobBatchList(batch);
        bs4DynRe.flush();
        dsRe.init(bs4DynRe, bss4DynRe, envDSRe, batchDSRe);
        generateSpecialEvents(dsRe.getEvents(), envDSRe);
        long tDSRe = System.nanoTime();
//        dsRe.setAutomaticEventsProcessing(false);
        dsRe.start();
        tDSRe = System.nanoTime() - tDSRe;
        if(!SchedulingResultsStats.checkBatchForSuccess(batchDSRe)){
            success = false;
            dsReStats.addFailExperiment();
            System.out.println("DS with rescheduling failed to find alternatives");
        }else{
            System.out.println("DS with " + dsRe.reSchedulingsNum + " reschedulings finished successfully");
        }
        
        envToShow = env;
        batchToShow = batchDSRe;

        if(success){
            oldBSStats.processResults(batchBSOld);
            newBSStats.processResults(batchBSNew, tBS);
            dsStats.processResults(batchDS, tDS);
            dsReStats.processResults(batchDSRe, tDSRe);
        }
    }
    
    private VOEnvironment generateNewEnvironment() {
        //Creating resources
        EnvironmentGeneratorSettings envSet = new EnvironmentGeneratorSettings();
        envSet.minResourceSpeed = 2;
        envSet.maxResourceSpeed = 11;
        envSet.resourceLineNum = 20;
        envSet.maxTaskLength = 100;
        envSet.minTaskLength = 10;
        //envSet.occupancyLevel = 1;
        HyperGeometricSettings hgSet = new HyperGeometricSettings(1000, 150, 30, 0, 10, 0, 2);
        envSet.occupGenerator = new HyperGeometricFacade(hgSet);
        envSet.timeInterval = cycleLength*2;
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
    
    private ArrayList<UserJob> generateJobBatch() {
        JobGenerator jg = new JobGenerator();
        JobGeneratorSettings jgs = new JobGeneratorSettings();
        jgs.taskNumber = 75;

        jgs.minPrice = 1.0;
        jgs.maxPrice = 1.6;
        jgs.useSpeedPriceFactor = true;

        jgs.minTime = 100;
        jgs.maxTime = 600;

        jgs.minSpeed = 1;
        jgs.maxSpeed = 1;

        jgs.minCPU = 3;
        jgs.maxCPU = 8;

        GaussianSettings gs = new GaussianSettings(0.2, 0.6, 1);
        jgs.timeCorrectiveCoefGen = new GaussianFacade(gs);
                
        ArrayList<UserJob> jobs = jg.generate(jgs);
        UserRanking ur = new PercentileUserRanking();
        for(UserJob job : jobs){
            job.rankingAlgorithm = ur;
            job.resourceRequest.deadLine = 12000;
        }
        
        jg.setRandomBatchCriterias(jobs);
        
        return jobs;
    }
    
    protected void generateSpecialEvents(PriorityQueue<Event> events, VOEnvironment voe){
        ResourceEvent re = new ResourceEvent(100, voe.resourceLines.get(0), ResourceEvent.STOP);
        ResourceEvent re3 = new ResourceEvent(200, voe.resourceLines.get(0), ResourceEvent.STOP);
        ResourceEvent re4 = new ResourceEvent(800, voe.resourceLines.get(0), ResourceEvent.STOP);
        
        ResourceLine newLine = new ResourceLine(voe.resourceLines.get(0));
        //newLine.id = 3632783;
        ResourceEvent re2 = new ResourceEvent(101, newLine, ResourceEvent.ADD);
        events.add(re);
        events.add(re4);
        //events.add(re3);
        events.add(re2);
    }

    public String getData(){
        String data = "DYNAMIC SCHEDULER EXPERIMENT\n"
                +"OLD BS STATS\n"
                +this.oldBSStats.getData()+"\n"
                +"NEW BS STATS\n"
                +this.newBSStats.getData()+"\n"
                +"DYNAMIC BS STATS\n"
                +this.dsStats.getData()+"\n"
                +"DYNAMIC BS WITH RESCHEDULING STATS\n"
                +this.dsReStats.getData();

        return data;
    }

    /**
     * @return the ds
     */
    public DynamicScheduler getDs() {
        return dsRe;
    }

    private void flush() {
        bsOld.flush();
        
        bsNew.flush();
        
        bs4Dyn.flush();
        bs4Dyn.flush();
        
        ds.flush();
        ds.setSchedulingInterval(0, cycleLength);
        
        dsRe.flush();
        dsRe.setSchedulingInterval(0, cycleLength);
        
    }

}
