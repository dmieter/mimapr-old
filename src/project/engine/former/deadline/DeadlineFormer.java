package project.engine.former.deadline;

import project.engine.data.UserJob;
import project.engine.data.VOEnvironment;
import project.engine.former.Former;
import project.engine.former.FormerSettings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by Petrukha on 24.04.2016.
 */
public class DeadlineFormer extends Former {

    public List<UserJob> form(List<UserJob> jobFlow, VOEnvironment environment,
            FormerSettings inputSettings) {
        DeadlineFormerSettings settings = (DeadlineFormerSettings) inputSettings;
        List<UserJob> result = new ArrayList<>();

        includeDeadlineJobs(result, jobFlow, settings);
        includeNormalJobs(result, jobFlow, environment, settings);

        return result;
    }

    private void includeDeadlineJobs(List<UserJob> jobBatch, List<UserJob> jobFlow,
            DeadlineFormerSettings settings) {
        List<UserJob> flowCopy = new ArrayList<>(jobFlow);
        
        Collections.sort(flowCopy, new Comparator<UserJob>() {
            @Override
            public int compare(UserJob a, UserJob b) {
                // -1 - less than, 1 - greater than, 0 - equal, all inversed for descending
                return a.resourceRequest.deadLine > b.resourceRequest.deadLine ? 1 : 
                        (a.resourceRequest.deadLine < b.resourceRequest.deadLine ) ? -1 : 0;
            }
        });

        for (UserJob job : flowCopy) {
            if (job.resourceRequest.deadLine > settings.periodStart + settings.deadlineIntervalLength) {
                break;
            }
            jobBatch.add(job);
        }
    }

    private void includeNormalJobs(List<UserJob> jobBatch, List<UserJob> jobFlow,
            VOEnvironment environment, DeadlineFormerSettings settings) {
        double weightRemainder
                = settings.limitCoefficient
                * DeadlineFormerVoeUtils.getEnvironmentSpecificSlotLength(environment, settings.periodStart,
                        settings.cycleLength);
        for (UserJob job : jobBatch) {
            weightRemainder -= getJobWeight(job);
        }
        if (weightRemainder <= 0) {
            return;
        }

        List<UserJob> normalJobs = new ArrayList<>();
        for (UserJob job : jobFlow) {
            if (jobBatch.contains(job)) {
                continue;
            }
            double weight = getJobWeight(job);
            if (weightRemainder >= weight) {
                normalJobs.add(job);
                weightRemainder -= weight;
            }
        }
        prioritizeJobs(normalJobs, environment, settings);

        for (UserJob job : normalJobs) {
            jobBatch.add(job);
        }
    }

    private double getJobWeight(UserJob job) {
        return job.resourceRequest.time * job.resourceRequest.resourceSpeed * job.resourceRequest.resourceNeed;
    }

    private void prioritizeJobs(List<UserJob> jobs, final VOEnvironment environment,
            final DeadlineFormerSettings settings) {
        Collections.sort(jobs, new Comparator<UserJob>() {
            @Override
            public int compare(UserJob o1, UserJob o2) {
                return (int) Math.signum(
                        ValueFinder.findSpecificValue(o1.resourceRequest, environment, settings.periodStart,
                                settings.cycleLength, settings.valueFinderSettings)
                        - ValueFinder.findSpecificValue(o2.resourceRequest, environment, settings.periodStart,
                                settings.cycleLength, settings.valueFinderSettings));
            }
        });
    }
}
