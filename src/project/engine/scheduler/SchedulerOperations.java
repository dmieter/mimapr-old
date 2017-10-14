package project.engine.scheduler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import project.engine.data.Alternative;
import project.engine.data.DistributedTask;
import project.engine.data.ResourceLine;
import project.engine.data.Slot;
import project.engine.data.UserJob;
import project.engine.data.VOEHelper;
import project.engine.data.VOEnvironment;
import project.engine.data.Window;
import project.engine.scheduler.dynamic.event.Event;
import project.engine.scheduler.dynamic.event.EventComparator;
import project.engine.scheduler.dynamic.event.ResourceEvent;
import project.engine.slot.slotProcessor.SlotProcessor;
import project.engine.slot.slotProcessor.SlotProcessorSettings;
import project.engine.slot.slotProcessor.userRankings.UserRanking;

/**
 *
 * @author emelyanov
 */
public class SchedulerOperations {

    public static double getCostBudgetForBatch(ArrayList<UserJob> batch) {
        double cost = 0;
        for (UserJob job : batch) {
            Alternative a = job.getBestAlternative();
            cost += a.getCost();
        }
        return cost;
    }

    public static void shiftBestAlternatives(ArrayList<UserJob> batch, VOEnvironment env, double start, double end) {
        ArrayList<UserJob> sortedBatch = orderJobsByBestCharacteristicStartTime(batch);
        VOEnvironment envTool = VOEHelper.copyEnvironment(env);
        SlotProcessorSettings sps = new SlotProcessorSettings();
        sps.algorithmType = "MODIFIED";
        sps.clean = true;
        sps.cycleStart = (int) start;
        sps.cycleLength = (int) (end - start);
        sps.countStats = false;
        SlotProcessor sp = new SlotProcessor();
        for (Iterator<UserJob> it = sortedBatch.iterator(); it.hasNext();) {
            UserJob job = it.next();
            if (job.bestAlternative < 0) {
                continue;
            }
            try {
                Alternative testA = job.getAlternative(job.bestAlternative);
                Alternative bestA = job.getAlternative(job.bestAlternative);
            } catch (Exception e) {
                int a = 4;
            }
            Alternative testA = job.getAlternative(job.bestAlternative);
            Alternative bestA = job.getAlternative(job.bestAlternative);
            ArrayList<Slot> slots = VOEHelper.getSlotsFromVOE(envTool, start, end, bestA);
            UserJob jobTemp = job.clone();
            jobTemp.alternatives.clear();
            ArrayList<UserJob> tempBatch = new ArrayList<UserJob>();
            tempBatch.add(jobTemp);
            sp.findAlternatives(tempBatch, slots, sps, 1);
            try {
                Alternative shiftedA = tempBatch.get(0).getAlternative(0);
                if (shiftedA.getStart() >= bestA.getStart()) {
                    VOEHelper.addAlternativeToVOE(envTool, bestA, bestA.name);
                    continue;
                    /* The same result after shifting, goto next job */

                }
                shiftedA.num = job.bestAlternative;
                shiftedA.name = "S" + job.name + " a" + job.bestAlternative;
                shiftedA.setUserRating(bestA.getUserRating());
                job.alternatives.add(job.bestAlternative, shiftedA);
                VOEHelper.addAlternativeToVOE(envTool, shiftedA, shiftedA.name);
            } catch (Exception e) {
                System.out.println("Smth wrong with shifting!!!");
                System.out.println("Possibly job has no alternatives and at the same time >=0 bestAlternativeValue... backfilling?");
                throw new RuntimeException(e);
            }
        }
    }

    public static ArrayList<UserJob> getSubListById(ArrayList<UserJob> AllRequests, ArrayList<UserJob> sourceRequests) {
        ArrayList<UserJob> subList = new ArrayList<UserJob>();
        for (Iterator itSrc = sourceRequests.iterator(); itSrc.hasNext();) {
            UserJob srcJob = (UserJob) itSrc.next();
            for (Iterator itAll = AllRequests.iterator(); itAll.hasNext();) {
                UserJob allJob = (UserJob) itAll.next();
                if (srcJob.id == allJob.id) {
                    subList.add(allJob);
                    break;
                }
            }
        }
        return subList;
    }

    public static void nameBatchAlternatives(ArrayList<UserJob> jobBatch) {
        for (Iterator<UserJob> itJob = jobBatch.iterator(); itJob.hasNext();) {
            UserJob job = itJob.next();
            nameAlternatives(job.alternatives, job.name + ": ");
        }
    }

    static void nameAlternatives(ArrayList<Alternative> alternatives, String jobPrefix) {
        Integer count = 0;
        for (Iterator<Alternative> itA = alternatives.iterator(); itA.hasNext();) {
            Alternative a = itA.next();
            if ((a.name == null) || (a.name == "")) {
                nameAlternative(a, count, jobPrefix);
                count++;
            }

        }
    }

    static void nameAlternative(Alternative a, Integer count, String jobPrefix) {
        if ((a.name == null) || (a.name == "")) {
            a.name = jobPrefix + "a" + count;
        }
    }

    public static void roundAlternativeStats(ArrayList<UserJob> batch) {
        for (UserJob job : batch) {
            for (Alternative a : job.alternatives) {
                a.roundCost();
                a.roundLength();
            }
        }
    }

    public static ArrayList<UserJob> orderJobsByBestCharacteristicStartTime(ArrayList<UserJob> batch) {
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

    public static void orderEventList(ArrayList<Event> events) {

        Collections.sort(events, new EventComparator());
    }

    public static void clearBatchSchedulingData(ArrayList<UserJob> batch) {
        for (UserJob job : batch) {
            job.alternatives = new ArrayList<Alternative>();
            job.bestAlternative = -1;
        }
    }

    public static void completeJobEarly(UserJob job, int completionTime) {
        Alternative a = job.getBestAlternative();
        Window newW = a.window.clone();

        // cutting window slots from right side because of early release
        for (Slot s : newW.slots) {
            if (s.end > completionTime) {
                s.end = completionTime;
            }
        }
        Alternative newA = new Alternative(newW);
        nameAlternative(newA, job.alternatives.size(), "");
        newA.name += "_E";
        newA.userTimeCorrectiveCoef = 1;
        newA.setUserRating(a.getUserRating());

        // new alternative is the best
        job.addAlternative(newA);
        //newA.num = -1;
        job.bestAlternative = job.alternatives.size() - 1;
        job.name += "_E";
    }

    public static String getAltenativesCombinationString(List<UserJob> batch) {
        StringBuilder combo = new StringBuilder();
        boolean success = true;
        double sumTime = 0;
        double sumCost = 0;
        for (Iterator<UserJob> it = batch.iterator(); it.hasNext();) {
            UserJob job = it.next();
            if (job.bestAlternative == -1) {
                success = false;
            }
            sumTime += job.getBestAlternative().getLength();
            sumCost += job.getBestAlternative().getCost();
            combo.append(job.bestAlternative);
            combo.append("(" + job.getBestAlternative().getLength() + ")");
            if (it.hasNext()) {
                combo.append(" - ");
            }
        }
        combo.append(" ").append(sumTime).append(" ").append(sumCost);
        if (success) {
            return "SUCCESS " + combo.toString();
        } else {
            return "FAIL " + combo.toString();
        }
    }

    public static void clearBatchAlternatives(ArrayList<UserJob> batch) {
        for (UserJob job : batch) {
            job.alternatives.clear();
            job.bestAlternative = -1;
        }
    }

    public static void rateBatchAlternativesByBatch(List<UserJob> batchToRate, List<UserJob> baseBatch) {
        for (UserJob jobToRate : batchToRate) {
            rateJobByBatch(jobToRate, baseBatch);
        }
    }

    public static void rateJobByBatch(UserJob jobToRate, List<UserJob> baseBatch) {
        if (jobToRate == null || jobToRate.getBestAlternative() == null) {
            return;
        }

        for (UserJob baseJob : baseBatch) {
            if (jobToRate.id == baseJob.id) {
                baseJob.rankingAlgorithm.rankExternalAlternative(baseJob, jobToRate.getBestAlternative());
                break;
            }
        }

    }

    public static void stopResource(ResourceLine resource, int time, ResourceEvent re) {
        for (Iterator<DistributedTask> it = resource.tasks.iterator(); it.hasNext();) {
            DistributedTask task = it.next();
            if (task.endTime > time) {
                it.remove();
            }
        }
        resource.tasks.add(new DistributedTask(re.getDescription(), time, time + re.getEffectDuration()));
    }

    public static void changeResource(VOEnvironment environment, ResourceLine oldResource, ResourceLine newResource, int time) {

        List<DistributedTask> executionHistoryTasks = new ArrayList<DistributedTask>();

        /*1. Clean up new resource history before the current time */
        for (Iterator<DistributedTask> it = newResource.tasks.iterator(); it.hasNext();) {
            DistributedTask task = it.next();
            if (task.endTime <= time) {
                it.remove();
            } else if (task.startTime < time) {
                task.startTime = time;
                break;
                /* the task is divided, leaving only ending part */
            } else {
                break;
                /* task is performed after the time */
            }
        }

        /*2. Move all old tasks to new line (for history) */
        for (Iterator<DistributedTask> it = oldResource.tasks.iterator(); it.hasNext();) {
            DistributedTask task = it.next();
            if (task.startTime < time && task.endTime > time) {
                task.endTime = time;
            } else if (task.startTime >= time) {
                it.remove();
            }
        }

        newResource.tasks.addAll(0, oldResource.tasks);

        /*3. switch the resource lines */
        int resourcePosition = environment.resourceLines.indexOf(oldResource);
        environment.resourceLines.set(resourcePosition, newResource);
    }

}
