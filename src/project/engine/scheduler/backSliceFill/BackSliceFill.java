/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package project.engine.scheduler.backSliceFill;

import java.util.ArrayList;
import project.engine.scheduler.backFill.Backfill;
import project.engine.scheduler.batchSlicer.BatchSlicer;
import project.engine.data.UserJob;
import project.engine.data.VOEHelper;
import project.engine.data.VOEnvironment;
import project.engine.scheduler.Scheduler;
import project.engine.scheduler.SchedulerSettings;

/**
 *
 * @author emelyanov
 */
public class BackSliceFill extends Scheduler {

    public ArrayList<UserJob> jobsForBS;
    public ArrayList<UserJob> jobsForBF;
    
    public void solve(SchedulerSettings settings, VOEnvironment env, ArrayList<UserJob> batch){
        BackSliceFillSettings bsfSettings = (BackSliceFillSettings)settings;
        int bsJobsNum = (int)(bsfSettings.slicerQuotient*batch.size());
        jobsForBS = new ArrayList<UserJob>();
        jobsForBF = new ArrayList<UserJob>();

        for(int i=0;i<batch.size();i++){
            UserJob job = batch.get(i);
            if(i <= bsJobsNum){
                jobsForBS.add(job);
            }
            else{
                jobsForBF.add(job);
            }
        }

        BatchSlicer bs = new BatchSlicer();
        VOEnvironment env1 = VOEHelper.copyEnvironment(env);
        bs.solve(bsfSettings.bss, env1, jobsForBS);

        Backfill bf = new Backfill();
        VOEnvironment env2 = VOEHelper.copyEnvironment(env);
        VOEHelper.applyBestAlternativesToVOE(batch, env2);
        bf.solve(bsfSettings.bfs, env2, jobsForBF);
    }

    @Override
    public void flush() {
        
    }
}
