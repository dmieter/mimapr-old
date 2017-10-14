
package project.engine.alternativeStats;

import java.util.ArrayList;
import project.engine.data.DistributedTask;
import project.engine.data.ResourceLine;
import project.engine.data.Slot;

/**
 *
 * @author Magica
 */
public class ResourceUtilizationStats {
    
    private double intervalStart;
    private double intervalEnd;
    private double intervalLength;
    public double price;
    public double slotLength = 0;
    public double taskLength = 0;
    public double utilization;
    public double profit;
    int expNum;
    
    public void clearStats() {
        slotLength = 0;
        taskLength = 0;
    }
    
    public void processResourceLine(double start, double end, ResourceLine node){
        this.intervalStart = start;
        this.intervalEnd = end;
        this.intervalLength = end - start;
        price = node.price;
        
        double sLength = getSumSlotLength(node.slots);
        double tLength = getSumTaskLength(node.tasks);
        
        slotLength = (slotLength*expNum + sLength)/(expNum+1);
        taskLength = (taskLength*expNum + tLength)/(expNum+1);
        utilization = taskLength/intervalLength;
        profit = price*taskLength;
        expNum++;
    }
    
    double getSumSlotLength(ArrayList<Slot> slots){
        double sLength = 0;
        for(Slot s : slots){
            if(((s.start < intervalStart)&&(s.end > intervalStart))){
                sLength += (s.end - intervalStart);
            }else if((s.start >= intervalStart)&&(s.end <= intervalEnd)){
                sLength += s.end - s.start;
            }else if((s.start < intervalEnd)&&(s.end >= intervalEnd)){
                sLength += intervalEnd - s.start;
            }
        }     
        return sLength;
    }
    
    double getSumTaskLength(ArrayList<DistributedTask> tasks){
        double tLength = 0;
        for(DistributedTask t : tasks){
            if(((t.startTime < intervalStart)&&(t.endTime > intervalStart))){
                tLength += (t.endTime - intervalStart);
            }else if((t.startTime >= intervalStart)&&(t.endTime <= intervalEnd)){
                tLength += t.endTime - t.startTime;
            }else if((t.startTime < intervalEnd)&&(t.endTime >= intervalEnd)){
                tLength += intervalEnd - t.startTime;
            }
        }     
        return tLength;
    }

    public String getData(){
        String data = "Resource Utilization\n"
                    +"Experiments: "+ this.expNum+"\n"
                    +"Price: "+this.price+"\n"
                    +"Slots Length: "+this.slotLength+"\n"
                    +"Tasks Length: "+this.taskLength+"\n"
                    +"Utilization: "+this.utilization+"\n"
                    +"Profit: "+this.profit+"\n";

        return data;
    }
}
