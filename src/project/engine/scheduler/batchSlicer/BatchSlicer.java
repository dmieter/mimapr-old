/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package project.engine.scheduler.batchSlicer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import project.engine.data.Alternative;
import project.engine.data.Slot;
import project.engine.data.UserJob;
import project.engine.data.VOEHelper;
import project.engine.data.VOEnvironment;
import project.engine.scheduler.Scheduler;
import project.engine.scheduler.SchedulerOperations;
import project.engine.scheduler.SchedulerSettings;
import project.engine.scheduler.alternativeSolver.v1.AlternativeSolver;
import project.engine.slot.slotProcessor.SlotProcessorSettings;
import project.engine.slot.slotProcessor.SlotProcessorV2;
import project.engine.slot.slotProcessor.criteriaHelpers.MinSumCostCriteria;

/**
 *
 * @author Magica
 */
public class BatchSlicer extends Scheduler {

    public VOEnvironment env = null;       //environment to work with
    public ArrayList<ArrayList<UserJob>> subBatches = new ArrayList<ArrayList<UserJob>>();
    public BatchSlicerSettings bsSettings = null;

    public void solve(SchedulerSettings settings, VOEnvironment voenv, ArrayList<UserJob> batch) {

        if (batch.isEmpty()) {
            System.out.println("BatchSlicer: input batch is empty");
            return;
        }

        env = VOEHelper.copyEnvironment(voenv);        //copy to use for temporary assignments
        applySettings(settings);

        sliceBatch(batch);

        SlotProcessorSettings spSettings = fillSPSettings();
        SlotProcessorV2 sp = new SlotProcessorV2();

        ArrayList<Slot> slots = null;

        for (Iterator it = subBatches.iterator(); it.hasNext();) {
            ArrayList<UserJob> subBatch = (ArrayList<UserJob>) it.next();

            if (!bsSettings.alternativesAlreadyFound) {
                slots = VOEHelper.getSlotsFromVOE(env, bsSettings.periodStart, bsSettings.periodEnd);
                sp.findAlternatives(subBatch, slots, spSettings);
                rankAlternatives(subBatch);
            }

            bsSettings.localScheduler.flush();
            bsSettings.localScheduler.solve(bsSettings.getLocalSchedulerSettings(), env, subBatch);

            if (!bsSettings.alternativesAlreadyFound) {
                if (bsSettings.shiftAlternatives) {
                    SchedulerOperations.shiftBestAlternatives(subBatch, env, bsSettings.periodStart, bsSettings.periodEnd);
                }
                for(UserJob j : subBatch){
                    if(j.alternatives.size() <= j.bestAlternative){
                        int a = 1;
                    }
                }
                applyAlternativesToVOE(subBatch);           //env is modified here for use at anothre iterations
            }

            //System.out.println(SchedulerOperations.getAltenativesCombinationString(subBatch));
        }
    }

    protected void rankAlternatives(ArrayList<UserJob> batch) {
        for (UserJob job : batch) {
            job.rankAlternatives();
        }
    }

    protected void applySettings(SchedulerSettings settings) {
        bsSettings = (BatchSlicerSettings) settings;
    }

    protected void sliceBatch(ArrayList<UserJob> batch) {
        switch (bsSettings.sliceAlgorithm) {
            case BatchSlicerSettings.defaultOrder:
                simpleSlice(batch);
                break;
            case BatchSlicerSettings.maxPriceOrder:
                simplePriceSlice(batch);
                break;
            case BatchSlicerSettings.maxSizeOrder:
                simpleSizeSlice(batch);
                break;
            case BatchSlicerSettings.criteriaBased:
                sliceByCriteria(batch);
                break;
            default:
                subBatches.add(batch);
        }

    }

    protected SlotProcessorSettings fillSPSettings() {
        SlotProcessorSettings sps = new SlotProcessorSettings();
        sps.algorithmType = bsSettings.spAlgorithmType;
        sps.cycleStart = bsSettings.periodStart;
        sps.cycleLength = bsSettings.periodEnd - bsSettings.periodStart;
        sps.countStats = false;
        sps.criteriaHelper = bsSettings.spCriteriaHelper;
        sps.algorithmConcept = bsSettings.spConceptType;
        sps.findAllPossibleAlternativesForJob = bsSettings.findAllPossibleAlternativesForJob;
        sps.clean = true;
        /*WHY?????*/
        //sps.check4PreviousAlternatives = true;
        //sps.alternativesMinDistance = 0.05;
        return sps;
    }

    protected void applyAlternativesToVOE(ArrayList<UserJob> subBatch) {
        UserJob job = null;
        Alternative a = null;
        for (Iterator it = subBatch.iterator(); it.hasNext();) {
            job = (UserJob) it.next();
            if (job.alternatives.size() == 0) {
                continue;
            }

            if (job.bestAlternative >= 0) {         //if we found a best alternative, by default it -1
                a = job.alternatives.get(job.bestAlternative);
                VOEHelper.addAlternativeToVOE(env, a, "Job" + job.name);
            }
        }
    }

    protected void simpleSlice(ArrayList<UserJob> batch) {
        int subBatchSize = (int) (batch.size() / bsSettings.slicesNum);
        if (subBatchSize == 0) {
            subBatches.add(batch);
        } else {
            int curRRNum = 0;
            ArrayList<UserJob> tempSubBatch = null;
            for (Iterator<UserJob> it = batch.iterator(); it.hasNext();) {
                if (curRRNum == 0) {
                    tempSubBatch = new ArrayList<UserJob>();
                    subBatches.add(tempSubBatch);
                }
                tempSubBatch.add(it.next());
                if (++curRRNum == subBatchSize) {
                    curRRNum = 0;
                }
            }
        }

    }

    protected void sliceByCriteria(ArrayList<UserJob> batch) {

        Map<String, ArrayList<UserJob>> criteriaBatches = new HashMap<String, ArrayList<UserJob>>();

        for (UserJob job : batch) {
            if (criteriaBatches.containsKey(job.resourceRequest.criteria.getDescription())) {
                criteriaBatches.get(job.resourceRequest.criteria.getDescription()).add(job);
            } else {
                ArrayList<UserJob> criteriaBatch = new ArrayList<UserJob>();
                criteriaBatch.add(job);
                subBatches.add(criteriaBatch);
                criteriaBatches.put(job.resourceRequest.criteria.getDescription(), criteriaBatch);
            }

        }
    }

    protected void simpleSizeSlice(ArrayList<UserJob> batch) {
        ArrayList<UserJob> sortedBatch = sortBatchBySize(batch);
        simpleSlice(sortedBatch);
    }

    protected void simplePriceSlice(ArrayList<UserJob> batch) {
        ArrayList<UserJob> sortedBatch = sortBatchByPrice(batch);
        simpleSlice(sortedBatch);
    }

    protected ArrayList<UserJob> sortBatchBySize(ArrayList<UserJob> batch) {
        ArrayList<UserJob> sortedBatch = new ArrayList<UserJob>();
        sortedBatch.addAll(batch);
        Collections.sort(sortedBatch, new Comparator<UserJob>() {
            public final int compare(UserJob a, UserJob b) {
                double startA = Double.MAX_VALUE;
                double startB = Double.MAX_VALUE;
                if (a.bestAlternative >= 0) {
                    startA = a.getBestAlternative().getStart();
                }
                if (b.bestAlternative >= 0) {
                    startB = b.getBestAlternative().getStart();
                }
                if (startA < startB) {
                    return -1;
                } else if (startA > startB) {
                    return 1;
                } else {
                    return 0;
                }
            }
        });

        return sortedBatch;
    }

    protected ArrayList<UserJob> sortBatchByPrice(ArrayList<UserJob> batch) {
        ArrayList<UserJob> sortedBatch = new ArrayList<UserJob>();
        sortedBatch.addAll(batch);
        Collections.sort(sortedBatch, new Comparator<UserJob>() {
            public final int compare(UserJob a, UserJob b) {

                if (a.resourceRequest.priceMax < b.resourceRequest.priceMax) {
                    return -1;
                } else if (a.resourceRequest.priceMax > b.resourceRequest.priceMax) {
                    return 1;
                } else {
                    return 0;
                }
            }
        });

        return sortedBatch;
    }

    protected double getJobSize(UserJob job) {
        double size = job.resourceRequest.priceMax
                * job.resourceRequest.resourceNeed
                * job.resourceRequest.resourceSpeed
                * job.resourceRequest.time;
        return size;
    }

    @Override
    public void flush() {
        env = null;
        subBatches = new ArrayList<ArrayList<UserJob>>();
        bsSettings = null;
    }
    
}
