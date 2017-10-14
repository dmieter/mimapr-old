/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package project.engine.data;
import project.engine.slot.slotProcessor.criteriaHelpers.ICriteriaHelper;
import project.engine.slot.slotProcessor.userRankings.SimpleUserRanking;
import project.engine.slot.slotProcessor.userRankings.UserRanking;
import project.math.utils.MathUtils;


public class ResourceRequest 
{
    //maximum allowed price of resource
    public double priceMax;
    
    //number of resources needed
    public int resourceNeed;
    
    //minimum resource speed
    public double resourceSpeed;
    
    //time for resource reservation
    public double time;
    
    // deadline before this job must be completed
    public long deadLine;
    
    //alternative searching criteria
    public ICriteriaHelper criteria;
    
    //alternative searching criteria
    public Class initialCriteria;
    
    public ResourceRequest()
    {

    }

    //Back-compatibility
    public ResourceRequest(int cpuNum, double volume, double cash, double resSpeed)
    {
        this.resourceNeed = cpuNum;
        this.time = volume;
        this.priceMax = cash;
        this.resourceSpeed = resSpeed;
        this.deadLine = 0;
    }
    
    //Back-compatibility
    public ResourceRequest(int cpuNum, double volume, double cash, double resSpeed, long deadLine)
    {
        this.resourceNeed = cpuNum;
        this.time = volume;
        this.priceMax = cash;
        this.resourceSpeed = resSpeed;
        this.deadLine = deadLine;
    }

    
    public double getVolume()
    {
        return resourceSpeed*time;
    }
    
    public Double getMaxCost()
    {
        return resourceNeed*time*priceMax;
    }
    
    public int getMaxCostInt()
    {
        return (int)MathUtils.nextUp(getMaxCost());
    }
    
    @Override
    public ResourceRequest clone(){
        ResourceRequest rr = new ResourceRequest(this.resourceNeed, this.time, this.priceMax,
                                                    this.resourceSpeed, this.deadLine);
        rr.criteria = this.criteria;
        return rr;
    }
}
