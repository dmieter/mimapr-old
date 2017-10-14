
package project.engine.slot.slotProcessor.userRankings;

import project.engine.data.Alternative;
import project.engine.data.UserJob;

/**
 *
 * @author emelyanov
 */
    public class SimpleUserRanking extends UserRanking {

    @Override
    /* sets user rank according to alternative order */        
    public void rankUserJobAlternatives(UserJob job) {
        int num = 0;
        for(Alternative a : job.alternatives){
            a.setUserRating(num);
            num++;
        }
    }

    @Override
    public void rankExternalAlternative(UserJob job, Alternative a) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
