/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package project.engine.slot.slotProcessor.userRankings;

import project.engine.data.Alternative;
import project.engine.data.UserJob;

/**
 *
 * @author emelyanov
 */
public abstract class UserRanking {
    public abstract void rankUserJobAlternatives(UserJob job);
    public abstract void rankExternalAlternative(UserJob job, Alternative a);
}
