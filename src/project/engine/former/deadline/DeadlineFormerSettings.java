package project.engine.former.deadline;

import project.engine.former.FormerSettings;

/**
 * Created by Petrukha on 24.04.2016.
 */
public class DeadlineFormerSettings extends FormerSettings {
    public int deadlineIntervalLength;
    public double limitCoefficient = 0.7;
    public ValueFinderSettings valueFinderSettings = new ValueFinderSettings();

    public class ValueFinderSettings {
        public double CQ = 1;
        public double KQ = 2;
        public double CN = 0.3;
        public double KN = 1;
        public double CL = 0.3;
        public double KL = 1;
        public double CV = 0.1;
        public double KV = 6;
    }
}
