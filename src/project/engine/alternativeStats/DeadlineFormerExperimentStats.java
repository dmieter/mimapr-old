/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package project.engine.alternativeStats;

import project.engine.data.*;
import project.engine.former.deadline.DeadlineFormerVoeUtils;

import java.util.ArrayList;
import java.util.List;

public class DeadlineFormerExperimentStats {

    public int experimentsNum;
    public StatsItem totalStats;
    public List<StatsItem> cycleStats;

    public double averageCycleNumber;	// average per experiment
    public int currentCycleNumber;	// cycle counter within current experiment

    public double averageMissedDeadlines;
    public int currentMissedDeadlines = 0;
    public double averageDeadlineMissingTime;
    public double currentDeadlineMissingTime = 0;

    public DeadlineFormerExperimentStats() {
        clearStats();
    }

    public void clearStats() {
        experimentsNum = 0;
        totalStats = new StatsItem();
        averageCycleNumber = 0;
        cycleStats = new ArrayList<StatsItem>();
        currentCycleNumber = 0;
    }

    public void processResults(VOEnvironment environment, List<UserJob> jobs, int cycleLength) {
        currentCycleNumber++;

        double avCost = 0;
        double avTime = 0;
        double avRuntime = 0;
        double avStart = 0;
        double avFinish = 0;
        double avAlternatives = 0;
        double avUtilization = getUtilization(environment, jobs, cycleLength);
        double avAltUtilization = getAltUtilization(environment, jobs, cycleLength);
        int successfulJobs = 0;
        int returns = 0;

        for (UserJob job : jobs) {
            if (!checkJobForSuccess(job)) {
                returns++;
                continue;
            }
            successfulJobs++;
            Alternative a = job.getBestAlternative();
            avCost += a.getCost();
            avTime += a.getLength();
            avRuntime += a.getRuntime();
            avStart += a.getStart();
            avFinish += a.getStart() + a.getRuntime();
            avAlternatives += job.alternatives.size();

            double deadlineMissingTime = a.getStart() + a.getRuntime() - job.resourceRequest.deadLine;
            if (deadlineMissingTime > 0) {
                currentMissedDeadlines++;
                currentDeadlineMissingTime += deadlineMissingTime;
            }
        }
        if (successfulJobs > 0) {
            avCost /= successfulJobs;
            avTime /= successfulJobs;
            avRuntime /= successfulJobs;
            avStart /= successfulJobs;
            avFinish /= successfulJobs;
            avAlternatives /= successfulJobs;
        }

        processItem(totalStats, avTime, avCost, avRuntime, avStart, avFinish, avAlternatives, jobs.size(), avUtilization, avAltUtilization, returns);

        if (cycleStats.size() < currentCycleNumber) {
            cycleStats.add(new StatsItem());
        }

        processItem(cycleStats.get(currentCycleNumber - 1), avTime, avCost, avRuntime, avStart, avFinish,
                avAlternatives, jobs.size(), avUtilization, avAltUtilization, returns);

    }

    private void processItem(StatsItem item, double avTime, double avCost, double avRuntime, double avStart,
            double avFinish, double avAlternatives, int jobsNum, double avUtilization, double avAltUtilization, int returns) {
        item.averageJobTime = (item.averageJobTime * item.cycleNumber + avTime) / (item.cycleNumber + 1);
        item.averageJobCost = (item.averageJobCost * item.cycleNumber + avCost) / (item.cycleNumber + 1);
        item.averageJobRunTime = (item.averageJobRunTime * item.cycleNumber + avRuntime) / (item.cycleNumber + 1);
        item.averageJobStartTime = (item.averageJobStartTime * item.cycleNumber + avStart) / (item.cycleNumber + 1);
        item.averageJobFinishTime = (item.averageJobFinishTime * item.cycleNumber + avFinish) / (item.cycleNumber + 1);
        item.averageJobAlternatives = (item.averageJobAlternatives * item.cycleNumber + avAlternatives) / (item.cycleNumber + 1);
        item.avJobsNum = (item.avJobsNum * item.cycleNumber + jobsNum) / (item.cycleNumber + 1);
        item.utilization = (item.utilization * item.cycleNumber + avUtilization) / (item.cycleNumber + 1);
        item.altUtilization = (item.altUtilization * item.cycleNumber + avAltUtilization) / (item.cycleNumber + 1);
        item.returnNumber = (item.returnNumber * item.cycleNumber + returns) / (item.cycleNumber + 1);
        item.cycleNumber++;
    }

    public void finishExperiment() {
        averageCycleNumber = (averageCycleNumber * experimentsNum + currentCycleNumber) / (experimentsNum + 1);
        currentCycleNumber = 0;

        averageMissedDeadlines = (averageMissedDeadlines * experimentsNum + currentMissedDeadlines)
                / (experimentsNum + 1);
        double missingTime = currentMissedDeadlines == 0 ? 0 : currentDeadlineMissingTime / currentMissedDeadlines;
        averageDeadlineMissingTime = (averageDeadlineMissingTime * experimentsNum + missingTime)
                / (experimentsNum + 1);
        currentDeadlineMissingTime = 0;
        currentMissedDeadlines = 0;

        experimentsNum++;
    }

    public boolean checkJobForSuccess(UserJob job) {
        if ((job.bestAlternative < 0) && (job.alternatives.size() == 1)) {
            job.bestAlternative = 0;
        }
        return !((job.bestAlternative < 0) || (job.alternatives.size() < 1));
    }

    public double getUtilization(VOEnvironment environment, List<UserJob> jobs, int cycleLength) {
        double sLength = DeadlineFormerVoeUtils.getSumSlotLength(environment, cycleLength);
        for (UserJob job : jobs) {
            if (checkJobForSuccess(job)) {
                sLength -= job.getBestAlternative().getLength();
            }
        }
        return 1 - sLength / environment.resourceLines.size() / cycleLength;
    }

    public double getAltUtilization(VOEnvironment environment, List<UserJob> jobs, int cycleLength) {
        double sLength = DeadlineFormerVoeUtils.getSumSlotLength(environment, cycleLength);
        for (UserJob job : jobs) {
            if (checkJobForSuccess(job)) {
                for (Alternative a : job.alternatives) {
                    sLength -= a.getLength();
                }
            }
        }
        return 1 - sLength / environment.resourceLines.size() / cycleLength;
    }

    public String getData(boolean showCycleStats, boolean showCriteriaStats) {
        String data = "Number Of Experiments:\t" + this.experimentsNum + "\n"
                + "Cycles per exp:\t" + this.averageCycleNumber + "\n"
                + getItemInfo(totalStats)
                + "Missed deadlines per exp:\t" + averageMissedDeadlines + "\n"
                + "Deadline missing time per exp:\t" + averageDeadlineMissingTime + "\n";
        if (showCycleStats) {
            for (int i = 0; i < cycleStats.size(); i++) {
                data += String.format("=== CYCLE %d ===\n", i + 1) + getItemInfo(cycleStats.get(i));
            }
        }
        return data;
    }

    private String getItemInfo(StatsItem item) {
        return "Jobs number\t" + item.avJobsNum + "\n"
                + "AlternativesPerJob:\t" + item.averageJobAlternatives + "\n"
                + "JobStart:\t" + item.averageJobStartTime + "\n"
                + "JobRunTime:\t" + item.averageJobRunTime + "\n"
                + "JobFinishTime:\t" + item.averageJobFinishTime + "\n"
                + "JobSumTime:\t" + item.averageJobTime + "\n"
                + "JobSumCost:\t" + item.averageJobCost + "\n"
                + "Utilization:\t" + item.utilization + "\n"
                + "Alternatives utilization:\t" + item.altUtilization + "\n"
                + "Returns:\t" + item.returnNumber + "\n"
                + "Cycles processed:\t" + item.cycleNumber + "\n";
    }

    public String getData() {
        return getData(false, false);
    }

    public class StatsItem {

        public double avJobsNum;
        public double averageJobTime;
        public double averageJobRunTime;
        public double averageJobStartTime;
        public double averageJobFinishTime;
        public double averageJobCost;
        public double averageJobAlternatives;
        public double utilization;
        public double altUtilization;
        public double returnNumber;
        public int cycleNumber;	// number of cycles processed by the statsItem
    }
}
