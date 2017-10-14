/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package project.engine.alternativeStats;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import project.engine.data.ResourceRequest;
import project.engine.data.Alternative;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import project.engine.data.ResourceLine;
import project.engine.data.Slot;
import project.engine.data.UserJob;
import project.engine.data.VOEnvironment;

/**
 *
 * @author Magica
 */
public class AlternativeStats {

    public double experimentsCount;
    public double alternativesAverage;

    // Price stat variables
    public double cMean;         // Мат ожидание стоимости
    public double cStdDev;       // Price Standard deviation
    public double cMin;          // Min price value
    public double cMax;          // Max price value
    public double cStdDevRelative;  // = (cStdDev*100/cMean) Standard deviation relative to Mean

    // Time stat variables
    public double tMean;         // Мат ожидание времени
    public double tStdDev;       // Time Standard Deviation
    public double tMin;          // Min time value
    public double tMax;          // Max time value
    public double tStdDevRelative;  // = (tStdDev*100/tMean) Standard deviation relative to Mean

    public AlternativeStats(){
        cMean = 0;
        cMax = 0;
        cMin = 0;
        cStdDev = 0;
        cStdDevRelative = 0;

        tMean = 0;
        tMax = 0;
        tMin = 0;
        tStdDev = 0;
        tStdDevRelative = 0;

        alternativesAverage = 0;
        experimentsCount = 0;
    }

    public void processResults(ArrayList<UserJob> res){
        int requestsProcessed = 0;
        double altsNum = 0;
        double expCMean=0, expCDev=0, expCMin=0, expCMax=0, expCDevRel=0;      //scope of one experiment
        double expTMean=0, expTDev=0, expTMin=0, expTMax=0, expTDevRel=0;

        for(UserJob job : res){
            double reqCMean=0, reqCDev=0, reqCMin, reqCMax;      //scope of one request
            double reqTMean=0, reqTDev=0, reqTMin, reqTMax;
            altsNum = job.alternatives.size();

            if(job.alternatives.size()>0){

                requestsProcessed++;                             //Another request with alternatives found
                reqCMin = job.alternatives.get(0).getCost();      //init
                reqCMax = job.alternatives.get(0).getCost();
                reqTMin = job.alternatives.get(0).getLength();
                reqTMax = job.alternatives.get(0).getLength();

                for(Alternative a : job.alternatives){       //First Cycle
                    reqCMean+=a.getCost();                  //Mean calculaion
                    reqTMean+=a.getLength();                //Mean calculation
                    if(a.getCost()>reqCMax)                 //CMax
                        reqCMax = a.getCost();
                    if(a.getCost()<reqCMin)                 //CMin
                        reqCMin = a.getCost();
                    if(a.getLength()>reqTMax)               //TMax
                        reqTMax = a.getLength();
                    if(a.getLength()<reqTMin)               //TMin
                        reqTMin = a.getLength();
                }
                reqCMean/=job.alternatives.size();           //Means for this request's alternatives
                reqTMean/=job.alternatives.size();

                for(Alternative a : job.alternatives){       //Second cycle for deviations
                    reqCDev+=(reqCMean-a.getCost())*(reqCMean-a.getCost());
                    reqTDev+=(reqTMean-a.getLength())*(reqTMean-a.getLength());
                }
                reqCDev = Math.sqrt(reqCDev/job.alternatives.size());    //Deviations for this rr request
                reqTDev = Math.sqrt(reqTDev/job.alternatives.size());

                expCMean+=reqCMean;            // Мат ожидание суммы равно сумме мат ожиданий
                expCMax+=reqCMax;              // Минимум суммы равен сумме минимумов
                expCMin+=reqCMin;              // Максимум суммы равен сумме максимумов
                expCDev+=reqCDev;              // Дисперсия суммы равна сумме дисперсий без учета ковариации случайных величин

                expTMean+=reqTMean;            // Мат ожидание суммы равно сумме мат ожиданий
                expTMax+=reqTMax;              // Минимум суммы равен сумме минимумов
                expTMin+=reqTMin;              // Максимум суммы равен сумме максимумов
                expTDev+=reqTDev;              // Дисперсия суммы равна сумме дисперсий без учета ковариации случайных величин

            }
        }
        if(requestsProcessed>0){               //if we had at least one request with alternatives and hence - stats

            expCDevRel = expCDev*100/expCMean;
            expTDevRel = expTDev*100/expTMean;

            cMean = (cMean*experimentsCount+expCMean)/(experimentsCount+1); //new average value
            cMax = (cMax*experimentsCount+expCMax)/(experimentsCount+1); //new average value
            cMin = (cMin*experimentsCount+expCMin)/(experimentsCount+1); //new average value
            cStdDev = (cStdDev*experimentsCount+expCDev)/(experimentsCount+1); //new average value
            cStdDevRelative = (cStdDevRelative*experimentsCount+expCDevRel)/(experimentsCount+1); //new average value

            tMean = (tMean*experimentsCount+expTMean)/(experimentsCount+1); //new average value
            tMax = (tMax*experimentsCount+expTMax)/(experimentsCount+1); //new average value
            tMin = (tMin*experimentsCount+expTMin)/(experimentsCount+1); //new average value
            tStdDev = (tStdDev*experimentsCount+expTDev)/(experimentsCount+1); //new average value
            tStdDevRelative = (tStdDevRelative*experimentsCount+expTDevRel)/(experimentsCount+1); //new average value

            alternativesAverage = (alternativesAverage*experimentsCount+altsNum)/(experimentsCount+1); //new average value
            experimentsCount++;
        }
    }
    
    public void blankStats(){
            cMean = 0;
            cMax = 0;
            cMin = 0;
            cStdDev = 0;
            cStdDevRelative = 0;

            tMean = 0;
            tMax = 0;
            tMin = 0;
            tStdDev = 0;
            tStdDevRelative = 0;

            alternativesAverage = 0;
            experimentsCount = 0;
    }
    public void addBatchToLog(ArrayList<UserJob> jobs) {
        BufferedWriter log = null;
        String newLine = "";
        String outputPath = "requestsLog.txt";
        try {
            log = new BufferedWriter(new FileWriter(outputPath, true));
            for(UserJob job: jobs){
                newLine = ""+job.alternatives.size()+" "+job.resourceRequest.priceMax+" "+
                        job.resourceRequest.resourceNeed+" "+job.resourceRequest.resourceSpeed+
                        " "+job.resourceRequest.time+"\r\n";
                log.write(newLine);
            }
        } catch (IOException ex) {
            Logger.getLogger(AlternativeStats.class.getName()).log(Level.SEVERE, null, ex);
        }
        finally{
            try {
               log.close();
            } catch (IOException ex) {
                Logger.getLogger(AlternativeStats.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void addEnvironmentToLog(VOEnvironment environment, int cStart, int cEnd){
        int cpuNumber = 0;
        int slotsNumber = 0;
        double slotSumLength = 0;
        double slotAvLength = 0;
        double slotAvPrice = 0;
        double cpuAvPerformance = 0;
        double perfCoef = 0;            // price/perf av coefficient


        cpuNumber = environment.resourceLines.size();
        for(ResourceLine rl : environment.resourceLines){
            slotsNumber += rl.slots.size();
            slotAvPrice += rl.getSlotCost(1d);      // rouge estimation... don't take into account future sales
            cpuAvPerformance += rl.resourceType.getSpeed();
            for(Slot s : rl.getSlots()){
                double end = s.end;
                double start = s.start;
                if(start<cStart)
                    start = cStart;
                if(start>cEnd)
                    start = cEnd;
                if(end<cStart)
                    end = cStart;
                if(end>cEnd)
                    end = cEnd;
                slotSumLength += end-start;
                slotsNumber++;
            }
        }
        slotAvLength = slotSumLength/slotsNumber;
        slotAvPrice /= cpuNumber;
        cpuAvPerformance /= cpuNumber;
        perfCoef = slotAvPrice/cpuAvPerformance;

        writeEnvironmentStats("envLogs.txt", cpuNumber, cpuAvPerformance, slotsNumber, slotAvLength, slotSumLength,
                            slotAvPrice, perfCoef);

    }

    private void writeEnvironmentStats(String path, int cpuNum, double cpuAvPerf, double slotsNum,
                                double slotAvL, double slotSumL, double slotAvPrice, double priceCoef){
        BufferedWriter log = null;
        String line;
        try {
            log = new BufferedWriter(new FileWriter(path, true));
            line = ""+cpuNum+" "+cpuAvPerf+" "+slotsNum+" "+
                    slotAvL+" "+slotSumL+" "+slotAvPrice+" "+
                    priceCoef+" "+"\r\n";
            log.write(line);

        } catch (IOException ex) {
            Logger.getLogger(AlternativeStats.class.getName()).log(Level.SEVERE, null, ex);
        }
        finally{
            try {
               log.close();
            } catch (IOException ex) {
                Logger.getLogger(AlternativeStats.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
