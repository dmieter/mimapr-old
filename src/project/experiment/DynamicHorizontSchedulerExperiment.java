package project.experiment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
import project.engine.scheduler.alternativeSolver.v2.AlternativeSolverSettingsV2;
import project.engine.scheduler.alternativeSolver.v2.AlternativeSolverV2;
import project.engine.scheduler.alternativeSolver.v2.LimitSettings;
import project.engine.scheduler.alternativeSolver.v2.optimization.ConfigurableLimitedOptimization;
import project.engine.scheduler.alternativeSolver.v2.optimization.OptimizationConfig;
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
public class DynamicHorizontSchedulerExperiment extends Experiment {

    public SchedulingResultsStats[] dsCycleStats;
    public SchedulingResultsStats[] dsHorizonStats;

    //BS entities
    private BatchSlicer bs;
    private BatchSlicerSettings bss;

    protected DynamicScheduler dsRe;

    public ArrayList<UserJob> batchToShow;
    public VOEnvironment envToShow;

    private int cycleLength = 600;

    //private int[] intervalLengths = {600};
    //private int[] batchSizes = {10, 15, 20, 25, 30, 35};
    private int[] queueSizes = {100};

    public DynamicHorizontSchedulerExperiment() {
        dsCycleStats = new SchedulingResultsStats[queueSizes.length];
        dsHorizonStats = new SchedulingResultsStats[queueSizes.length];

        for (int i = 0; i < queueSizes.length; i++) {
            dsCycleStats[i] = new SchedulingResultsStats();
            dsHorizonStats[i] = new SchedulingResultsStats();
        }
    }

    private void configureDynamicBatchSlicer() {

        AlternativeSolverV2 as2 = new AlternativeSolverV2();
        AlternativeSolverSettingsV2 ass2 = new AlternativeSolverSettingsV2();

        ass2.optType = AlternativeSolverSettingsV2.MIN;
        ass2.secondaryOptType = AlternativeSolverSettingsV2.MIN;
        ass2.setSchedulingInterval(0, cycleLength);

        LimitSettings ls = new LimitSettings();
        ls.limitType = LimitSettings.LIMIT_TYPE_CONST_PROPORTIONAL;
        ls.constLimit = 50;
        ls.roundLimitUp = true;
        ass2.limitSettings = ls;

        OptimizationConfig config = new ConfigurableLimitedOptimization(
                ConfigurableLimitedOptimization.FINISH_TIME, /* optimization */
                ConfigurableLimitedOptimization.FINISH_TIME, /* secondary optimization */
                ConfigurableLimitedOptimization.USER);
        /* limit */

        ass2.optimizationConfig = config;

        bs = new BatchSlicer();
        bss = new BatchSlicerSettings(as2, ass2);

        bss.setSchedulingInterval(0, cycleLength);
        bss.sliceAlgorithm = BatchSlicerSettings.defaultOrder;
        bss.spAlgorithmType = "MODIFIED";
        bss.spConceptType = "EXTREME";
        bss.slicesNum = 1;
        bss.shiftAlternatives = true;
    }

    private void configureDynamicReScheduler() {
        dsRe = new DynamicScheduler();
        dsRe.setAutomaticEventsProcessing(true);
        dsRe.setReSchedulingAllowed(true);
    }

    private void configureSchedulers(){
        configureDynamicBatchSlicer();
        configureDynamicReScheduler();
    }
    
    public void performExperiments(int expNum) {

        configureSchedulers();

        for (int i = 0; i < expNum; i++) {
            System.out.println("--------------Experiment #" + i + " -------------------");
            flush();
            performSingleExperiment();
        }
    }

    private void performSingleExperiment() {
        VOEnvironment env = generateNewEnvironment();
        
        boolean success = true;

        for (int i = 0; i < queueSizes.length; i++) {

            ArrayList<UserJob> batch = generateJobBatch(queueSizes[i]);
            
            configureSchedulers();
            
            //Horizon Scheduling
            VOEnvironment envHorizon = VOEHelper.copyEnvironment(env);
            ArrayList<UserJob> batchHorizon = VOEHelper.copyJobBatchList(batch);
            bs.flush();

            dsRe.setHorizonProcessing();
            dsRe.setSchedulingIntervalLength(cycleLength);
            dsRe.init(bs, bss, envHorizon, batchHorizon);
            dsRe.setAutomaticEventsProcessing(true);
            dsRe.start();
            if (!SchedulingResultsStats.checkBatchForSuccess(batchHorizon)) {
                success = false;
                dsHorizonStats[i].addFailExperiment();
                System.out.println("Horizon rescheduling failed to find alternatives");
            } else {
                System.out.println("Horizon DS with " + dsRe.reSchedulingsNum + " reschedulings finished successfully");
            }
            List<Double> horizonCompletionTime = new ArrayList<>(Arrays.asList(dsRe.getLastJobCompletionTime(), getAverageJobBudget(batch)));

            /* Just a double check */
            if (VOEHelper.checkBatchIntersectionsWithVOE(batchHorizon, env)) {
                throw new RuntimeException("Alternatives have intersections with environment!!!");
            }

            configureSchedulers();

            //Cycle Scheduling
            VOEnvironment envCycle = VOEHelper.copyEnvironment(env);
            ArrayList<UserJob> batchCycle = VOEHelper.copyJobBatchList(batch);

            dsRe.setCycleProcessing();
            dsRe.setSchedulingIntervalLength(cycleLength);
            dsRe.init(bs, bss, envCycle, batchCycle);
            //generateSpecialEvents(dsRe.getEvents(), env);
            dsRe.start();
            if (!SchedulingResultsStats.checkBatchForSuccess(batchCycle)) {
                success = false;
                dsCycleStats[i].addFailExperiment();
                System.out.println("Cycle rescheduling failed to find alternatives");
            } else {
                System.out.println("Cycle DS with " + dsRe.reSchedulingsNum + " reschedulings finished successfully");
            }
            Integer cyclesNum = dsRe.schedulingCyclesNum;
            List<Double> cycleCompletionTime = new ArrayList<>(Arrays.asList(dsRe.getLastJobCompletionTime(), cyclesNum.doubleValue()));

            /* Just a double check */
            if (VOEHelper.checkBatchIntersectionsWithVOE(batchCycle, env)) {
                //int b = 0;
                throw new RuntimeException("Alternatives have intersections with environment!!!");
            }

            //envToShow = env;
            //batchToShow = batchCycle;

            if (success) {
                dsCycleStats[i].processResults(batchCycle);
                dsCycleStats[i].addAverageList(cycleCompletionTime);
                
                dsHorizonStats[i].processResults(batchHorizon);
                dsHorizonStats[i].addAverageList(horizonCompletionTime);
            }
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
        envSet.timeInterval = cycleLength * 25;
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

    private ArrayList<UserJob> generateJobBatch(int queueSize) {
        JobGenerator jg = new JobGenerator();
        JobGeneratorSettings jgs = new JobGeneratorSettings();
        jgs.taskNumber = queueSize;

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
        for (UserJob job : jobs) {
            job.rankingAlgorithm = ur;
            job.resourceRequest.deadLine = 20000;
            job.userTimeCorrectiveCoef = 0.75;
        }

        jg.setRandomBatchCriterias(jobs);

        return jobs;
    }

    protected void generateSpecialEvents(PriorityQueue<Event> events, VOEnvironment voe) {

        ResourceLine newLine = new ResourceLine(voe.resourceLines.get(0));
        newLine.id = 3632783;
        ResourceEvent reSTART = new ResourceEvent(400, newLine, ResourceEvent.ADD);
        
        ResourceLine changedLine = new ResourceLine(voe.resourceLines.get(1));
        changedLine.resourceType.setSpeed(10); /* speed of all resoyrceType*/
        changedLine.resourceType = new Resource("Changed", 1);
        changedLine.price = 0.01;
        ResourceEvent reCHANGE = new ResourceEvent(400, changedLine, ResourceEvent.ADD);
        
        ResourceEvent reSTOP = new ResourceEvent(400, voe.resourceLines.get(0), ResourceEvent.STOP);
        
        events.add(reSTOP);
        events.add(reSTART);
        events.add(reCHANGE);
        //events.add(re2);
    }

    public String getData() {
        String data = "\nDYNAMIC SCHEDULER EXPERIMENT\n";
        for (int i = 0; i < queueSizes.length; i++) {
            data += "\nHORIZON DCHEDULING RESULTS WITH " + queueSizes[i] + " JOBS\n"
                    + this.dsHorizonStats[i].getData()
                    + "\nCYCLE DCHEDULING RESULTS WITH " + queueSizes[i] + " JOBS\n"
                    + this.dsCycleStats[i].getData();
        }

        return data;
    }

    /**
     * @return the ds
     */
    public DynamicScheduler getDs() {
        return dsRe;
    }

    private void flush() {
        bs.flush();

        dsRe.flush();
        dsRe.setSchedulingInterval(0, cycleLength);

    }
    
    protected Double getAverageJobBudget(List<UserJob> batch){
        Double sumCost = 0d;
        
        for(UserJob j : batch){
            sumCost += j.resourceRequest.priceMax*j.resourceRequest.resourceNeed*j.resourceRequest.time/j.resourceRequest.resourceSpeed;
        }
        
        return sumCost/batch.size();
    }

}
