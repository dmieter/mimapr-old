/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package project.engine.scheduler.alternativeSolver.v1;

import java.util.ArrayList;
import project.engine.data.ResourceRequest;
import project.engine.data.UserJob;

/**
 *
 * @author Magica
 */
public class LimitCountData {

    public int budgetLimit = 0;  //Const budget limit
    public int timeLimit = 0;  //Const time limit
    public ArrayList<UserJob> externalJobs;

    public LimitCountData(int budgetLimit, int timeLimit){
        this.budgetLimit = budgetLimit;
        this.timeLimit = timeLimit;
    }
    public LimitCountData(){

    }
}
