/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package project.engine.scheduler.backFill;

import java.util.*;
import project.engine.data.Alternative;
import project.engine.data.Slot;
import project.engine.slot.slotProcessor.SlotProcessor;
import project.engine.slot.slotProcessor.SlotProcessorSettings;
import project.engine.data.UserJob;
import project.engine.data.VOEHelper;
import project.engine.data.VOEnvironment;
import project.engine.scheduler.Scheduler;
import project.engine.scheduler.SchedulerSettings;

/**
 *
 * @author Stanislavs
 */



public class Backfill extends Scheduler {
    
    private int recursioncounter = 0; //счётчик рекурсии - на медленных машинах позволяет убедиться, что программа ещё работает :)
    
    
    //без комментариев 
    private void MakeReservation(VOEnvironment environment, UserJob job, int altNum, int periodStart, int periodEnd)
    {
        VOEHelper.addAlternativeToVOE(environment, job.alternatives.get(altNum), job.name);
        VOEHelper.updateSlots(environment);
        VOEHelper.trimSlots(environment, periodStart, periodEnd);
        job.bestAlternative = altNum;
    }
    
    
    //длина самого короткого отрезка альтернативы. Пока не используется
    private double GetMinSlotLength(Alternative alt)
    {
        double min = alt.window.slots.get(0).getLength();
        
        
             for (Slot s: alt.window.slots) if(min > s.getLength()) min = s.getLength();        
            
            
        return min;  
    
    }
    
    
    //длина самого длинного отрезка альтернативы. Используется для метрики SECONDS
    private double GetMaxSlotLength(Alternative alt)
    {
        double max = alt.window.slots.get(0).getLength();
        
        
             for (Slot s: alt.window.slots) if(max < s.getLength()) max = s.getLength();        
            
            
        return max;  
    
    }
    
    
    //сортировка очереди для бэкфиллинга по запросам времени
    private void PriorityPolicyDuration(ArrayList<UserJob> backfillRequests, BackfillSettings settings)
    {int qsize = backfillRequests.size(), num;
     ArrayList<UserJob> temp = new ArrayList<UserJob>();
     double extremum;
    
     
            while(!backfillRequests.isEmpty())
                  {int csize = backfillRequests.size();                 
                   extremum = backfillRequests.get(0).resourceRequest.time;                    
                   num = 0;
                  
                  
                     for(int i=0; i<csize; i++)  
                         if((extremum > backfillRequests.get(i).resourceRequest.time && "DURATIONMIN".equals(settings.priorityPolicy)) 
                                 || (extremum < backfillRequests.get(i).resourceRequest.time && "DURATIONMAX".equals(settings.priorityPolicy))) 
                         {extremum = backfillRequests.get(i).resourceRequest.time; num = i;}
                        
                    
                   temp.add(backfillRequests.get(num));
                   backfillRequests.remove(num);
                    
                  }
            
             for(int i=0; i<qsize; i++) backfillRequests.add(temp.get(i));
            // for(int i=0;i<qsize;i++) System.out.println(" || " + backfillRequests.get(i).time);
    
    }   
    
    
    //рандомайзер очереди
    private void PriorityPolicyRandom(ArrayList<UserJob> backfillRequests)
    {ArrayList<UserJob> temp = new ArrayList<UserJob>();
     Random rand = new Random();
     int qsize = backfillRequests.size(), random;
             
        
            while(!backfillRequests.isEmpty())
                  {random = rand.nextInt(backfillRequests.size());
                   temp.add(backfillRequests.get(random));
                   backfillRequests.remove(random);}
            
            for(int i=0; i<qsize; i++) backfillRequests.add(temp.get(i));
           // for(int i=0;i<qsize;i++) System.out.println(" || " + backfillRequests.get(i).time);
            
    }
    
    
    //Бэкфиллинг первой подходящей альтернативы.
    private void FirstFit(VOEnvironment environment, BackfillSettings settings, SlotProcessor proc, SlotProcessorSettings prSet, ArrayList<UserJob> backfillRequests, HashSet<Integer> excludeSet, double lastScheduledJobWinStart, double lastScheduledJobWinEnd, int jobsToBackfill)
    {ArrayList<UserJob> jobs = new ArrayList<UserJob>(); //для передачи задания в slotprocessor.findalternatives 
        
     //перебираем задачи из очереди бэкфиллинга по порядку
            for(UserJob job : backfillRequests)
                 {if (jobsToBackfill == 0) break;   //достигает 0, когда кол-во забэкфилленых задач = depth                              
                   //System.out.println(" || " + rt.time);               
                  jobs.clear();
                  jobs.add(job);
                     
                  proc.findAlternatives(jobs, environment, prSet); //ищем все возможные альтернативы
                     
                  if(jobs.get(0).alternatives.size() > 0) //если нашли хоть одну
                     {
                        for(int i=0; i<jobs.get(0).alternatives.size(); i++)
                              {double altWinStart = jobs.get(0).alternatives.get(i).getStart();
                               double altWinEnd = altWinStart + GetMaxSlotLength(jobs.get(0).alternatives.get(i));
                               //проверяем на соответствие условиям бэкфиллинга. aggressive - альтернатива должна начаться раньше, чем начнётся зарезервированное ранее задание
                               //conservative - альтернатива должна закончиться раньше, чем начнётся зарезервированное ранее задание
                               if((settings.aggressive && altWinStart < lastScheduledJobWinStart) || (!settings.aggressive && altWinEnd <= lastScheduledJobWinStart)) 
                                    {excludeSet.add(job.id); //первую же подходящую альтернативу вырезаем из Environment (MakeReservation) и добавляем задание в список распределённых
                                     MakeReservation(environment, jobs.get(0), i, settings.periodStart, settings.periodEnd);                        
                                                      
                                     jobsToBackfill--; 
                                     break;
                                                      
                                    }
                                      
                                        
                              } 
                                        
                     } 
                 
                  jobs.get(0).alternatives.clear();
                                  
                 }   
     //System.out.println("Cycle end");
     
    }
    
    
    //Бэкфиллинг наиболее подходящей альтернативы
    private void BestFit(VOEnvironment environment, BackfillSettings settings, SlotProcessor proc, SlotProcessorSettings prSet, ArrayList<UserJob> backfillRequests, HashSet<Integer> excludeSet, double lastScheduledJobWinStart, double lastScheduledJobWinEnd, int jobsToBackfill)
    {ArrayList<UserJob> jobs = new ArrayList<UserJob>();
     boolean altFound = true;
     int width, jobId, altId;
     double length, totalLength, cost;
        
     
            while(altFound && (jobsToBackfill != 0)) //продолжаем, пока находятся альтернативы или пока не забэкфиллили = depth
                {altFound = false;
                 jobId = 0;
                 altId = 0;
                 width = 0;
                 length = 0;
                 totalLength = Double.MAX_VALUE;
                 
                 if("COSTMAX".equals(settings.backfillMetric)) cost = 0; //здесь не очень красиво, но лучше в голову ничего не пришло
                      else cost = 1000000000;
                     
                 //здесь перебираем все задания из очереди для бэкфиллинга, для каждого находим все альтернативы,
                 //из подходящих ищем наиболее соответствующую метрике
                    for(UserJob job : backfillRequests) 
                         {if (excludeSet.contains(job.id)) continue;                                  
                                  
                          jobs.clear();
                          jobs.add(job);
                     
                          proc.findAlternatives(jobs, environment, prSet);
                          //System.out.println("altNUM " + job.get(0).alternatives.size());
                     
                          if(jobs.get(0).alternatives.size() > 0) 
                             {
                                for(int i=0; i<jobs.get(0).alternatives.size(); i++)
                                     {double altWinStart = jobs.get(0).alternatives.get(i).getStart();
                                      double altWinEnd = altWinStart + GetMaxSlotLength(jobs.get(0).alternatives.get(i));
                                                                                            
                                      if((settings.aggressive && altWinStart < lastScheduledJobWinStart) || (!settings.aggressive && altWinEnd <= lastScheduledJobWinStart)) 
                                         {if("PROCS".equals(settings.backfillMetric) && (width < jobs.get(0).alternatives.get(i).window.slots.size())) {width = jobs.get(0).alternatives.get(i).window.slots.size(); jobId = job.id; altId = i;}
                                          if("SECONDS".equals(settings.backfillMetric) && (length < GetMaxSlotLength(jobs.get(0).alternatives.get(i)))) {length = GetMaxSlotLength(jobs.get(0).alternatives.get(i)); jobId = job.id; altId = i;}
                                          if("PROCSECONDS".equals(settings.backfillMetric) && (totalLength > jobs.get(0).alternatives.get(i).getLength())) {totalLength = jobs.get(0).alternatives.get(i).getLength(); jobId = job.id; altId = i;}
                                          if(("COSTMIN".equals(settings.backfillMetric) && (cost > jobs.get(0).alternatives.get(i).getCost())) || ("COSTMAX".equals(settings.backfillMetric) && (cost < jobs.get(0).alternatives.get(i).getCost()))) {cost = jobs.get(0).alternatives.get(i).getCost(); jobId = job.id; altId = i;}
                                          
                                          //System.out.println("jobid " + jobId + "|alt " + altId + "|cost " + cost);
                                          
                                          altFound = true;
                                          
                                          if("PROCS".equals(settings.backfillMetric)) break;
                                          
                                         }
                                      
                                        
                                     } 
                                        
                             }
                 
                          jobs.get(0).alternatives.clear(); 
                                  
                         }
                    
                   // System.out.println("Cycle End");
                    if(altFound) //если нашли, вырезаем
                        {//System.out.println("Reservation");
                         jobs.clear();

                        
                            for(UserJob job : backfillRequests) if(job.id == jobId) jobs.add(job);
                            
                    
                         proc.findAlternatives(jobs, environment, prSet);
                         MakeReservation(environment, jobs.get(0), altId, settings.periodStart, settings.periodEnd);                    
                         excludeSet.add(jobId);
                                          
                         jobsToBackfill--; 
                         
                        }  //          
                    
                }
            
            //for(int i: excludeSet) System.out.println("  "+i);
            
     
    }
    
    
    //здесь некрасивый код и не совсем логичные названия переменных. Потом почищу. Связано с тем, что я не люблю рекурсии, а они не любят меня - поэтому писался этот сегмент по принципу "лишь бы заработало"
    private void Greedy(HashSet<Integer> globalExcludeSet, VOEnvironment environment, BackfillSettings settings, SlotProcessor proc, SlotProcessorSettings prSet, ArrayList<UserJob> backfillRequests, double lastScheduledJobWinStart, double lastScheduledJobWinEnd, int jobsToBackfill)
    {ArrayList<UserJob> jobs = new ArrayList<UserJob>();
     ArrayList<ArrayList> hugeTable = new ArrayList<ArrayList>(); //здесь содержатся все возможные комбинации альтернатив
     HashSet<Integer> localExcludeSet = new HashSet<Integer>(); 
     ArrayList<AlternativeData> altCollection = new ArrayList<AlternativeData>();
     int recursionDepth = 0, width = 0, targetId = 0;
     double length = 0, totalLength = 0, cost;
     /*Честно говоря, нет ни малейшего желания расписывать этот ужас. Вкратце - рекурсивная процедура перебирает все возможные комбинации альтернатив, 
       и просматривая каждую подходящую альтернативу (положем, задания 2) она вырезает её из копии Environment, вызывает себя же и на следующей глубине рекурсии она делает тоже самое, только для всех заданий и всех альтернатив этих заданий, кроме
       задания 2. Рекурсия останавливается, когда глубина равна либо depth, либо длине очереди заданий для бэкфиллинга. В результате таблица hugeTable оказывается заполнена данными об альтернативах, одна строка таблицы - одна возможная комбинация альтернатив
       Из всех комбинаций выбирается наиболее подходящяя по метрике, и последовательно вырезается уже из основного Environment*/
    
     
        if("COSTMAX".equals(settings.backfillMetric)) cost = 0;
            else cost = 1000000000;
           
     
     HilariousRecursiveMadnessEnsues(recursionDepth, altCollection, localExcludeSet, hugeTable, environment, settings, proc, prSet, backfillRequests, lastScheduledJobWinStart, lastScheduledJobWinEnd, jobsToBackfill);
     altCollection.clear();
     //System.out.println("Number of altCollections found: "+hugeTable.size());
            for(int i=0; i<hugeTable.size(); i++)
                 {int curWidth = 0;
                  double curLength = 0, curTotalLength = 0, curCost = 0;
                  ArrayList<AlternativeData> altCol = new ArrayList<AlternativeData>();
                  
                  altCol = hugeTable.get(i);
                  //System.out.println("altColSize: "+altCol.size());
                 
                     for(AlternativeData altData: altCol) 
                          {curWidth = curWidth + altData.width;
                           curLength = curLength + altData.length;
                           curTotalLength = curTotalLength + altData.totalLength;
                           curCost = curCost + altData.cost;
                           
                          }
                
                  System.out.println("Current altCollection cost"+curCost);
                  if("PROCS".equals(settings.backfillMetric) && (width < curWidth)) {width = curWidth; targetId = i;}
                  if("SECONDS".equals(settings.backfillMetric) && (length < curLength)) {length = curLength; targetId = i;}
                  if("PROCSECONDS".equals(settings.backfillMetric) && (totalLength < curTotalLength)) {totalLength = curTotalLength; targetId = i;}                    
                  if(("COSTMIN".equals(settings.backfillMetric) && (cost > curCost)) || ("COSTMAX".equals(settings.backfillMetric) && (cost < curCost))) {cost = curCost; targetId = i;}
                 }
            
     System.out.println("AltCollection Cost: "+cost);
     if(hugeTable.size()>0) altCollection.addAll(hugeTable.get(targetId));  
          
     System.out.println("Alt collection consists of: ");      
             for(AlternativeData altData: altCollection)
                  {jobs.clear();
                  
                  
                        for(UserJob job : backfillRequests) if(job.id == altData.jobId) jobs.add(job);         
                   
                   System.out.println("Does this work?!");
                   System.out.println("Job num: "+altData.jobId+"|alt num: "+(altData.altId+1)+"|job cost: "+altData.cost);
                   proc.findAlternatives(jobs, environment, prSet);
                   MakeReservation(environment, jobs.get(0), altData.altId, settings.periodStart, settings.periodEnd);
                   globalExcludeSet.add(altData.jobId);
                 
                  }
                
                
     
    }
    
    
    //я думаю, из названия понятно, что я об этом думаю
    private void HilariousRecursiveMadnessEnsues(int recursionDepth, ArrayList<AlternativeData> altCollection, HashSet<Integer> excludeSet, ArrayList<ArrayList> hugeTable, VOEnvironment environment, BackfillSettings settings, SlotProcessor proc, SlotProcessorSettings prSet, ArrayList<UserJob> backfillRequests, double lastScheduledJobWinStart, double lastScheduledJobWinEnd, int jobsToBackfill)
    {ArrayList<UserJob> jobs = new ArrayList<UserJob>();  
     
     recursionDepth++;
     recursioncounter++;
     System.out.println("recursive madness call "+ recursioncounter); 
     
           for(UserJob job : backfillRequests)
                {if(excludeSet.contains(job.id)) continue; 
                    
                 jobs.clear();
                 jobs.add(job);
                    
                 proc.findAlternatives(jobs, environment, prSet);
                    
                 if(jobs.get(0).alternatives.size() > 0) 
                    {excludeSet.add(job.id);                         
                        
                       for(int i=0; i<jobs.get(0).alternatives.size(); i++)
                             {double altWinStart = jobs.get(0).alternatives.get(i).getStart();
                              double altWinEnd = altWinStart + GetMaxSlotLength(jobs.get(0).alternatives.get(i));
                                                                                           
                              if((settings.aggressive && altWinStart < lastScheduledJobWinStart) || (!settings.aggressive && altWinEnd <= lastScheduledJobWinStart)) 
                                   {if((recursionDepth == backfillRequests.size()) || (recursionDepth == jobsToBackfill))
                                        {AlternativeData alt = new AlternativeData();
                                         ArrayList<AlternativeData> altCol = new ArrayList<AlternativeData>();
                                    
                                         alt.jobId = job.id;
                                         alt.altId = i;
                                         
                                         if("PROCS".equals(settings.backfillMetric)) alt.width = jobs.get(0).alternatives.get(i).window.slots.size(); 
                                         if("SECONDS".equals(settings.backfillMetric)) alt.length = GetMaxSlotLength(jobs.get(0).alternatives.get(i));
                                         if("PROCSECONDS".equals(settings.backfillMetric)) alt.totalLength = jobs.get(0).alternatives.get(i).getLength();
                                         if("COSTMIN".equals(settings.backfillMetric)||"COSTMAX".equals(settings.backfillMetric)) alt.cost = jobs.get(0).alternatives.get(i).getCost();
                                         
                                         altCollection.add(alt);                               
                                         altCol.addAll(altCollection);
                                         //System.out.println("alt |Job num: "+alt.jobId +"|job cost: "+alt.length);
                                        // System.out.println("altcol |Job num: "+altCol.get(0).jobId +"|job cost: "+altCol.get(0).length);
                                         hugeTable.add(altCol);
                                         
                                         //System.out.println("altcol |Job num: "+altCol.get(altCol.size()-1).jobId +"|job cost: "+altCol.get(0).cost);
                                         altCollection.remove(alt);
                                       
                                        }
                                           else
                                               {VOEnvironment env = VOEHelper.copyEnvironment(environment);
                                                AlternativeData alt = new AlternativeData();
                                    
                                                alt.jobId = job.id;
                                                alt.altId = i;
                                                
                                                if("PROCS".equals(settings.backfillMetric)) alt.width = jobs.get(0).alternatives.get(i).window.slots.size(); 
                                                if("SECONDS".equals(settings.backfillMetric)) alt.length = GetMaxSlotLength(jobs.get(0).alternatives.get(i));
                                                if("PROCSECONDS".equals(settings.backfillMetric)) alt.totalLength = jobs.get(0).alternatives.get(i).getLength();  
                                                if("COSTMIN".equals(settings.backfillMetric)||"COSTMAX".equals(settings.backfillMetric)) alt.cost = jobs.get(0).alternatives.get(i).getCost();
                                                
                                                altCollection.add(alt);
                                                                        
                                                MakeReservation(env, jobs.get(0), i, settings.periodStart, settings.periodEnd);
                                                HilariousRecursiveMadnessEnsues(recursionDepth, altCollection, excludeSet, hugeTable, env, settings, proc, prSet, backfillRequests, lastScheduledJobWinStart, lastScheduledJobWinEnd, jobsToBackfill);
                                    
                                                altCollection.remove(alt); 
                                       
                                               }                            
                                                     
                                   }
                                     
                                       
                             } 
                       
                     excludeSet.remove(job.id);
                     
                    }
                       else 
                           {ArrayList<AlternativeData> altCol = new ArrayList<AlternativeData>();
                            altCol.addAll(altCollection);
                            hugeTable.add(altCol);
                 
                           }
                
                 jobs.get(0).alternatives.clear();
                                 
                } 
           
    }
    
    
    //единственная видимая процедура класса - управляющая. 
    public void solve(SchedulerSettings settings, VOEnvironment voenv, ArrayList<UserJob> batch)
    {
        BackfillSettings bfSettings = (BackfillSettings)settings;
        VOEnvironment env = VOEHelper.copyEnvironment(voenv);
        
        boolean altFound = true; //если хоть одно задание было зарезервировано 
        double lastScheduledJobWinStart = 0, lastScheduledJobWinEnd = 0; //время старта и время конца предыдущего зарезервированного хадания. Второе не используется
        int jobsToBackfill = 0; //счётчик бэкфиллинга. Перед бэкфиллингом ставиться равным depth
        HashSet<Integer> excludeSet = new HashSet<Integer>(); //все зарезервированные или забэкфилленные задания идут сюда  
        ArrayList<UserJob> jobs = new ArrayList<UserJob>(); //для передачи задания в slotprocessor.findalternative
        ArrayList<UserJob> backfillRequests = new ArrayList<UserJob>(); //очередь для бэкфиллинга - для передачи в соответствующие методы
         
        //настройка слотпроцессора
        SlotProcessor proc = new SlotProcessor();
        SlotProcessorSettings prSet = new SlotProcessorSettings();
        prSet.algorithmType = "MODIFIED";
        prSet.cycleLength = bfSettings.periodEnd - bfSettings.periodStart; 
        prSet.cycleStart = bfSettings.periodStart;
        prSet.algorithmConcept = "COMMON";
        prSet.check4PreviousAlternatives = false;
        prSet.clean = false; //эт что?!
        
        
        VOEHelper.updateSlots(env);
        VOEHelper.trimSlots(env, bfSettings.periodStart, bfSettings.periodEnd);
        //System.out.println("jtb "+jobsToBackfill);
        
        while(altFound) //если хоть одно задание было зарезервировано на предыдущем шаге
            {//System.out.println("go");
             if(jobsToBackfill == 0) //если счётчик бэкфиллинга равен 0, будем резервировать
                {altFound = false;
                  
                    for(UserJob job: batch) //последовательно смотрим все задания в очереди
                        {if(excludeSet.contains(job.id)) continue; //кроме тех, что уже успешно зарезервированны или забэкфилленны
                     
                         jobs.clear(); 
                         jobs.add(job);
                     
                         proc.findAlternatives(jobs, env, prSet); //ищем всё возможные альтернативы на текущей среде
                     
                         if(jobs.get(0).alternatives.size() > 0) //если нашли хоть одну
                             {
                                 for(int i=0; i<jobs.get(0).alternatives.size(); i++)//смотрим 
                                     {double altWinEnd = 0;                                     
                                      double altWinStart = jobs.get(0).alternatives.get(i).getStart(); //ставим сюда время старта текущей альтернативы (для лучшей читаемости кода)                                   
                                      if(!bfSettings.aggressive) altWinEnd = altWinStart + GetMaxSlotLength(jobs.get(0).alternatives.get(i)); //сюда - время конца. Не используется, потом уберу
                        
                                      if(altWinStart >= lastScheduledJobWinStart) //Спорное условие - надо обсудить. подразумевается, что резервация должна начаться тогда же, когда и началась предыдущая резервация, или позже
                                                     {excludeSet.add(job.id); //исключаем из рассмотрения
                                                      MakeReservation(env, jobs.get(0), i, bfSettings.periodStart, bfSettings.periodEnd); //вырезаем из среды
                                      
                                                      lastScheduledJobWinStart = altWinStart; //ноу коммент
                                                      lastScheduledJobWinEnd = altWinEnd;
                             
                                                      altFound = true;           
                                                     }
                                                        else if(i == jobs.get(0).alternatives.size()-1) excludeSet.add(job.id); //если всё просмотрели - значит, подходящих под то спорное условие альтернатив для данного задания 
                                                                                                                               //в данной среде нет и не будет                                      
                                      if(altFound) //если зарезервировали
                                          {jobsToBackfill = bfSettings.depth; //будем бэкфиллить                                           
                                           break;}
                                
                                     }                 
                         
                             }
                                else excludeSet.add(job.id); //если не нашли не одной альтернативы - значит, и дальше не найдём. Исключаем.
                          //if(altFound)System.out.println("alt found");
                          //job.get(0).alternatives.clear(); 
                          
                          if(altFound) break;
                        
                         }
            
                } //Счётчик бэкфиллинга не равен нулю - бэкфиллим
                    else 
                        { //System.out.println("try for backfill");
                            for(UserJob job: batch) if(!excludeSet.contains(job.id)) backfillRequests.add(job); //очередь заданий для бэкфиллинга
                         
                            //System.out.println("LSJWE "+lastScheduledJobWinEnd);
                            //for(ResourceRequest rt: backfillRequests) System.out.println(rt.id);
                            
                         if("FIRSTFIT".equals(bfSettings.policy)) //если FIRSTFIT - учитываем priorityPolicy
                             {if("RANDOM".equals(bfSettings.priorityPolicy)) PriorityPolicyRandom(backfillRequests);
                                  else if("DURATIONMIN".equals(bfSettings.priorityPolicy) || "DURATIONMAX".equals(bfSettings.priorityPolicy)) PriorityPolicyDuration(backfillRequests, bfSettings);                               
                             }
                         
                         //for(ResourceRequest rt: backfillRequests) System.out.println(rt.id);
                         //тут и так понятно  
                         if("FIRSTFIT".equals(bfSettings.policy)) FirstFit(env, bfSettings, proc, prSet, backfillRequests, excludeSet, lastScheduledJobWinStart, lastScheduledJobWinEnd, jobsToBackfill);
                         if("BESTFIT".equals(bfSettings.policy)) BestFit(env, bfSettings, proc, prSet, backfillRequests, excludeSet, lastScheduledJobWinStart, lastScheduledJobWinEnd, jobsToBackfill);
                         if("GREEDY".equals(bfSettings.policy)) Greedy(excludeSet, env, bfSettings, proc, prSet, backfillRequests, lastScheduledJobWinStart, lastScheduledJobWinEnd, jobsToBackfill);
                         
                         
                         //if jobsToBackfill != 0 then a straight resrvation was made at some point in the past and we are trying to backfill
                         
                         backfillRequests.clear(); //очищаем очередь на бэкфиллинг
                         jobsToBackfill=0;  //счётчик в любом случаем ставим 0, чтобы бэкифиллинг не вызывался несколько раз подряд                      
                         
                        }
             
            }
        
    }   

    @Override
    public void flush() {
        recursioncounter = 0;
    }

    
}
//



/*
 *  {                                                   
                            for(ResourceRequest rt: requests)
                                 {if (excludeSet.contains(rt.id) || jobsToBackfill == 0) continue;  //self-explanatory. the rest is pretty similar as well                                 
                                  
                                  job.clear();
                                  job.add(rt);
                     
                                  proc.findAlternatives(job, environment, prSet);
                     
                                  if(job.get(0).alternatives.size() > 0) 
                                     {
                                        for(int i=0; i<job.get(0).alternatives.size(); i++)
                                             {double altWinStart = job.get(0).alternatives.get(i).getStart();
                                              double altWinEnd = altWinStart + GetMinSlotLength(job.get(0).alternatives.get(i));                                                      
                                              
                                              if(altWinStart < lastScheduledJobWinStart && altWinEnd <= lastScheduledJobWinEnd) //if an alternative for a job starts before the last scheduled job -it is backfill. if it ends before the last scheduled job - it is conservative
                                                     {excludeSet.add(rt.id);
                                                      MakeReservation(environment, job.get(0), i, settings.cycleLength);                        
                                                      
                                                      jobsToBackfill--; //one job less to backfill
                                                      break;
                                                      
                                                     }                                          
                                        
                                             } 
                                        
                                    }
                                  
                                  job.get(0).alternatives.clear();
                                  
                                 }
                            
                           
                         jobsToBackfill=0; //iterated through all of the jobs in the queue - even if none was backfilled, there is no point in going over them again. And again. And again..  
                        }
 */