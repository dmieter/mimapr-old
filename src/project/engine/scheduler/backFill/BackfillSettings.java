/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package project.engine.scheduler.backFill;

import project.engine.scheduler.SchedulerSettings;

/**
 *
 * @author Stanislav
 */
public class BackfillSettings  extends SchedulerSettings {
    //тип алгоритма
    public boolean aggressive = false;
    //политика - FIRSTFIT, BESTFIT (выбор наиболее соответствующей метрике альтернативы из всех альтернатив ОДНОГО задания), 
    //GREEDY (выбор наиболее соответствующей метрике комбинации альтернатив) !!!!! Внимание !!!!! На старых машинах может отрабатывать ОЧЕНЬ долго. На Core 2 DUO SU9400
    //(ноутбучный проц) в отдельных случаях очередь из 4 заданий бэкфилилась до 15 минут! На современных машинах при малых размерх очереди на бэкфиллинг особых задержек быть не должно.
    public String policy = "FIRSTFIT";
    //метрика - PROCS (чем больше процессоров занимает, тем лучше), SECONDS (длиннее - лучше), PROCSECONDS (площадь больше - лучше), COSTMIN, COSTMAX (это и так понятно)
    public String backfillMetric = "PROCSECONDS";
    //сортировка очереди для бэкфиллинга - NONE, DURATIONMIN/DURATIONMAX, RANDOM. Учитывается только для политики FIRSTFIT
    public String priorityPolicy = "NONE";
    //глубина бэкфиллинга
    public int depth = 10000; 
    
}
