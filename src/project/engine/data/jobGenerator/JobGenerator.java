package project.engine.data.jobGenerator;

import java.util.ArrayList;
import java.util.Date;
import project.engine.data.ResourceRequest;
import project.engine.data.UserJob;
import project.engine.slot.slotProcessor.criteriaHelpers.MinFinishTimeCriteria;
import project.engine.slot.slotProcessor.criteriaHelpers.MinRunTimeCriteria;
import project.engine.slot.slotProcessor.criteriaHelpers.MinSumCostCriteria;
import project.engine.slot.slotProcessor.criteriaHelpers.MinSumTimeCriteria;
import project.math.utils.MathUtils;

/**
 *
 * @author emelyanov
 */
public class JobGenerator {
    
    public JobGenerator(){
    
    }
    
    public ArrayList<UserJob> generate(JobGeneratorSettings settings){
        
        RequestGenerator rg = new RequestGenerator();
        ArrayList<ResourceRequest> requests = rg.generate(settings);
        
        ArrayList<UserJob> jobs = generateJobsByRequests(requests, settings);
        fillTimeCorrectiveCoefs(jobs, settings);
        
        return jobs;
    }

    private ArrayList<UserJob> generateJobsByRequests(ArrayList<ResourceRequest> requests, JobGeneratorSettings settings) {
        ArrayList<UserJob> jobs = new ArrayList<UserJob>();
        int i = 0;
        for(ResourceRequest rr : requests){
            String jobName = "J"+i;
            UserJob job = new UserJob(i, jobName, rr, null, new Date());
            
            jobs.add(job);
            i++;
        }
        
        return jobs;
    }
    
    
    protected void fillTimeCorrectiveCoefs(ArrayList<UserJob> jobs, JobGeneratorSettings settings) {
        if(jobs != null && settings.timeCorrectiveCoefGen != null){
            for(UserJob job : jobs){
                job.userTimeCorrectiveCoef = settings.timeCorrectiveCoefGen.getRandom();
            }
        }
    }
    
    public void setRandomBatchCriterias(ArrayList<UserJob> jobs){

        for(UserJob job : jobs)
            setRandomRequestCriteria(job);
    }

    private void setRandomRequestCriteria(UserJob job){
        int c = MathUtils.getUniform(1, 1);
        //int c = 1;
        try{
            Thread.sleep(5);           //to make random more random
        }catch(Exception e){

        }
//        
//        if(c == 2){
//            c = MathUtils.getUniform(2, 3);
//            try{
//                Thread.sleep(7);           //to make random more random
//            }catch(Exception e){
//
//            }
//        }
        
        switch(c){
            //case 0: job.resourceRequest.criteria = null; break;   //MinStartTime
            case 1: job.resourceRequest.criteria = new MinSumCostCriteria(); break;
            case 2: job.resourceRequest.criteria = new MinRunTimeCriteria(); break;
            //case 3: job.resourceRequest.criteria = new MinFinishTimeCriteria(); break;
            //case 4: job.resourceRequest.criteria = new MinSumTimeCriteria(); break;
            default: break;
        }
    }
    
}
