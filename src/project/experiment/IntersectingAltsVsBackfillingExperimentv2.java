/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package project.experiment;

import java.util.ArrayList;
import java.util.List;
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
import project.engine.scheduler.alternativeSolver.v2.AlternativeSolverSettingsV2;
import project.engine.scheduler.alternativeSolver.v2.AlternativeSolverV2;
import project.engine.scheduler.alternativeSolver.v2.LimitSettings;
import project.engine.scheduler.alternativeSolver.v2.optimization.ConfigurableLimitedOptimization;
import project.engine.scheduler.alternativeSolver.v2.optimization.OptimizationConfig;
import project.engine.scheduler.batchSlicer.BatchSlicer;
import project.engine.scheduler.batchSlicer.BatchSlicerSettings;
import project.engine.scheduler.intersect.similarity.CopyScheduler;
import project.engine.scheduler.intersect.similarity.CopySchedulerSettings;
import project.engine.slot.slotProcessor.SlotProcessorSettings;
import project.engine.slot.slotProcessor.SlotProcessorV2;
import project.engine.slot.slotProcessor.criteriaHelpers.ExecutionSimilarityCriteria;
import project.engine.slot.slotProcessor.criteriaHelpers.ICriteriaHelper;
import project.engine.slot.slotProcessor.criteriaHelpers.MaxSumCostCriteria;
import project.engine.slot.slotProcessor.criteriaHelpers.MinFinishTimeCriteria;
import project.engine.slot.slotProcessor.criteriaHelpers.MinRunTimeCriteria;
import project.engine.slot.slotProcessor.criteriaHelpers.MinStartTimeCriteria;
import project.engine.slot.slotProcessor.criteriaHelpers.MinSumCostCriteria;
import project.engine.slot.slotProcessor.criteriaHelpers.MinSumTimeCriteria;
import project.engine.slot.slotProcessor.userRankings.PercentileUserRanking;
import project.engine.slot.slotProcessor.userRankings.UserRanking;
import project.math.distributions.GaussianFacade;
import project.math.distributions.GaussianSettings;
import project.math.distributions.HyperGeometricFacade;
import project.math.distributions.HyperGeometricSettings;
import project.math.utils.MathUtils;

/**
 *
 * @author magica
 */
public class IntersectingAltsVsBackfillingExperimentv2 extends Experiment {

    private final int cycleLength = 3000;

    protected AlternativeSolverV2 as2;
    protected AlternativeSolverSettingsV2 ass2;

    protected BatchSlicer bs;
    protected BatchSlicerSettings bss;

    protected CopyScheduler cs;
    protected CopySchedulerSettings css;

    protected SlotProcessorV2 sp;
    protected SlotProcessorSettings sps;

    public ArrayList<UserJob> batchToShow;
    public VOEnvironment envToShow;

    protected double[] bfMultipliers = {1};
    //protected double[] bfMultipliers = {0, 0.1, 0.5, 0.8, 1, 1.1, 1.5};
    protected SchedulingResultsStats bsStats;
    protected SchedulingResultsStats[] antStats;
    protected SchedulingResultsStats[] tminUserStats;
    protected SchedulingResultsStats[] cminUserStats;
    protected SchedulingResultsStats bfStatsFinish;
    protected SchedulingResultsStats bfStatsStart;

    protected void configureExperiment() {
        bsStats = new SchedulingResultsStats();
        antStats = new SchedulingResultsStats[bfMultipliers.length];
        tminUserStats = new SchedulingResultsStats[bfMultipliers.length];
        cminUserStats = new SchedulingResultsStats[bfMultipliers.length];
        bfStatsFinish = new SchedulingResultsStats();
        bfStatsStart = new SchedulingResultsStats();

        for (int i = 0; i < bfMultipliers.length; i++) {
            antStats[i] = new SchedulingResultsStats();
            tminUserStats[i] = new SchedulingResultsStats();
            cminUserStats[i] = new SchedulingResultsStats();
        }

        configureAlternativeSolverV2();
        configureBatchSlicer();
        configureCopyScheduler();
        configureSlotProcessorV2();
    }

    @Override
    public void performExperiments(int expNum) {

        configureExperiment();

        for (int i = 0; i < expNum; i++) {
            System.out.println("Experiment #" + i);
            performExperiment();
        }
    }

    protected void performExperiment() {

        boolean success = true;

        ArrayList<UserJob> batch = generateJobBatch();

        VOEnvironment env = generateNewEnvironment(25);

        System.out.println("\n SIZE = " + 25 + "\n");

        ArrayList<UserJob> batchBS = VOEHelper.copyJobBatchList(batch);

        flush();

        /* BS */
        System.out.println("BS scheduling");
        bss.shiftAlternatives = true;
        bss.findAllPossibleAlternativesForJob = false;
        bs.solve(bss, env, batchBS);
//        if (!SchedulingResultsStats.checkBatchForSuccess(batchBS)) {
//            System.out.println("BS failed to find alternatives");
//            bsStats.addFailExperiment();
//            success = false;
//        }
        flush();

        /* Simple backfilling solution with finish time criterion */
        System.out.println("BF-FINISH scheduling");
        ArrayList<UserJob> batchBFFinish = VOEHelper.copyJobBatchList(batch);
        for (UserJob job : batchBFFinish) {
            job.resourceRequest.initialCriteria = job.resourceRequest.criteria.getClass();
            job.resourceRequest.criteria = new MinFinishTimeCriteria();
        }

        sp.findAlternatives(batchBFFinish, env, sps, 1);
        for (UserJob job : batchBFFinish) {
            if (!job.alternatives.isEmpty()) {
                job.bestAlternative = 0;
            }
        }
        if (!SchedulingResultsStats.checkBatchForSuccess(batchBFFinish)) {
            System.out.println("BF-FINISH failed to find alternatives");
            bfStatsFinish.addFailExperiment();
            success = false;
        }

        flush();

        /* Simple backfilling solution with start time criterion */
        System.out.println("BF-START scheduling");
        ArrayList<UserJob> batchBFStart = VOEHelper.copyJobBatchList(batch);
        for (UserJob job : batchBFStart) {
            job.resourceRequest.initialCriteria = job.resourceRequest.criteria.getClass();
            job.resourceRequest.criteria = new MinStartTimeCriteria();
        }

        sp.findAlternatives(batchBFStart, env, sps, 1);
        for (UserJob job : batchBFStart) {
            if (!job.alternatives.isEmpty()) {
                job.bestAlternative = 0;
            }
        }
        if (!SchedulingResultsStats.checkBatchForSuccess(batchBFStart)) {
            System.out.println("BF-START failed to find alternatives");
            bfStatsFinish.addFailExperiment();
            success = false;
        }

        if (success) {
            
            /* calculating average placing errors for BF */
            List<Double> bfOrderErrors = new ArrayList<>();
            Double averageError = 0d;
            for (UserJob job : batchBFFinish) {
                double error = getJobOrderError(job, batchBFFinish);
                bfOrderErrors.add(error);
                averageError += error;
            }
            bfOrderErrors.add(0d);  /* delimeter */
            averageError /= batchBFFinish.size();
            bfOrderErrors.add(averageError);

            /* calculating test error */
            for (UserJob job : batchBFFinish) {
                double error = getJobOrderError(job, batchBFFinish, batchBFFinish);
                if (error > 0) {
                    throw new RuntimeException("Error in order calculation");
                }
            }
            
            //bsStats.processResults(batchBS);
            bfStatsFinish.processResults(batchBFFinish);
            bfStatsStart.processResults(batchBFStart);
            bfStatsFinish.addAverageList(bfOrderErrors);

        }

        for (int iVal = 0; iVal < bfMultipliers.length; iVal++) {

            if (bfMultipliers[iVal] == 0) {
                /* special case to get even closer to BF */
                ExecutionSimilarityCriteria.startTimeWeight = 0;
                ExecutionSimilarityCriteria.finishTimeWeight = 1;
                ExecutionSimilarityCriteria.costWeight = 0;
                ExecutionSimilarityCriteria.timeWeight = 0;
            } else {
                ExecutionSimilarityCriteria.startTimeWeight = 1;
                ExecutionSimilarityCriteria.finishTimeWeight = 1;
                ExecutionSimilarityCriteria.costWeight = 1;
                ExecutionSimilarityCriteria.timeWeight = 1;
            }

            flush();

            /* Anticipation solution */
            System.out.println("\nAnticipation scheduling with VAR = " + bfMultipliers[iVal] + "\n");
            css.setUseBaseSolutionFromBatch(false);

            /* USING BF as etalon for starting jobs */
            css.setStartTimeBaseSolution(batchBFFinish);
            css.starttimeMultiplier = bfMultipliers[iVal];

            if (bfMultipliers[iVal] == 0) {
                /* special case to get even closer to BF */
                css.setRuntimeBaseSolution(batchBFStart);
            } else {
                css.proctimeMultiplier = 0.78d;
                css.runtimeMultiplier = 0.78d;
            }

            bss.findAllPossibleAlternativesForJob = true;
            bss.shiftAlternatives = false;
            ArrayList<UserJob> batchCS = VOEHelper.copyJobBatchList(batch);

            cs.solve(css, env, batchCS);
            if (!SchedulingResultsStats.checkBatchForSuccess(batchCS)) {
                System.out.println("Anticipation failed to find alternatives");
                antStats[iVal].addFailExperiment();
                success = false;
            }

            if (success) {

                ArrayList<UserJob> minCostJobs = new ArrayList<>();
                ArrayList<UserJob> minTimeJobs = new ArrayList<>();
                double minCostCost = 0;
                double minTimeCost = 0;
                double minCostTime = 0;
                double minTimeTime = 0;
                double startTime = 0;

                for (UserJob job : batchCS) {
                    ICriteriaHelper jobCriterion = job.resourceRequest.criteria;
                    if (jobCriterion instanceof ExecutionSimilarityCriteria) {
                        ExecutionSimilarityCriteria ec = (ExecutionSimilarityCriteria) jobCriterion;
                        if (ec.getPreviousCriterionClass().equals(MinSumCostCriteria.class)) {
                            minCostJobs.add(job);
                            minCostCost += ec.getPatternObject().getCost();
                            minCostTime += ec.getPatternObject().getRuntime();
                            startTime += ec.getPatternObject().getStartTime();
                        } else if (ec.getPreviousCriterionClass().equals(MinRunTimeCriteria.class)) {
                            minTimeJobs.add(job);
                            minTimeCost += ec.getPatternObject().getCost();
                            minTimeTime += ec.getPatternObject().getRuntime();
                            startTime += ec.getPatternObject().getStartTime();
                        } else {
                            throw new RuntimeException("wow, we have a job with unexpected previous criterion " + jobCriterion.getClass());
                        }
                    } else {
                        throw new RuntimeException("wow, we have a job with unexpected criterion " + jobCriterion.getClass());
                    }
                }

                minCostCost /= minCostJobs.size();
                minTimeCost /= minTimeJobs.size();
                minCostTime /= minCostJobs.size();
                minTimeTime /= minTimeJobs.size();
                startTime /= batchCS.size();

                /* List to save average reference values across the experiments */
                List<Double> refList = new ArrayList<>();
                refList.add(minCostCost);
                refList.add(minTimeCost);
                refList.add(minCostTime);
                refList.add(minTimeTime);

                System.out.println("Pattern StartTime:" + startTime);
                System.out.println("Pattern Time t:c " + minTimeTime + ":" + minCostTime);
                System.out.println("Pattern Cost t:c " + minTimeCost + ":" + minCostCost);

                /* calculating average placing errors for CS */
                List<Double> csOrderErrors = new ArrayList<>();
                Double averageError = 0d;
                for (UserJob job : batchCS) {
                    double error = getJobOrderError(job, batchCS, batchBFFinish);
                    csOrderErrors.add(error);
                    averageError += error;
                }
                csOrderErrors.add(0d);  /* delimeter */
                averageError /= batchCS.size();
                csOrderErrors.add(averageError); /* average value at the end */

                antStats[iVal].processResults(batchCS);
                antStats[iVal].addAverageList(csOrderErrors);
                //antStats[iVal].addAverageList(refList);
                tminUserStats[iVal].processResults(minTimeJobs);
                cminUserStats[iVal].processResults(minCostJobs);
            }

            batchToShow = batchCS;
        }

        envToShow = env;

        int a = 0;
    }

    protected void configureSlotProcessorV2() {
        sp = new SlotProcessorV2();
        sps = new SlotProcessorSettings();

        sps.algorithmConcept = SlotProcessorSettings.CONCEPT_EXTREME;
        sps.algorithmType = SlotProcessorSettings.TYPE_MODIFIED;
        sps.cycleLength = cycleLength;
        sps.cycleStart = 0;

        sps.findAllPossibleAlternativesForJob = false;
    }

    protected void configureAlternativeSolverV2() {
        as2 = new AlternativeSolverV2();
        ass2 = new AlternativeSolverSettingsV2();

        ass2.optType = AlternativeSolverSettingsV2.MAX;
        ass2.secondaryOptType = AlternativeSolverSettingsV2.MAX;
        ass2.periodStart = 0;
        ass2.periodEnd = cycleLength;
        ass2.cleanUpInferiorAlternatives = false;

        LimitSettings ls = new LimitSettings();
        ls.limitType = LimitSettings.LIMIT_TYPE_CONST_PROPORTIONAL;
        ls.constLimit = 10; //%
        ls.limitStepQuotient = 100;      // For each limit we will have 100 steps in incremental mode
        //ls.limitStep = 2d;
        ass2.limitSettings = ls;

        OptimizationConfig config = new ConfigurableLimitedOptimization(
                ConfigurableLimitedOptimization.COST, /* optimization */
                ConfigurableLimitedOptimization.COST, /* secondary optimization */
                ConfigurableLimitedOptimization.USER);/* limit */

        ass2.optimizationConfig = config;
    }

    protected void configureBatchSlicer() {
        bs = new BatchSlicer();
        bss = new BatchSlicerSettings(as2, ass2);

        bss.periodStart = 0;
        bss.periodEnd = cycleLength;

        bss.sliceAlgorithm = BatchSlicerSettings.defaultOrder;
        bss.slicesNum = 2;

        bss.spAlgorithmType = SlotProcessorSettings.TYPE_MODIFIED;
        bss.spConceptType = SlotProcessorSettings.CONCEPT_EXTREME;
        bss.findAllPossibleAlternativesForJob = false;

        bss.shiftAlternatives = true;
    }

    protected void configureCopyScheduler() {
        css = new CopySchedulerSettings(bs, bss);
        cs = new CopyScheduler(css);
    }

    private ArrayList<UserJob> generateJobBatch() {
        JobGenerator jg = new JobGenerator();
        JobGeneratorSettings jgs = new JobGeneratorSettings();
        jgs.taskNumber = 74;

        jgs.minPrice = 1.0;
        jgs.maxPrice = 1.6;
        jgs.useSpeedPriceFactor = true;

        jgs.minTime = 100;
        jgs.maxTime = 600;

        jgs.minSpeed = 1;
        jgs.maxSpeed = 1;

        jgs.minCPU = 2;
        jgs.maxCPU = 5;

        GaussianSettings gs = new GaussianSettings(0.2, 0.6, 1);
        jgs.timeCorrectiveCoefGen = new GaussianFacade(gs);

        ArrayList<UserJob> jobs = jg.generate(jgs);

        UserRanking ur = new PercentileUserRanking();
        for (UserJob job : jobs) {
            job.rankingAlgorithm = ur;
        }

        setRandomRequestCriteria(jobs, 50);

        return jobs;
    }

    private VOEnvironment generateNewEnvironment(int nodesNumber) {
        //Creating resources
        EnvironmentGeneratorSettings envSet = new EnvironmentGeneratorSettings();
        envSet.minResourceSpeed = 2;
        envSet.maxResourceSpeed = 11;
        envSet.resourceLineNum = nodesNumber;
        envSet.maxTaskLength = 100;
        envSet.minTaskLength = 10;
        HyperGeometricSettings hgSet = new HyperGeometricSettings(1000, 150, 30, 0, 10, 0, 2);
        envSet.occupGenerator = new HyperGeometricFacade(hgSet);
        envSet.timeInterval = cycleLength * 4;
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

    protected void flush() {
        bs.flush();
        cs.flush();
        as2.flush();
    }

    protected void setRandomRequestCriteria(List<UserJob> jobs, int costPercent) {
        for (UserJob job : jobs) {
            int percent = MathUtils.getUniform(0, 100);

            if (percent < costPercent) {
                job.resourceRequest.criteria = new MinSumCostCriteria();
            } else {
                job.resourceRequest.criteria = new MinRunTimeCriteria();
            }

            try {
                Thread.sleep(2);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public String getData() {
        String data = "\n ==============COPY EXPERIMENT================== \n";

        data += "BS STATS\n"
                + this.bsStats.getData() + "\n"
                + "BF START STATS\n"
                + this.bfStatsStart.getData() + "\n"
                + "BF FINISH STATS\n"
                + this.bfStatsFinish.getData() + "\n";

        for (int i = 0; i < bfMultipliers.length; i++) {

            data += "\n ================== VAR = " + bfMultipliers[i] + "==================\n";
            data += "Anticipation STATS with VAR = " + bfMultipliers[i] + "\n"
                    + this.antStats[i].getData() + "\n"
                    + "Anticipation Min Cost STATS with VAR = " + bfMultipliers[i] + "\n"
                    + this.cminUserStats[i].getData() + "\n"
                    + "Anticipation Min Time STATS with VAR = " + bfMultipliers[i] + "\n"
                    + this.tminUserStats[i].getData() + "\n";

        }

        return data;
    }

    protected int getJobOrderError(UserJob job, List<UserJob> batch) {
        int error = Integer.MAX_VALUE;
        int i = 0;
        int orderPlace = 0;
        int realPlace = 0;

        double startTime = job.getBestAlternative().getStart();

        for (UserJob nextJob : batch) {
            if (job.id == nextJob.id) {
                orderPlace = i;
            } else {
                i++;
                if (startTime > nextJob.getBestAlternative().getStart()) {
                    realPlace++;
                }
            }
        }

        error = Math.abs(realPlace - orderPlace);

        return error;
    }

    protected int getJobOrderError(UserJob job, List<UserJob> batch, List<UserJob> refJobs) {
        int refPlace = getJobRealPlace(job, refJobs);
        int realPlace = getJobRealPlace(job, batch);

        return Math.abs(realPlace - refPlace);
    }

    protected int getJobRealPlace(UserJob job, List<UserJob> jobs) {
        int realPlace = 0;
        double startTime = job.getBestAlternative().getStart();

        for (UserJob nextJob : jobs) {
            if (job.id != nextJob.id && startTime > nextJob.getBestAlternative().getStart()) {
                realPlace++;
            }
        }

        return realPlace;
    }
}
