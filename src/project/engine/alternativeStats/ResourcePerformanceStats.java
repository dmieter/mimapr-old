/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package project.engine.alternativeStats;

import java.util.HashMap;
import java.util.Map;
import project.engine.data.ResourceLine;
import project.engine.data.VOEnvironment;

/**
 *
 * @author emelyanov
 */
public class ResourcePerformanceStats {
    public int expNum = 0;
    public double slotsNum = 0;
    public HashMap<Integer, Integer> performanceInstances = new HashMap<Integer, Integer>();
    public HashMap<Integer, Double> performancePrice = new HashMap<Integer, Double>();

    public void processEnvironment(VOEnvironment env){
        expNum++;
        int slots = 0;
        for(ResourceLine rl: env.resourceLines){
            slots+=rl.getSlots().size();
            
            int perf = (int)rl.resourceType.getSpeed();

            if(performanceInstances.containsKey(perf)){
                int num = performanceInstances.get(perf);
                performanceInstances.put(perf, ++num);
            }else{
                performanceInstances.put(perf, 1);
            }

            if(performancePrice.containsKey(perf)){
                double price = performancePrice.get(perf);
                double newPrice = (price*(expNum-1)+ rl.price)/expNum;
                performancePrice.put(perf, newPrice);
            }else{
                performancePrice.put(perf, rl.price);
            }
        }
        
        slotsNum = (slotsNum*(expNum-1)+slots)/expNum;
    }

    public String getData(){
        String data = "Performance Instances\n";
        for (Map.Entry<Integer,Integer> entry : performanceInstances.entrySet()) {
            data+= entry.getKey()+" -> "+entry.getValue()+"\n";
        }

        data += "Performance Price\n";
        for (Map.Entry<Integer,Double> entry : performancePrice.entrySet()) {
            data+= entry.getKey()+" -> "+entry.getValue()+"\n";
        }
        
        data += "Average Slots Number: " + slotsNum + "\n";

        return data;
    }
}
