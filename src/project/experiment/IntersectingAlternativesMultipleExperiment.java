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
import project.engine.slot.slotProcessor.criteriaHelpers.MaxSumCostCriteria;
import project.engine.slot.slotProcessor.criteriaHelpers.MinRunTimeCriteria;
import project.engine.slot.slotProcessor.criteriaHelpers.MinSumCostCriteria;
import project.engine.slot.slotProcessor.criteriaHelpers.MinSumTimeCriteria;
import project.engine.slot.slotProcessor.userRankings.PercentileUserRanking;
import project.engine.slot.slotProcessor.userRankings.UserRanking;
import project.math.distributions.GaussianFacade;
import project.math.distributions.GaussianSettings;
import project.math.distributions.HyperGeometricFacade;
import project.math.distributions.HyperGeometricSettings;

/**
 *
 * @author magica
 */
public class IntersectingAlternativesMultipleExperiment extends Experiment {

    private int cycleLength = 750;

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

    protected double[] limits = {1, 7, 15, 30, 60, 100};
    protected SchedulingResultsStats[] bsStats;
    protected SchedulingResultsStats[] bsCopyStats;
    protected SchedulingResultsStats[] bsAllStats;
    protected SchedulingResultsStats[] csStats;
    protected SchedulingResultsStats[] spStats;
    protected SchedulingResultsStats[] spUserStats;

    protected void configureExperiment() {
        bsStats = new SchedulingResultsStats[limits.length];
        bsCopyStats = new SchedulingResultsStats[limits.length];
        bsAllStats = new SchedulingResultsStats[limits.length];
        csStats = new SchedulingResultsStats[limits.length];
        spStats = new SchedulingResultsStats[limits.length];
        spUserStats = new SchedulingResultsStats[limits.length];
        
        for(int i = 0; i < limits.length; i++){
            bsStats[i] = new SchedulingResultsStats();
            bsCopyStats[i] = new SchedulingResultsStats();
            bsAllStats[i] = new SchedulingResultsStats();
            csStats[i] = new SchedulingResultsStats();
            spStats[i] = new SchedulingResultsStats();
            spUserStats[i] = new SchedulingResultsStats();
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

        VOEnvironment env = generateNewEnvironment();
        ArrayList<UserJob> batch = generateJobBatch();

        for (int iLimit = 0; iLimit  < limits.length; iLimit++) {

            System.out.println("\n LIMIT = "+limits[iLimit]+"\n");
            ass2.limitSettings.constLimit = limits[iLimit];
            
            
            ArrayList<UserJob> batchBS = VOEHelper.copyJobBatchList(batch);
            ArrayList<UserJob> batchBSAll = VOEHelper.copyJobBatchList(batch);

            flush();

            /* BS */
            System.out.println("BS scheduling");
            bss.shiftAlternatives = false;
            bss.findAllPossibleAlternativesForJob = false;
            bss.slicesNum = 1;
            bs.solve(bss, env, batchBS);
            if (!SchedulingResultsStats.checkBatchForSuccess(batchBS)) {
                System.out.println("BS failed to find alternatives");
                bsStats[iLimit].addFailExperiment();
                success = false;
            }

            flush();

            /* Copy BS Solution */
            System.out.println("Copy BS scheduling");
            ArrayList<UserJob> batchBSCopy = VOEHelper.copyJobBatchList(batchBS);
            if (success) {
                css.setUseBaseSolutionFromBatch(true);
                cs.solve(css, env, batchBSCopy);
                if (!SchedulingResultsStats.checkBatchForSuccess(batchBSCopy)) {
                    System.out.println("BS Copy failed to find alternatives");
                    bsCopyStats[iLimit].addFailExperiment();
                    success = false;
                }
            }

            flush();

            /* BS with intersections, not feasible solution */
            System.out.println("BS All scheduling");
            bss.shiftAlternatives = false;
            bss.findAllPossibleAlternativesForJob = true;
            bss.slicesNum = 1;
            bs.solve(bss, env, batchBSAll);
            if (!SchedulingResultsStats.checkBatchForSuccess(batchBSAll)) {
                System.out.println("BSAll failed to find alternatives");
                bsAllStats[iLimit].addFailExperiment();
                success = false;
            }

            flush();

            /* BSS intersected based feasible solution */
            System.out.println("Copy BS All scheduling");
            css.setUseBaseSolutionFromBatch(true);
            ArrayList<UserJob> batchCS = VOEHelper.copyJobBatchList(batchBSAll);
            //if (success) {
            cs.solve(css, env, batchCS);
            if (!SchedulingResultsStats.checkBatchForSuccess(batchCS)) {
                System.out.println("CS failed to find alternatives");
                csStats[iLimit].addFailExperiment();
                success = false;
            }
            //}

            /* Simple best non-intersecting alternatives */
            ArrayList<UserJob> batchSPAll = VOEHelper.copyJobBatchList(batch);
            for (UserJob job : batchSPAll) {
                job.resourceRequest.criteria = new MaxSumCostCriteria();
            }
            if (success) {
                sp.findAlternatives(batchSPAll, env, sps, 1);
                if (!SchedulingResultsStats.checkBatchForSuccess(batchSPAll)) {
                    System.out.println("SP failed to find alternatives");
                    spStats[iLimit].addFailExperiment();
                    success = false;
                }
            }
            
            /* Simple best non-intersecting alternatives */
            ArrayList<UserJob> batchSPUSer = VOEHelper.copyJobBatchList(batch);
            if (success) {
                sp.findAlternatives(batchSPUSer, env, sps, 1);
                if (!SchedulingResultsStats.checkBatchForSuccess(batchSPUSer)) {
                    System.out.println("SP User failed to find alternatives");
                    spUserStats[iLimit].addFailExperiment();
                    success = false;
                }
            }


            if (success) {

                //SchedulerOperations.rateBatchAlternativesByBatch(batchBS, batchBSAll);
                SchedulerOperations.rateBatchAlternativesByBatch(batchBSCopy, batchBSAll);
                SchedulerOperations.rateBatchAlternativesByBatch(batchCS, batchBSAll);
                SchedulerOperations.rateBatchAlternativesByBatch(batchSPAll, batchBSAll);
                SchedulerOperations.rateBatchAlternativesByBatch(batchSPUSer, batchBSAll);

                bsStats[iLimit].processResults(batchBS);
                bsCopyStats[iLimit].processResults(batchBSCopy);
                csStats[iLimit].processResults(batchCS);
                spStats[iLimit].processResults(batchSPAll);
                spUserStats[iLimit].processResults(batchSPUSer);
                bsAllStats[iLimit].processResults(batchBSAll);

                List<Double> sim1 = new ArrayList<>();
                for (UserJob job : batchBSCopy) {
                    ExecutionSimilarityCriteria cr = (ExecutionSimilarityCriteria) job.resourceRequest.criteria;
                    sim1.add(cr.getCriteriaValue(job.getBestAlternative().window));
                }
                bsCopyStats[iLimit].addAverageList(sim1);

                List<Double> sim2 = new ArrayList<>();
                for (UserJob job : batchCS) {
                    ExecutionSimilarityCriteria cr = (ExecutionSimilarityCriteria) job.resourceRequest.criteria;
                    sim2.add(cr.getCriteriaValue(job.getBestAlternative().window));
                    //System.out.println(job.getBestAlternative().window.maxCost + " : "+job.getBestAlternative().getCost());
                }
                csStats[iLimit].addAverageList(sim2);
                
            }

            envToShow = env;
            batchToShow = batchBS;
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
        ls.constLimit = 100; //%
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
        bss.slicesNum = 1;

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
        jgs.taskNumber = 125;

        jgs.minPrice = 1.0;
        jgs.maxPrice = 1.6;
        jgs.useSpeedPriceFactor = true;

        jgs.minTime = 100;
        jgs.maxTime = 600;

        jgs.minSpeed = 1;
        jgs.maxSpeed = 1;

        jgs.minCPU = 2;
        jgs.maxCPU = 4;

        GaussianSettings gs = new GaussianSettings(0.2, 0.6, 1);
        jgs.timeCorrectiveCoefGen = new GaussianFacade(gs);

        ArrayList<UserJob> jobs = jg.generate(jgs);

        UserRanking ur = new PercentileUserRanking();
        for (UserJob job : jobs) {
            job.rankingAlgorithm = ur;
        }

        jg.setRandomBatchCriterias(jobs);

        return jobs;
    }

    private VOEnvironment generateNewEnvironment() {
        //Creating resources
        EnvironmentGeneratorSettings envSet = new EnvironmentGeneratorSettings();
        envSet.minResourceSpeed = 2;
        envSet.maxResourceSpeed = 11;
        envSet.resourceLineNum = 80;
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

    public String getData() {
        String data = "\n ==============COPY EXPERIMENT================== \n";

        for (int i = 0; i < limits.length; i++) {

            data += "\n ==================LIMIT = " + limits[i] + "==================\n";
            data += "BS STATS with LIMIT = " + limits[i] + "\n"
                    + this.bsStats[i].getData() + "\n"
                    + "BS Copy STATS with LIMIT = " + limits[i] + "\n"
                    + this.bsCopyStats[i].getData() + "\n"
                    + "BS All STATS (Not feasible solution) with LIMIT = " + limits[i] + "\n"
                    + this.bsAllStats[i].getData() + "\n"
                    + "CS STATS with LIMIT = " + limits[i] + "\n"
                    + this.csStats[i].getData() + "\n"
                    + "SP VO STATS\n"
                    + this.spStats[i].getData() + "\n"
                    + "SP User STATS\n"
                    + this.spUserStats[i].getData();
        }

        return data;
    }
}
