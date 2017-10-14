
package project.engine.data;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import project.engine.slot.slotProcessor.userRankings.UserRanking;
import project.math.utils.MathUtils;

/**
 *
 * @author Emelyanov
 */
public class UserJob {
    
    //job id
    public int id;
    
    // job name
    public String name;
    
    //resource request
    public ResourceRequest resourceRequest;
    
    //time when request was issued
    public Date timestamp;
    
    //user that has issued a request
    public VOUser user;
    
    // Coefficient reflecting real job execution time
    public double userTimeCorrectiveCoef = 1;
    
    //alternatives ranking algorithm
    public UserRanking rankingAlgorithm;
    
    public int bestAlternative = -1;

    public ArrayList<Alternative> alternatives = new ArrayList<Alternative>(); // alternatives 4 this job
    
    
    public UserJob(int id, String name, ResourceRequest resourceRequest, VOUser user, Date timestamp){
        this.id = id;
        this.name = name;
        this.resourceRequest = resourceRequest;
        this.user = user;
        this.timestamp = timestamp;
        
        alternatives = new ArrayList<Alternative>();
        this.bestAlternative = -1;
    }
    
    
    public double getBestCost()
    {
        if (bestAlternative != -1)
            return getAlternative(bestAlternative).getCost();
        return -1;
    }

    public double getBestTime()
    {
        if (bestAlternative != -1)
            return getAlternative(bestAlternative).getLength();
        return -1;
    }
    
    public int getIntegerCompletionTime()
    {
        if (bestAlternative != -1){
            
            Double completiontime = MathUtils.nextUp(getCompletionTime());
            return completiontime.intValue();
        }
        else
            return -1;
    }
    
    public int getRealIntegerCompletionTime(){
        if (bestAlternative != -1){
            
            return getIntegerStartTime()+getRealIntegerRuntime();
        }
        else
            return -1;
    }
    
    public int getIntegerStartTime()
    {
        if (bestAlternative != -1){
            
            Double startTime = MathUtils.nextUp(getStartTime());
            return startTime.intValue();
        }
        else
            return -1;
    }
    
    public double getCompletionTime()
    {
        if (bestAlternative != -1){
            Alternative a = getAlternative(bestAlternative);
            return a.getStart() + a.getRuntime();
        }
        else
            return -1;
    }


    public double getStartTime()
    {
        if (bestAlternative != -1){
            Alternative a = getAlternative(bestAlternative);
            return a.getStart();
        }
        else
            return -1;
    }
    
    public int getRealIntegerRuntime()
    {
        if (bestAlternative != -1){
            Alternative a = getAlternative(bestAlternative);
            Double runtime =  MathUtils.nextUp(a.getRuntime()*a.userTimeCorrectiveCoef);
            return runtime.intValue();
        }
        else
            return -1;
    }
    
    public Alternative getAlternative(int num)
    {
        return this.alternatives.get(num);
    }

    public Alternative getBestAlternative()
    {
        return this.getAlternative(bestAlternative);
    }
    
    public void addAlternative(Alternative a){
        addAlternative(a, true); // by default all added alternatives should have shrink coefficient according to user job request
    }
    
    public void addAlternative(Alternative a, boolean shrink)
    {
        a.num = alternatives.size();
        if(shrink){
            a.userTimeCorrectiveCoef = userTimeCorrectiveCoef;  // Setting alternative shrink coefficient
        }
        alternatives.add(a);
    }
    
    public void clearAlternatives(){
        alternatives.clear();
        bestAlternative = -1;
    }
    
    public double getVolume()
    {
        return resourceRequest.resourceSpeed*resourceRequest.time;
    }
    
    public void rankAlternatives(){
        if(rankingAlgorithm != null){
            rankingAlgorithm.rankUserJobAlternatives(this);
        }else{
            for(Alternative a : alternatives){
                a.setUserRating(0);  /* All alternatives have same rating */
            }
        }
    }
    
    @Override
    public UserJob clone(){
        ResourceRequest rr = this.resourceRequest.clone();
        UserJob job = new UserJob(this.id, this.name, rr, this.user, this.timestamp);
        job.userTimeCorrectiveCoef = this.userTimeCorrectiveCoef;
        job.bestAlternative = this.bestAlternative;
        job.rankingAlgorithm = this.rankingAlgorithm;
        
        
        job.alternatives = new ArrayList<Alternative>();
        for(Iterator<Alternative> it = this.alternatives.iterator(); it.hasNext();){
            Alternative a = it.next().clone();
            job.alternatives.add(a);
        }
        return job;
    }
    
    
    
    public String debugInfo()
      {
        StringBuilder sb = new StringBuilder();
        DecimalFormat df = new DecimalFormat("#.####");
        sb.append("### RESOURCE REQUEST ###"+ "\n");
        sb.append("ID: "+id + "\n");
        sb.append("Name: "+ name + "\n");
        sb.append("Time: "+df.format(resourceRequest.time) + "\n");
        sb.append("Volume: "+df.format(this.getVolume())+"\n");
        sb.append("Resource need:  "+resourceRequest.resourceNeed+"\n");
        sb.append("Resource speed: " + df.format(resourceRequest.resourceSpeed) + "\n");
        sb.append("Price maximum:  " + df.format(resourceRequest.priceMax) + "\n");
        if (alternatives.size()<1)
        {
           sb.append("\nNo Alternatives\n");
        }
        else
        {
              sb.append("\nAlternatives ("+alternatives.size()+" total):\n");
              for (Alternative alternative: alternatives)
              {
                  sb.append("\n");
                  sb.append("Alternative:" + String.valueOf(alternative.num)+"\n");
                  if (alternative.window != null)
                  {
                      sb.append("Window length: "+alternative.window.length+"\n");
                      sb.append("Window start:  "+alternative.window.start+"\n");
                      sb.append("Total volume cost: "+alternative.window.getTotalVolumeCost()+"\n");
                      for (Slot slot: alternative.window.slots)
                      {
                        sb.append(slot.resourceLine.name+";"+slot.getLength()+"//");
                      }
                      sb.append("\n");
                  }
               }
              if (bestAlternative != -1 && bestAlternative < alternatives.size())
              {
                  sb.append("\nBest="+String.valueOf(alternatives.get(bestAlternative).num));
                  sb.append("\n");
                  sb.append("Window length: "+alternatives.get(bestAlternative).window.length+"\n");
                      sb.append("Window start:  "+alternatives.get(bestAlternative).window.start+"\n");
                      sb.append("Total volume cost: "+alternatives.get(bestAlternative).window.getTotalVolumeCost()+"\n");
                      for (Slot slot: alternatives.get(bestAlternative).window.slots)
                      {
                        sb.append(slot.resourceLine.name+";"+slot.getLength()+"//");
                      }
                      sb.append("\n");
              }
              else
              {
                  sb.append("Error: Bad alternative: "+ String.valueOf(bestAlternative));
              }
        }
        return sb.toString();
    }
}
