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
import project.engine.slot.slotProcessor.criteriaHelpers.MinFinishTimeCriteria;
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
public class CopySchedulerExperiment extends Experiment {

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

    protected SchedulingResultsStats bsStats;
    protected SchedulingResultsStats bsModuleCopyStats;
    protected SchedulingResultsStats bsSpaceCopyStats;
    protected SchedulingResultsStats spErrorStats;

    protected void configureExperiment() {
        bsStats = new SchedulingResultsStats();
        bsModuleCopyStats = new SchedulingResultsStats();
        bsSpaceCopyStats = new SchedulingResultsStats();
        spErrorStats = new SchedulingResultsStats();

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

        ArrayList<UserJob> batchBS = VOEHelper.copyJobBatchList(batch);

        flush();

        /* BS */
        System.out.println("BS scheduling");
        bss.shiftAlternatives = true;
        bss.findAllPossibleAlternativesForJob = false;
        bss.slicesNum = 2;
        bs.solve(bss, env, batchBS);
        if (!SchedulingResultsStats.checkBatchForSuccess(batchBS)) {
            System.out.println("BS failed to find alternatives");
            bsStats.addFailExperiment();
            success = false;
        }

        flush();

        /* Module Copy BS Solution */
        System.out.println("Module Copy BS scheduling");
        ExecutionSimilarityCriteria.spaceDistance = false;
        ArrayList<UserJob> batchBSModuleCopy = VOEHelper.copyJobBatchList(batchBS);
        if (success) {
            css.setUseBaseSolutionFromBatch(true);
            cs.solve(css, env, batchBSModuleCopy);
            if (!SchedulingResultsStats.checkBatchForSuccess(batchBSModuleCopy)) {
                System.out.println("BS Module Copy failed to find alternatives");
                bsModuleCopyStats.addFailExperiment();
                success = false;
            }
        }

        List<Double> moduleErrors = new ArrayList<>();
        if (success) {
            for (UserJob job : batchBSModuleCopy) {
                ExecutionSimilarityCriteria cr = (ExecutionSimilarityCriteria) job.resourceRequest.criteria;
                moduleErrors.add(cr.getCriteriaValue(job.getBestAlternative().window));
            }
        }

        flush();

        /* Space Copy BS Solution */
        System.out.println("Space Copy BS scheduling");
        ExecutionSimilarityCriteria.spaceDistance = true;
        ArrayList<UserJob> batchBSSpaceCopy = VOEHelper.copyJobBatchList(batchBS);
        if (success) {
            css.setUseBaseSolutionFromBatch(true);
            cs.solve(css, env, batchBSSpaceCopy);
            if (!SchedulingResultsStats.checkBatchForSuccess(batchBSSpaceCopy)) {
                System.out.println("BS Space Copy failed to find alternatives");
                bsModuleCopyStats.addFailExperiment();
                success = false;
            }
        }

        List<Double> spaceErrors = new ArrayList<>();
        if (success) {
            for (UserJob job : batchBSSpaceCopy) {
                ExecutionSimilarityCriteria cr = (ExecutionSimilarityCriteria) job.resourceRequest.criteria;
                spaceErrors.add(cr.getCriteriaValue(job.getBestAlternative().window));
            }
        }

        flush();

        /* SP error test */
        UserJob spJobMax = batch.get(0).clone();
        spJobMax.resourceRequest.criteria = new MaxSumCostCriteria();
        UserJob spJobMin = batch.get(0).clone();
        spJobMin.resourceRequest.criteria = new MinSumCostCriteria();

        ArrayList<UserJob> batchSP = new ArrayList<>();
        batchSP.add(spJobMax);
        batchSP.add(spJobMin);
        sp.findAlternatives(batchSP, env, sps, 1);
        
        List<Double> spErrors = new ArrayList<>();

        if (!spJobMax.alternatives.isEmpty() && !spJobMin.alternatives.isEmpty()) {
            ExecutionSimilarityCriteria esc = new ExecutionSimilarityCriteria();
            esc.setBaseAlternative(spJobMax.alternatives.get(0));
            
            ExecutionSimilarityCriteria.spaceDistance = false;
            spErrors.add(esc.getCriteriaValue(spJobMin.alternatives.get(0).window));
            
            ExecutionSimilarityCriteria.spaceDistance = true;
            spErrors.add(esc.getCriteriaValue(spJobMin.alternatives.get(0).window));
            
        } else {
            System.out.println("SP failed to find alternatives");
            spErrorStats.addFailExperiment();
            success = false;
        }

        if (success) {

            SchedulerOperations.rateBatchAlternativesByBatch(batchBSModuleCopy, batchBS);
            SchedulerOperations.rateBatchAlternativesByBatch(batchBSSpaceCopy, batchBS);

            bsStats.processResults(batchBS);
            bsModuleCopyStats.processResults(batchBSModuleCopy);
            bsSpaceCopyStats.processResults(batchBSSpaceCopy);

            bsModuleCopyStats.addAverageList(moduleErrors);
            bsSpaceCopyStats.addAverageList(spaceErrors);
            
            spErrorStats.addAverageList(spErrors);

        }

        envToShow = env;
        batchToShow = batchBS;

        int a = 0;
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
        bss.slicesNum = 2;

        bss.spAlgorithmType = SlotProcessorSettings.TYPE_MODIFIED;
        bss.spConceptType = SlotProcessorSettings.CONCEPT_EXTREME;
        bss.findAllPossibleAlternativesForJob = false;

        bss.shiftAlternatives = true;
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
        jgs.maxCPU = 6;

        GaussianSettings gs = new GaussianSettings(0.2, 0.6, 1);
        jgs.timeCorrectiveCoefGen = new GaussianFacade(gs);

        ArrayList<UserJob> jobs = jg.generate(jgs);

        UserRanking ur = new PercentileUserRanking();
        for (UserJob job : jobs) {
            job.rankingAlgorithm = ur;
        }

        distributeJobsPreferences(jobs);

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

    protected void distributeJobsPreferences(List<UserJob> batch) {
        int i = 0;
        for (UserJob job : batch) {
            int d = i % 2;

            switch (d) {
                case 0:
                    job.resourceRequest.criteria = new MinFinishTimeCriteria();
                    break;
                case 1:
                    job.resourceRequest.criteria = new MinFinishTimeCriteria();
                    break;
                default:
                    job.resourceRequest.criteria = new MinFinishTimeCriteria();
                    break;
            }

            i++;
        }
    }

    public String getData() {
        String data = "\n ============== SIMILARITY COPY EXPERIMENT ================== \n";

        data += "BS STATS\n"
                + this.bsStats.getData() + "\n"
                + "BS Module Copy STATS with LIMIT\n"
                + this.bsModuleCopyStats.getData() + "\n"
                + "BS Space Copy STATS with LIMIT\n"
                + this.bsSpaceCopyStats.getData() + "\n"
                + "SP ERROR STATS with LIMIT\n"
                + this.spErrorStats.getData() + "\n";

        return data;
    }
}
