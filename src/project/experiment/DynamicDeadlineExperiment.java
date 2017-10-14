//package project.experiment;
//
//import project.engine.data.Resource;
//import project.engine.data.UserJob;
//import project.engine.data.VOEHelper;
//import project.engine.data.VOEnvironment;
//import project.engine.data.environmentGenerator.EnvironmentGenerator;
//import project.engine.data.environmentGenerator.EnvironmentGeneratorSettings;
//import project.engine.data.environmentGenerator.EnvironmentPricingSettings;
//import project.engine.data.jobGenerator.JobGenerator;
//import project.engine.data.jobGenerator.JobGeneratorSettings;
//import project.engine.former.deadline.DeadlineFormer;
//import project.engine.former.deadline.DeadlineFormerSettings;
//import project.engine.former.rweight.RandomWeightFormer;
//import project.engine.former.rweight.RandomWeightFormerSettings;
//import project.engine.scheduler.alternativeSolver.v2.AlternativeSolverSettingsV2;
//import project.engine.scheduler.alternativeSolver.v2.AlternativeSolverV2;
//import project.engine.scheduler.alternativeSolver.v2.LimitSettings;
//import project.engine.scheduler.alternativeSolver.v2.optimization.ConfigurableLimitedOptimization;
//import project.engine.scheduler.alternativeSolver.v2.optimization.OptimizationConfig;
//import project.engine.scheduler.batchSlicer.BatchSlicer;
//import project.engine.scheduler.batchSlicer.BatchSlicerSettings;
////import project.engine.scheduler.dynamic.DynamicSchedulerV2;
//import project.math.distributions.HyperGeometricFacade;
//import project.math.distributions.HyperGeometricSettings;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Random;
//
//public class DynamicDeadlineExperiment extends Experiment {
//
//    private static final int FLOW_SIZE = 150;
//    private static final int CYCLE_LENGTH = 600;
//    private static final int MAX_DEADLINE_VALUE = CYCLE_LENGTH * 10;
//
//    public ArrayList<UserJob> batchToShow;
//    public VOEnvironment envToShow;
//
//    DynamicSchedulerV2 dsRandom;
//    DynamicSchedulerV2 dsDeadline;
//
//    BatchSlicer batchSlicerRandom;
//    BatchSlicerSettings batchSlicerSettingsRandom;
//    BatchSlicer batchSlicerDeadline;
//    BatchSlicerSettings batchSlicerSettingsDeadline;
//
//    DeadlineFormer deadlineFormer;
//    DeadlineFormerSettings deadlineFormerSettings;
//    RandomWeightFormer randomFormer;
//    RandomWeightFormerSettings randomFormerSettings;
//
//    // todo stats
//    double randomDeadlinesMissed = 0;
//    double deadlineDeadlinesMissed = 0;
//
//    private void configureDynamicScheduler() {
//        dsRandom = null; //new DynamicSchedulerV2();
//        dsRandom.setAutomaticEventsProcessing(true);
//
//        dsDeadline = null; //new DynamicSchedulerV2();
//        dsDeadline.setAutomaticEventsProcessing(true);
//    }
//
//    private void configureBatchSlicerRandom() {
//        AlternativeSolverV2 as2 = new AlternativeSolverV2();
//        AlternativeSolverSettingsV2 ass2 = new AlternativeSolverSettingsV2();
//
//        ass2.optType = AlternativeSolverSettingsV2.MIN;
//        ass2.secondaryOptType = AlternativeSolverSettingsV2.MIN;
//        ass2.setSchedulingInterval(0, CYCLE_LENGTH);
//
//        LimitSettings ls = new LimitSettings();
//        ls.limitType = LimitSettings.LIMIT_TYPE_AVERAGE;
//        ls.roundLimitUp = true;
//        ass2.limitSettings = ls;
//
//        OptimizationConfig config = new ConfigurableLimitedOptimization(
//                ConfigurableLimitedOptimization.TIME, /* optimization */
//                ConfigurableLimitedOptimization.START_TIME, /* secondary optimization */
//                ConfigurableLimitedOptimization.COST); /* limit */
//
//        ass2.optimizationConfig = config;
//
//        batchSlicerRandom = new BatchSlicer();
//        batchSlicerSettingsRandom = new BatchSlicerSettings(as2, ass2);
//        batchSlicerSettingsRandom.setSchedulingInterval(0, CYCLE_LENGTH);
//        batchSlicerSettingsRandom.sliceAlgorithm = BatchSlicerSettings.defaultOrder;
//        batchSlicerSettingsRandom.spAlgorithmType = "MODIFIED";
//        batchSlicerSettingsRandom.spConceptType = "COMMON";
//        batchSlicerSettingsRandom.slicesNum = 1;
//        batchSlicerSettingsRandom.shiftAlternatives = true;
//    }
//
//    private void configureBatchSlicerDeadline() {
//        AlternativeSolverV2 as2 = new AlternativeSolverV2();
//        AlternativeSolverSettingsV2 ass2 = new AlternativeSolverSettingsV2();
//
//        ass2.optType = AlternativeSolverSettingsV2.MIN;
//        ass2.secondaryOptType = AlternativeSolverSettingsV2.MIN;
//        ass2.setSchedulingInterval(0, CYCLE_LENGTH);
//
//        LimitSettings ls = new LimitSettings();
//        ls.limitType = LimitSettings.LIMIT_TYPE_AVERAGE;
//        ls.roundLimitUp = true;
//        ass2.limitSettings = ls;
//
//        OptimizationConfig config = new ConfigurableLimitedOptimization(
//                ConfigurableLimitedOptimization.TIME, /* optimization */
//                ConfigurableLimitedOptimization.START_TIME, /* secondary optimization */
//                ConfigurableLimitedOptimization.COST); /* limit */
//
//        ass2.optimizationConfig = config;
//
//        batchSlicerDeadline = new BatchSlicer();
//        batchSlicerSettingsDeadline = new BatchSlicerSettings(as2, ass2);
//        batchSlicerSettingsDeadline.setSchedulingInterval(0, CYCLE_LENGTH);
//        batchSlicerSettingsDeadline.sliceAlgorithm = BatchSlicerSettings.defaultOrder;
//        batchSlicerSettingsDeadline.spAlgorithmType = "MODIFIED";
//        batchSlicerSettingsDeadline.spConceptType = "COMMON";
//        batchSlicerSettingsDeadline.slicesNum = 1;
//        batchSlicerSettingsDeadline.shiftAlternatives = true;
//    }
//
//    private void configureFormers() {
//        deadlineFormerSettings = new DeadlineFormerSettings();
//        deadlineFormerSettings.deadlineIntervalLength = 2 * CYCLE_LENGTH;
//        deadlineFormer = new DeadlineFormer();
//
//        randomFormerSettings = new RandomWeightFormerSettings();
//        randomFormer = new RandomWeightFormer();
//    }
//
//    @Override
//    public void performExperiments(int experimentNumber) {
//        configureDynamicScheduler();
//        configureBatchSlicerRandom();
//        configureBatchSlicerDeadline();
//        configureFormers();
//        for (int i = 0; i < experimentNumber; i++) {
//            System.out.println("-------------- Experiment #" + i + " -------------------");
//            performSingleExperiment(i);
//        }
//        System.out.printf("\n\nRANDOM MISSED:\t%f\nDEADLINE MISSED:\t%f\n",
//                randomDeadlinesMissed, deadlineDeadlinesMissed);
//    }
//
//    private void performSingleExperiment(int experimentNumber) {
//        List<UserJob> randomFlow = generateJobFlow();
//        List<UserJob> deadlineFlow = VOEHelper.copyJobBatchList((ArrayList) randomFlow);
//
//        dsRandom.init(batchSlicerRandom, batchSlicerSettingsRandom, null, (ArrayList) randomFlow,
//                randomFormer, randomFormerSettings, CYCLE_LENGTH);
//        dsDeadline.init(batchSlicerDeadline, batchSlicerSettingsDeadline, null, (ArrayList) deadlineFlow,
//                deadlineFormer, deadlineFormerSettings, CYCLE_LENGTH);
//
//        int cycleNumber = 1;
//        while (anyListHasElements(dsRandom.getQueue(), dsDeadline.getQueue())) {
//
//            System.out.printf("cycle %d... (%d; %d)\n", cycleNumber, randomFlow.size(), deadlineFlow.size());
//            VOEnvironment voe = generateNewEnvironment(CYCLE_LENGTH * (cycleNumber - 1));
//
//            if (!dsRandom.getQueue().isEmpty()) {
//                VOEnvironment randomEnv = VOEHelper.copyEnvironment(voe);
//                dsRandom.start(randomEnv);
//                // todo stats formerStats.processResults(formerEnv, formerBatch, CYCLE_LENGTH);
//            }
//
//            if (!dsDeadline.getQueue().isEmpty()) {
//                VOEnvironment deadlineEnv = VOEHelper.copyEnvironment(voe);
//                dsDeadline.start(deadlineEnv);
//                // todo stats formerStats.processResults(formerEnv, formerBatch, CYCLE_LENGTH);
//            }
//            cycleNumber++;
//        }
//
//        processStats(experimentNumber);
//    }
//
//    private void processStats(int experimentNumber) {
//        int randomMissedCurrent = 0;
//        int deadlineMissedCurrent = 0;
//
//        for (int i = 0; i < FLOW_SIZE; i++) {
//            UserJob randomJob = dsRandom.getCompletedJobs().get(i);
//            if (randomJob.alternatives.get(randomJob.bestAlternative).window.start > randomJob.resourceRequest.deadLine) {
//                randomMissedCurrent++;
//            }
//            UserJob deadlineJob = dsDeadline.getCompletedJobs().get(i);
//            if (deadlineJob.alternatives.get(deadlineJob.bestAlternative).window.start > deadlineJob.resourceRequest.deadLine) {
//                deadlineMissedCurrent++;
//            }
//        }
//
//        randomDeadlinesMissed = (randomDeadlinesMissed * experimentNumber + randomMissedCurrent) / (experimentNumber + 1);
//        deadlineDeadlinesMissed = (deadlineDeadlinesMissed * experimentNumber + deadlineMissedCurrent) / (experimentNumber + 1);
//    }
//
//    private VOEnvironment generateNewEnvironment(double periodStart) {
//        //Creating resources
//        EnvironmentGeneratorSettings envSet = new EnvironmentGeneratorSettings();
//        envSet.minResourceSpeed = 2;
//        envSet.maxResourceSpeed = 15;
//        envSet.resourceLineNum = 24;
//        envSet.maxTaskLength = 100;
//        envSet.minTaskLength = 10;
//        HyperGeometricSettings hgSet = new HyperGeometricSettings(1000, 400, 30, 0, 30, 0, 8);
//        envSet.occupGenerator = new HyperGeometricFacade(hgSet);
//        //envSet.periodStart = periodStart;
//        envSet.timeInterval = CYCLE_LENGTH;
//        EnvironmentGenerator envGen = new EnvironmentGenerator();
//        EnvironmentPricingSettings epc = new EnvironmentPricingSettings();
//        epc.priceQuotient = 1;
//        epc.priceMutationFactor = 0.6;
//        epc.speedExtraCharge = 0.02;
//
//        ArrayList<Resource> lines = envGen.generateResourceTypes(envSet);
//
//        //creating environment
//        VOEnvironment env = envGen.generate(envSet, lines);
//        env.applyPricing(epc);
//
//        return env;
//    }
//
//    private List<UserJob> generateJobFlow() {
//        JobGenerator jg = new JobGenerator();
//        JobGeneratorSettings jgs = new JobGeneratorSettings();
//        jgs.taskNumber = FLOW_SIZE;
//
//        jgs.minPrice = 1.0;
//        jgs.maxPrice = 1.6;
//        jgs.useSpeedPriceFactor = true;
//
//        jgs.minTime = 50;
//        jgs.maxTime = 150;
//
//        jgs.minSpeed = 1;
//        jgs.maxSpeed = 5;
//
//        jgs.minCPU = 2;
//        jgs.maxCPU = 5;
//
//        ArrayList<UserJob> result = jg.generate(jgs);
//        jg.setRandomBatchCriterias(result);
//
//        generateJobDeadlines(result);
//
//        return result;
//    }
//
//    private void generateJobDeadlines(List<UserJob> jobs) {
//        Random random = new Random();
//        for (UserJob job : jobs) {
//            job.resourceRequest.deadLine = random.nextInt(MAX_DEADLINE_VALUE);
//        }
//    }
//
//    @Override
//    public String getData() {
//        // todo correct this
//        return "";
////        return "EXPERIMENT DATA: \n" +
////                "+++++++++++++++++++++++++++\nRANDOM BATCH\n" + randomStats.getData() +
////                "+++++++++++++++++++++++++++\nRANDOM PRIORITIZED BATCH\n" + formerStats.getData();
//    }
//
//    private boolean anyListHasElements(List... lists) {
//        for (List list : lists) {
//            if (!list.isEmpty()) {
//                return true;
//            }
//        }
//        return false;
//    }
//
//}
