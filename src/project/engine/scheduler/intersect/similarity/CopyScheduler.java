package project.engine.scheduler.intersect.similarity;

import java.util.ArrayList;
import project.engine.data.UserJob;
import project.engine.data.VOEHelper;
import project.engine.data.VOEnvironment;
import project.engine.scheduler.Scheduler;
import project.engine.scheduler.SchedulerOperations;
import project.engine.scheduler.SchedulerSettings;
import project.engine.slot.slotProcessor.SlotProcessorSettings;
import project.engine.slot.slotProcessor.SlotProcessorV2;
import project.engine.slot.slotProcessor.criteriaHelpers.ExecutionSimilarityCriteria;
import project.engine.slot.slotProcessor.criteriaHelpers.PatternObject;

/**
 *
 * @author magica
 */
public class CopyScheduler extends Scheduler {

    protected CopySchedulerSettings csSettings;

    protected SlotProcessorV2 sp;
    protected SlotProcessorSettings sps;

    protected VOEnvironment voenv;
    protected ArrayList<UserJob> batch;

    public CopyScheduler(CopySchedulerSettings settings) {
        this.csSettings = settings;
        configureSlotProcessor();
    }

    @Override
    public void solve(SchedulerSettings settings, VOEnvironment voenv, ArrayList<UserJob> batch) {
        this.voenv = voenv;
        this.batch = batch;

        ArrayList<UserJob> baseBatch = VOEHelper.copyJobBatchList(this.batch);

        /* 1. Obtaining base solution with a general algorithm */
        if (!csSettings.isUseBaseSolutionFromBatch()) {
            SchedulerOperations.clearBatchAlternatives(baseBatch);
            solveBaseProblem(baseBatch);
        }

        /* 2. Trying to find feasible alternatives similar to ones from base solution */
        findSimilarSolution(baseBatch);
    }

    @Override
    public void flush() {
        csSettings.getLocalScheduler().flush();
    }

    protected void solveBaseProblem(ArrayList<UserJob> collisionBatch) {
        VOEnvironment tempEnv = VOEHelper.copyEnvironment(voenv);
        csSettings.getLocalScheduler().solve(csSettings.getSchedulerSettings(), tempEnv, collisionBatch);
    }

    protected void findSimilarSolution(ArrayList<UserJob> baseBatch) {

        /* HERE need to initialaize critrria helper for each job in batch based on same jobs from collision batch */
        boolean allPatternsConfigured = true;
        for (UserJob job : batch) {
            for (UserJob baseJob : baseBatch) {
                if (job.id == baseJob.id) {
                    ExecutionSimilarityCriteria criterion = new ExecutionSimilarityCriteria();
                    PatternObject po;
                    if (baseJob.bestAlternative >= 0) {
                        po = new PatternObject(baseJob.getBestAlternative());
                    } else {
                        po = new PatternObject();
                    }

                    if (!additionalPatternUpdate(job, po)) {
                        allPatternsConfigured = false;
                    }
                    criterion.setPatternObject(po);
                    criterion.setPreviousCriterionClass(job.resourceRequest.criteria.getClass());
                    job.resourceRequest.criteria = criterion;
                    break; // go to next real job
                }
            }
        }

        if (allPatternsConfigured) {
            /* We use SP to find one feasible alternative for each job with maximum similarity to collision batch best alternative */
            sp.findAlternatives(batch, voenv, sps, 1);
        }
        
        /* Setting the only found alternative as best */
        for (UserJob job : batch) {
            if (!job.alternatives.isEmpty()) {
                job.bestAlternative = 0;
            } else {
                job.bestAlternative = -1;
            }
        }

    }

    protected void configureSlotProcessor() {

        sp = new SlotProcessorV2();
        sps = new SlotProcessorSettings();

        sps.algorithmConcept = SlotProcessorSettings.CONCEPT_EXTREME;
        sps.algorithmType = SlotProcessorSettings.TYPE_MODIFIED;

        sps.cycleLength = csSettings.getSchedulerSettings().periodEnd - csSettings.getSchedulerSettings().periodStart;
        sps.cycleStart = csSettings.getSchedulerSettings().periodStart;

        sps.findAllPossibleAlternativesForJob = false;

    }

    private boolean additionalPatternUpdate(UserJob job, PatternObject po) {
        /* setting new base value */
        if (csSettings.costBaseSolution != null) {
            for (UserJob baseJob : csSettings.costBaseSolution) {
                if (job.id == baseJob.id) {
                    if (baseJob.bestAlternative >= 0) {
                        po.setCost(baseJob.getBestAlternative().getCost());
                    }
                }
            }
        }
        
        if(po.getCost() == null){ /* empty cost value */
            return false;
        }
        /* adjusting value by coefficient */
        po.setCost(po.getCost()*csSettings.costMultiplier);
        

        /* setting new base value */
        if (csSettings.starttimeBaseSolution != null) {
            for (UserJob baseJob : csSettings.starttimeBaseSolution) {
                if (job.id == baseJob.id) {
                    if (baseJob.bestAlternative >= 0) {
                        po.setStartTime(baseJob.getBestAlternative().getStart());
                    }
                }
            }
        }
        
        if(po.getStartTime() == null){ /* empty StartTime value */
            return false;
        }
        /* adjusting value by coefficient */
        po.setStartTime(po.getStartTime()*csSettings.starttimeMultiplier);

        /* setting new base value */
        if (csSettings.runtimeBaseSolution != null) {
            for (UserJob baseJob : csSettings.runtimeBaseSolution) {
                if (job.id == baseJob.id) {
                    if (baseJob.bestAlternative >= 0) {
                        po.setRuntime(baseJob.getBestAlternative().getRuntime());
                    }
                }
            }
        }
        
        if(po.getRuntime() == null){ /* empty Runtime value */
            return false;
        }
        /* adjusting value by coefficient */
        po.setRuntime(po.getRuntime()*csSettings.runtimeMultiplier);

        /* setting new base value */
        if (csSettings.proctimeBaseSolution != null) {
            for (UserJob baseJob : csSettings.proctimeBaseSolution) {
                if (job.id == baseJob.id) {
                    if (baseJob.bestAlternative >= 0) {
                        po.setProctime(baseJob.getBestAlternative().getLength());
                    }
                }
            }
        }
        
        if(po.getProctime() == null){ /* empty Proctime value */
            return false;
        }
        /* adjusting value by coefficient */
        po.setProctime(po.getProctime()*csSettings.proctimeMultiplier);

        if (po.getCost() == null || po.getStartTime() == null
                || po.getRuntime() == null || po.getProctime() == null) {

            System.out.println("Pattern isn't filled for Job " + job.name);
            return false;
        }

        return true;

    }

}
