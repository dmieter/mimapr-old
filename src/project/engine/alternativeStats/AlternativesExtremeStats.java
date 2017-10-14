/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package project.engine.alternativeStats;

import java.util.ArrayList;
import project.engine.data.Alternative;

/**
 *
 * @author Magica
 */
public class AlternativesExtremeStats {
    public double minStartTime = 0;
    public double minFinishTime = 0;
    public double minSumCost = 0;
    public double minRunTime = 0;
    public double minSumTime = 0;
    public int experimentsNum = 0;
    public double averageTime = 0;
    public double avAlternatives = 0;
    public long altSum = 0;
    public double avCpuNum = 0;

    public void processAlternatives(ArrayList<Alternative> alternatives, long t){
        double curMinStartTime = Double.MAX_VALUE;
        double curMinFinishTime = Double.MAX_VALUE;
        double curMinSumCost = Double.MAX_VALUE;
        double curMinRunTime = Double.MAX_VALUE;
        double curMinSumTime = Double.MAX_VALUE;
        double curCpuNum = Double.MAX_VALUE;

        for(Alternative a : alternatives){
            double cost = a.getCost();
            double runtime = a.getRuntime();
            double time = a.getLength();
            double start = a.getStart();
            double finish = a.getStart()+a.getRuntime();
            curCpuNum = a.window.slotsNeed;

            if(curMinFinishTime >= finish)
                curMinFinishTime = finish;

            if(curMinRunTime >= runtime)
                curMinRunTime = runtime;

            if(curMinSumTime >= time)
                curMinSumTime = time;

            if(curMinStartTime >= start)
                curMinStartTime = start;

            if(curMinSumCost >= cost)
                curMinSumCost = cost;
        }

        minStartTime = (minStartTime*experimentsNum + curMinStartTime)/(experimentsNum+1);
        minFinishTime = (minFinishTime*experimentsNum + curMinFinishTime)/(experimentsNum+1);
        minRunTime = (minRunTime*experimentsNum + curMinRunTime)/(experimentsNum+1);
        minSumTime = (minSumTime*experimentsNum + curMinSumTime)/(experimentsNum+1);
        minSumCost = (minSumCost*experimentsNum + curMinSumCost)/(experimentsNum+1);
        avCpuNum = (avCpuNum*experimentsNum + curCpuNum)/(experimentsNum+1);

        averageTime = (averageTime*experimentsNum + t)/(experimentsNum + 1);
        avAlternatives = (avAlternatives*experimentsNum + alternatives.size())/(experimentsNum+1);
        altSum += alternatives.size();

        experimentsNum++;
    }

    public void processAlternatives(ArrayList<Alternative> alternatives){
        processAlternatives(alternatives, 0);
    }

    public String getData(){
        String data = "Number Of Experiments: "+this.experimentsNum+"\n"
                +"CPUNum: "+this.avCpuNum+"\n"
                +"AlternativesNum: "+this.avAlternatives+"\n"
                +"AlternativesSum: "+this.altSum+"\n"
                +"MinStart: "+this.minStartTime+"\n"
                +"MinRunTime: "+this.minRunTime+"\n"
                +"MinFinishTime: "+this.minFinishTime+"\n"
                +"MinSumTime: "+this.minSumTime+"\n"
                +"MinSumCost: "+this.minSumCost+"\n"
                +"Working Time: "+this.averageTime/1000000d+"\n";
          
        return data;
    }
}
