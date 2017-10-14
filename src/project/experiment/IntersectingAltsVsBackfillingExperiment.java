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
import project.engine.scheduler.SchedulerOperations;
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
public class IntersectingAltsVsBackfillingExperiment extends Experiment {

    private int cycleLength = 3000;

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

    protected int[] nodesNum = {25, 30, 40};
    protected SchedulingResultsStats[] bsStats;
    protected SchedulingResultsStats[] antStats;
    protected SchedulingResultsStats[] tminUserStats;
    protected SchedulingResultsStats[] cminUserStats;
    protected SchedulingResultsStats[] bfStats;

    protected void configureExperiment() {
        bsStats = new SchedulingResultsStats[nodesNum.length];
        antStats = new SchedulingResultsStats[nodesNum.length];
        tminUserStats = new SchedulingResultsStats[nodesNum.length];
        cminUserStats = new SchedulingResultsStats[nodesNum.length];
        bfStats = new SchedulingResultsStats[nodesNum.length];

        for (int i = 0; i < nodesNum.length; i++) {
            bsStats[i] = new SchedulingResultsStats();
            antStats[i] = new SchedulingResultsStats();
            tminUserStats[i] = new SchedulingResultsStats();
            cminUserStats[i] = new SchedulingResultsStats();
            bfStats[i] = new SchedulingResultsStats();
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

        for (int iNodes = 0; iNodes < nodesNum.length; iNodes++) {

            VOEnvironment env = generateNewEnvironment(nodesNum[iNodes]);

            System.out.println("\n SIZE = " + nodesNum[iNodes] + "\n");

            ArrayList<UserJob> batchBS = VOEHelper.copyJobBatchList(batch);

            flush();

            /* BS */
            System.out.println("BS scheduling");
            bss.shiftAlternatives = true;
            bss.findAllPossibleAlternativesForJob = false;
            //bss.slicesNum = 1;
            bs.solve(bss, env, batchBS);
            if (!SchedulingResultsStats.checkBatchForSuccess(batchBS)) {
                System.out.println("BS failed to find alternatives");
                bsStats[iNodes].addFailExperiment();
                success = false;
            }

            flush();

            /* Simple backfilling solution */
            System.out.println("BF scheduling");
            ArrayList<UserJob> batchBF = VOEHelper.copyJobBatchList(batch);
            for (UserJob job : batchBF) {
                job.resourceRequest.initialCriteria = job.resourceRequest.criteria.getClass();
                job.resourceRequest.criteria = new MinFinishTimeCriteria();
            }

            sp.findAlternatives(batchBF, env, sps, 1);
            for(UserJob job : batchBF){
                if(!job.alternatives.isEmpty()){
                    job.bestAlternative = 0;
                }
            }
            if (!SchedulingResultsStats.checkBatchForSuccess(batchBF)) {
                System.out.println("BF failed to find alternatives");
                bfStats[iNodes].addFailExperiment();
                success = false;
            }

            flush();

            /* Anticipation solution */
            System.out.println("Anticipation scheduling");
            css.setUseBaseSolutionFromBatch(false);
            css.setStartTimeBaseSolution(batchBF);
            //css.proctimeMultiplier = 0.78d;
            //css.runtimeMultiplier = 0.78d;
            /* USING BF as etalon for starting jobs */
            bss.findAllPossibleAlternativesForJob = true;
            bss.shiftAlternatives = false;
            ArrayList<UserJob> batchCS = VOEHelper.copyJobBatchList(batch);
            //if (success) {
            cs.solve(css, env, batchCS);
            if (!SchedulingResultsStats.checkBatchForSuccess(batchCS)) {
                System.out.println("Anticipation failed to find alternatives");
                antStats[iNodes].addFailExperiment();
                success = false;
            }
            //}

            if (success) {

                //SchedulerOperations.rateBatchAlternativesByBatch(batchBS, batchBSAll);
                //SchedulerOperations.rateBatchAlternativesByBatch(batchCS, batchBSAll);
                //SchedulerOperations.rateBatchAlternativesByBatch(batchBF, batchBSAll);

                ArrayList<UserJob> minCostJobs = new ArrayList<>();
                ArrayList<UserJob> minTimeJobs = new ArrayList<>();
                double minCostCost = 0;
                double minTimeCost = 0;
                double minCostTime = 0;
                double minTimeTime = 0;

                for (UserJob job : batchCS) {
                    ICriteriaHelper jobCriterion = job.resourceRequest.criteria;
                    if (jobCriterion instanceof ExecutionSimilarityCriteria) {
                        ExecutionSimilarityCriteria ec = (ExecutionSimilarityCriteria) jobCriterion;
                        if (ec.getPreviousCriterionClass().equals(MinSumCostCriteria.class)) {
                            minCostJobs.add(job);
                            minCostCost += ec.getPatternObject().getCost();
                            minCostTime += ec.getPatternObject().getRuntime();
                        } else if (ec.getPreviousCriterionClass().equals(MinRunTimeCriteria.class)) {
                            minTimeJobs.add(job);
                            minTimeCost += ec.getPatternObject().getCost();
                            minTimeTime += ec.getPatternObject().getRuntime();
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

                List<Double> refList = new ArrayList<>();
                refList.add(minCostCost);
                refList.add(minTimeCost);
                refList.add(minCostTime);
                refList.add(minTimeTime);
                
                System.out.println("Time t:c " + minTimeTime + ":" + minCostTime);
                System.out.println("Cost t:c " + minTimeCost + ":" + minCostCost);
                
                bsStats[iNodes].processResults(batchBS);
                antStats[iNodes].processResults(batchCS);
                antStats[iNodes].addAverageList(refList);
                tminUserStats[iNodes].processResults(minTimeJobs);
                cminUserStats[iNodes].processResults(minCostJobs);
                bfStats[iNodes].processResults(batchBF);

            }

            envToShow = env;
            batchToShow = batchCS;
        }
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
        //ls.limitType = LimitSettings.LIMIT_TYPE_AVERAGE;
        ls.limitType = LimitSettings.LIMIT_TYPE_CONST_PROPORTIONAL;
        ls.constLimit = 10; //%
        ls.limitStepQuotient = 100;      // For each limit we will have 100 steps in incremental mode
        //ls.limitStep = 2d;
        ass2.limitSettings = ls;

        OptimizationConfig config = new ConfigurableLimitedOptimization(
                ConfigurableLimitedOptimization.COST, /* optimization */
                ConfigurableLimitedOptimization.COST, /* secondary optimization */
                ConfigurableLimitedOptimization.USER);
        /* limit */

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
        //envSet.occupancyLevel = 1;
        HyperGeometricSettings hgSet = new HyperGeometricSettings(1000, 150, 30, 0, 10, 0, 2);
        envSet.occupGenerator = new HyperGeometricFacade(hgSet);
        envSet.timeInterval = cycleLength * 4;
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

        for (int i = 0; i < nodesNum.length; i++) {

            data += "\n ================== NODES = " + nodesNum[i] + "==================\n";
            data += "BS STATS with NODE = " + nodesNum[i] + "\n"
                    + this.bsStats[i].getData() + "\n"
                    + "Anticipation STATS with NODES = " + nodesNum[i] + "\n"
                    + this.antStats[i].getData() + "\n"
                    + "Anticipation Min Cost STATS with NODES = " + nodesNum[i] + "\n"
                    + this.cminUserStats[i].getData() + "\n"
                    + "Anticipation Min Time STATS with NODES = " + nodesNum[i] + "\n"
                    + this.tminUserStats[i].getData() + "\n"
                    + "BF STATS with NODES = " + nodesNum[i] + "\n"
                    + this.bfStats[i].getData() + "\n"
                    + "BF Min Cost STATS with NODES = " + nodesNum[i];
        }

        return data;
    }
}
