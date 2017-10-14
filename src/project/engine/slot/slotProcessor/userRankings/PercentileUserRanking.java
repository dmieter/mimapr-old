package project.engine.slot.slotProcessor.userRankings;

import project.engine.data.Alternative;
import project.engine.data.UserJob;

/**
 *
 * @author emelyanov
 */
public class PercentileUserRanking extends UserRanking {

    protected Double maxValue;
    protected Double minValue;
    protected Double interval;

    @Override
    public void rankUserJobAlternatives(UserJob job) {

        /* We need user criteria to check calculate the ratings */
        if (job.resourceRequest.criteria == null) {
            setRatings(job, 0d);
            return;
        }

        /* 1. Look for criteria value interval */
        calculateAlternativesInterval(job);

        if (interval <= 0d) {
            setRatings(job, 0d);  /* If all alternatives are same - return same rating */

            return;
        }

        /* 2. Calculating each alternative percentage value inside the interval */
        for (Alternative a : job.alternatives) {
            calcAlternativeRating(job, a);
        }

    }

    protected void calculateAlternativesInterval(UserJob job) {

        maxValue = Double.NEGATIVE_INFINITY;
        minValue = Double.POSITIVE_INFINITY;
        Double userValue;

        for (Alternative a : job.alternatives) {
            userValue = job.resourceRequest.criteria.getCriteriaValue(a.window);
            if (userValue > maxValue) {
                maxValue = userValue;
            }
            if (userValue < minValue) {
                minValue = userValue;
            }
        }

        interval = maxValue - minValue;
    }

    protected void calcAlternativeRating(UserJob job, Alternative a) {
        Double userValue = job.resourceRequest.criteria.getCriteriaValue(a.window);
        a.setUserRating(100 - 100 * (userValue - minValue) / interval);
//            System.out.println("Criteria: "+job.resourceRequest.criteria.getClass()+" Max Value: "+maxValue+" Min Value: "+minValue+
//                    " UserValue: "+ userValue +" Percentile: "+(100 - 100*(userValue-minValue)/interval));
    }

    protected void setRatings(UserJob job, Double rating) {
        for (Alternative a : job.alternatives) {
            a.setUserRating(rating);
        }
    }

    @Override
    public void rankExternalAlternative(UserJob job, Alternative extA) {
        /* We need user criteria to check calculate the ratings */
        if (job.resourceRequest.criteria == null) {
            extA.setUserRating(0d);
            return;
        }

        /* 1. Look for criteria value interval */
        calculateAlternativesInterval(job);

        if (interval <= 0d) {
            extA.setUserRating(0d);  /* If all alternatives are same - return same rating */

            return;
        }

        /* 2. Calculating percentage value inside the interval */
        calcAlternativeRating(job, extA);

    }

}
