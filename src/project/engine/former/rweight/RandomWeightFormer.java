package project.engine.former.rweight;

import project.engine.data.UserJob;
import project.engine.data.VOEnvironment;
import project.engine.former.Former;
import project.engine.former.FormerSettings;
import project.engine.former.deadline.DeadlineFormerVoeUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Petrukha on 15.05.2016.
 */
public class RandomWeightFormer extends Former {
    public List<UserJob> form(List<UserJob> jobFlow, VOEnvironment environment, FormerSettings inputSettings) {
        RandomWeightFormerSettings settings = (RandomWeightFormerSettings) inputSettings;
        List<UserJob> result = new ArrayList<>();

        double weightRemainder =
                settings.limitCoefficient *
                        DeadlineFormerVoeUtils.getEnvironmentSpecificSlotLength(environment,
                                settings.periodStart, settings.cycleLength);

        for (UserJob job : jobFlow) {
            double weight = getJobWeight(job);
            if (weightRemainder >= weight) {
                result.add(job);
                weightRemainder -= weight;
            }
        }
        return result;
    }

    private double getJobWeight(UserJob job) {
        return job.resourceRequest.time * job.resourceRequest.resourceSpeed * job.resourceRequest.resourceNeed;
    }
}
