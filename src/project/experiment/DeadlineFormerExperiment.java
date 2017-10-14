package project.experiment;

import project.engine.alternativeStats.DeadlineFormerExperimentStats;
import project.engine.data.*;
import project.engine.data.environmentGenerator.EnvironmentGenerator;
import project.engine.data.environmentGenerator.EnvironmentGeneratorSettings;
import project.engine.data.environmentGenerator.EnvironmentPricingSettings;
import project.engine.data.jobGenerator.JobGenerator;
import project.engine.data.jobGenerator.JobGeneratorSettings;
import project.engine.former.deadline.DeadlineFormer;
import project.engine.former.deadline.DeadlineFormerSettings;
import project.engine.former.rweight.RandomWeightFormer;
import project.engine.former.rweight.RandomWeightFormerSettings;
import project.engine.scheduler.alternativeSolver.v2.AlternativeSolverSettingsV2;
import project.engine.scheduler.alternativeSolver.v2.AlternativeSolverV2;
import project.engine.scheduler.alternativeSolver.v2.LimitSettings;
import project.engine.scheduler.alternativeSolver.v2.optimization.ConfigurableLimitedOptimization;
import project.engine.scheduler.alternativeSolver.v2.optimization.OptimizationConfig;
import project.engine.scheduler.batchSlicer.BatchSlicer;
import project.engine.scheduler.batchSlicer.BatchSlicerSettings;
import project.math.distributions.HyperGeometricFacade;
import project.math.distributions.HyperGeometricSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by Petrukha on 20.04.2016.
 */
public class DeadlineFormerExperiment extends Experiment {

    private static final int FLOW_SIZE = 150;
    private static final int CYCLE_LENGTH = 600;
    private static final int MAX_DEADLINE_VALUE = CYCLE_LENGTH * 10;
    private static final int RANDOM_BATCH_SIZE = 10;

    public ArrayList<UserJob> batchToShow;
    public VOEnvironment envToShow;

    BatchSlicer batchSlicer;
    BatchSlicerSettings batchSlicerSettings;

    RandomWeightFormer randomFormer;
    RandomWeightFormerSettings randomFormerSettings;
    DeadlineFormer deadlineFormer;
    DeadlineFormerSettings deadlineFormerSettings;

    DeadlineFormerExperimentStats randomStats = new DeadlineFormerExperimentStats();
    DeadlineFormerExperimentStats formerStats = new DeadlineFormerExperimentStats();

    private void configureBatchSlicer() {
        AlternativeSolverV2 as2 = new AlternativeSolverV2();
        AlternativeSolverSettingsV2 ass2 = new AlternativeSolverSettingsV2();

        ass2.optType = AlternativeSolverSettingsV2.MIN;
        ass2.secondaryOptType = AlternativeSolverSettingsV2.MIN;
        ass2.setSchedulingInterval(0, CYCLE_LENGTH);

        LimitSettings ls = new LimitSettings();
        ls.limitType = LimitSettings.LIMIT_TYPE_AVERAGE;
        ls.roundLimitUp = true;
        ass2.limitSettings = ls;

        OptimizationConfig config = new ConfigurableLimitedOptimization(
                ConfigurableLimitedOptimization.TIME, /* optimization */
                ConfigurableLimitedOptimization.START_TIME, /* secondary optimization */
                ConfigurableLimitedOptimization.COST); /* limit */

        ass2.optimizationConfig = config;

        batchSlicer = new BatchSlicer();
        batchSlicerSettings = new BatchSlicerSettings(as2, ass2);
        batchSlicerSettings.setSchedulingInterval(0, CYCLE_LENGTH);
        batchSlicerSettings.sliceAlgorithm = BatchSlicerSettings.defaultOrder;
        batchSlicerSettings.spAlgorithmType = "MODIFIED";
        batchSlicerSettings.spConceptType = "COMMON";
        batchSlicerSettings.slicesNum = 1;
        batchSlicerSettings.shiftAlternatives = true;
    }

    private void configureFormers() {
        randomFormer = new RandomWeightFormer();
        randomFormerSettings = new RandomWeightFormerSettings();
        randomFormerSettings.cycleLength = CYCLE_LENGTH;

        deadlineFormer = new DeadlineFormer();
        deadlineFormerSettings = new DeadlineFormerSettings();
        deadlineFormerSettings.deadlineIntervalLength = 2 * CYCLE_LENGTH;
        deadlineFormerSettings.cycleLength = CYCLE_LENGTH;
    }

    @Override
    public void performExperiments(int experimentNumber) {
        configureBatchSlicer();
        configureFormers();
        for (int i = 0; i < experimentNumber; i++) {
            System.out.println("-------------- Experiment #" + i + " -------------------");
            performSingleExperiment();
        }
    }

    private void performSingleExperiment() {
        List<UserJob> randomFlow = generateJobFlow();
        List<UserJob> formerFlow = VOEHelper.copyJobBatchList((ArrayList) randomFlow);

        int cycleNumber = 1;
        while (anyListHasElements(randomFlow, formerFlow)) {

            // Show info
            System.out.printf("cycle %d... (%d; %d)\n",
                    cycleNumber++, randomFlow.size(), formerFlow.size());
            VOEnvironment voe = generateNewEnvironment();

            if (!randomFlow.isEmpty()) {
                VOEnvironment randomEnv = VOEHelper.copyEnvironment(voe);
                List<UserJob> randomBatch = randomFormer.form(randomFlow, randomEnv, randomFormerSettings);
                batchSlicer.flush();
                batchSlicer.solve(batchSlicerSettings, randomEnv, (ArrayList) randomBatch);
                randomStats.processResults(randomEnv, randomBatch, CYCLE_LENGTH);
                excludeSuccessfulJobsFromFlow(randomFlow, randomBatch);
                adjustDeadlines(randomFlow);
            }

            if (!formerFlow.isEmpty()) {
                VOEnvironment formerEnv = VOEHelper.copyEnvironment(voe);
                List<UserJob> formerBatch = deadlineFormer.form(formerFlow, formerEnv, deadlineFormerSettings);
                batchSlicer.flush();
                batchSlicer.solve(batchSlicerSettings, formerEnv, (ArrayList) formerBatch);
                formerStats.processResults(formerEnv, formerBatch, CYCLE_LENGTH);
                excludeSuccessfulJobsFromFlow(formerFlow, formerBatch);
                adjustDeadlines(formerFlow);
            }
        }
        finishExperiment();
    }

    private VOEnvironment generateNewEnvironment() {
        //Creating resources
        EnvironmentGeneratorSettings envSet = new EnvironmentGeneratorSettings();
        envSet.minResourceSpeed = 2;
        envSet.maxResourceSpeed = 15;
        envSet.resourceLineNum = 24;
        envSet.maxTaskLength = 100;
        envSet.minTaskLength = 10;
        HyperGeometricSettings hgSet = new HyperGeometricSettings(1000, 400, 30, 0, 30, 0, 8);
        envSet.occupGenerator = new HyperGeometricFacade(hgSet);
        envSet.timeInterval = CYCLE_LENGTH;
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

    private List<UserJob> generateJobFlow() {
        JobGenerator jg = new JobGenerator();
        JobGeneratorSettings jgs = new JobGeneratorSettings();
        jgs.taskNumber = FLOW_SIZE;

        jgs.minPrice = 1.0;
        jgs.maxPrice = 1.6;
        jgs.useSpeedPriceFactor = true;

        jgs.minTime = 50;
        jgs.maxTime = 150;

        jgs.minSpeed = 1;
        jgs.maxSpeed = 5;

        jgs.minCPU = 2;
        jgs.maxCPU = 5;

        ArrayList<UserJob> result = jg.generate(jgs);
        jg.setRandomBatchCriterias(result);

        generateJobDeadlines(result);

        return result;
    }

    private void generateJobDeadlines(List<UserJob> jobs) {
        Random random = new Random();
        for (UserJob job : jobs) {
            job.resourceRequest.deadLine = random.nextInt(MAX_DEADLINE_VALUE);
        }
    }

    @Override
    public String getData() {
        return "EXPERIMENT DATA: \n"
                + "+++++++++++++++++++++++++++\nRANDOM BATCH\n" + randomStats.getData()
                + "+++++++++++++++++++++++++++\nRANDOM PRIORITIZED BATCH\n" + formerStats.getData();
    }

    private void excludeSuccessfulJobsFromFlow(List<UserJob> flow, List<UserJob> batch) {
        for (UserJob job : batch) {
            if (job.alternatives.size() > 0 && job.bestAlternative < 0) {
                throw new IllegalStateException("Disaster!"); //TODO will this fire?
            }
            if (job.bestAlternative >= 0) {
                flow.remove(job);
            }
        }
    }

    private void adjustDeadlines(List<UserJob> flow) {
        for (UserJob job : flow) {
            job.resourceRequest.deadLine -= CYCLE_LENGTH;
        }
    }

    private boolean anyListHasElements(List... lists) {
        for (List list : lists) {
            if (!list.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private void finishExperiment() {
        randomStats.finishExperiment();
        formerStats.finishExperiment();
    }

}
