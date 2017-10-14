package project.engine.scheduler.alternativeSolver.v1;

import project.engine.scheduler.alternativeSolver.v1.data.ParetoTable;
import project.engine.data.*;
import project.engine.scheduler.alternativeSolver.v1.data.AltColumn;
import project.engine.scheduler.alternativeSolver.v1.data.AltTable;

import java.util.*;
import project.engine.scheduler.Scheduler;
import project.engine.scheduler.SchedulerSettings;
import project.math.utils.MathUtils;

/**
 * Created by IntelliJ IDEA.
 * User: unco
 * Date: 17.10.2009
 * Time: 16:58:35
 * To change this template use File | Settings | File Templates.
 */
public class AlternativeSolver extends Scheduler{

    public AlternativeSolver()
    {

    }

    public ArrayList<AltTable> tables = new ArrayList<AltTable>();
    public int budgetLimit = 0;
    public int timeLimit = 0;

    //Вычисление лимита по бюджету для пакета заданий
    private void countBudgetLimit(ArrayList<UserJob> jobs, AlternativeSolverSettings settings)
    {
        switch(settings.limitCalculationType){
            case 0: this.budgetLimit = countAverageBudgetLimit(jobs, settings); break;
            case 1: this.budgetLimit = settings.limitCountData.budgetLimit; break;
            default: this.budgetLimit = countExternalBudgetLimit(jobs, settings);
        }
    }

    private int countAverageBudgetLimit(ArrayList<UserJob> jobs, AlternativeSolverSettings settings){
        int bLimit = 0;
        double budgetLimit = 0;
        double quot = 1.0;
        if(jobs.size() == 1){
            int alts = jobs.get(0).alternatives.size();
            if(alts == 1){
                int exc = 1;
            }
        }
        if (settings != null)
            quot = settings.limitQuotient;
        for (UserJob job : jobs)
        {
            double partialBudget = 0;
            for (Alternative alternative : job.alternatives)
            {
                partialBudget += alternative.getCost();
                partialBudget = MathUtils.nextUp(partialBudget); 
                /*В таблицах AS все необходимые бюджеты округляются вверх,значит при подсчете
                 среднего нужно также округлять их вверх, иначе в крайних случаях могут возникать ошибки!*/
            }
            budgetLimit += (partialBudget / job.alternatives.size())* quot;
        }
        bLimit = (int)MathUtils.nextUp(budgetLimit);  // Magica... was error here sometomes, when

        return bLimit;
    }

    private int countExternalBudgetLimit(ArrayList<UserJob> jobs, AlternativeSolverSettings settings){
        int bLimit;
        double budgetLimit = 0;
        if(settings.limitCountData.externalJobs == null)
            throw new RuntimeException("AlternativeSolver: No external requests to count budget limit");

        try{            //counting budget from external batch
            ArrayList<UserJob> externalJobs = settings.limitCountData.externalJobs;

            for(Iterator<UserJob> it = jobs.iterator();it.hasNext();){      //through all job batch
                UserJob curJob = it.next();
                boolean foundPairedRequest = false;
                for(Iterator<UserJob> extIt = externalJobs.iterator(); extIt.hasNext();){   //through all externel jobs to find the match an get value from it
                    UserJob extJob = extIt.next();
                    if(extJob.id == curJob.id){
                        if(extJob.bestAlternative < 0)
                            throw new RuntimeException("AlternativeSolver: bestAlternative in external request is null, id: "+extJob.id);
                        budgetLimit+=extJob.alternatives.get(extJob.bestAlternative).getCost();
                        foundPairedRequest = true;
                        break;
                    }
                }
                if(!foundPairedRequest)
                    throw new RuntimeException("AlternativeSolver: Can't find External Request with same id: "+curJob.id);
            }
            bLimit = (int)Math.floor(budgetLimit);
            return bLimit;
        }catch(Exception e){
            return countAverageBudgetLimit(jobs, settings);
        }
    }

    //Вычисление лимита по времени для пакета заданий
    private void countTimeLimit(ArrayList<UserJob> jobs, AlternativeSolverSettings settings)
    {
        switch(settings.limitCalculationType){
            case 0: this.timeLimit = countAverageTimeLimit(jobs, settings); break;
            case 1: this.timeLimit = settings.limitCountData.timeLimit; break;
            default: this.timeLimit = countExternalTimeLimit(jobs, settings);
        }
    }

    private int countAverageTimeLimit(ArrayList<UserJob> jobs, AlternativeSolverSettings settings){
        int tLimit = 0;
        double timeLimit = 0;
        double quot = 1.0;
        if (settings != null)
            quot = settings.limitQuotient;
        for (UserJob job : jobs)
        {
            double partialTime = 0;
            for (Alternative alternative : job.alternatives)
            {
                partialTime += alternative.getLength();
            }
            timeLimit += (partialTime / job.alternatives.size()) * quot;
        }
        tLimit = (int)Math.floor(timeLimit);
        
        return tLimit;
    }

    private int countExternalTimeLimit(ArrayList<UserJob> jobs, AlternativeSolverSettings settings){
        int tLimit;
        double timeLimit = 0;
        if(settings.limitCountData.externalJobs == null)
            throw new RuntimeException("AlternativeSolver: No external requests to count budget limit");

        try{
            ArrayList<UserJob> externalJobs = settings.limitCountData.externalJobs;

            for(Iterator<UserJob> it = jobs.iterator();it.hasNext();){      //through all job batch
                UserJob curJob = it.next();
                boolean foundPairedRequest = false;
                for(Iterator<UserJob> extIt = externalJobs.iterator(); extIt.hasNext();){   //through all externel jobs to find the match an get value from it
                    UserJob extJob = extIt.next();
                    if(extJob.id == curJob.id){
                        if(extJob.bestAlternative<0)
                            throw new RuntimeException("AlternativeSolver: bestAlternative in external request is null, id: "+extJob.id);
                        timeLimit+=extJob.alternatives.get(extJob.bestAlternative).getLength();
                        foundPairedRequest = true;
                        break;
                    }
                }
                if(!foundPairedRequest)
                    throw new RuntimeException("AlternativeSolver: Can't find External Request with same id: "+curJob.id);
            }
            tLimit = (int)Math.floor(timeLimit);
            return tLimit;
        }catch(Exception e){
            return countAverageTimeLimit(jobs, settings);
        }
    }

    private void sortAlternativesByCost(ArrayList<UserJob> jobs)
    {
        for (UserJob job: jobs)
        {
            Collections.sort(job.alternatives, new Comparator<Alternative>()
           {
                public final int compare ( Alternative a, Alternative b )
                {
                    return Double.compare(a.getCost(), b.getCost());
                }
            });
        }
    }


    //Подготовка пакета заданий
    private ArrayList<UserJob> prepareRequests(AlternativeSolverSettings settings, ArrayList<UserJob> jobs)
    {
        //берем подмножество реквестов, у которых ЕСТЬ альтернативы в этом цикле
        ArrayList<UserJob> result = new ArrayList<UserJob>();
        for (UserJob job: jobs)
        {
            if (job.alternatives.size() > 0)
                result.add(job);
        }
        //вычисление производных лимитов для режима Парето
        //(т.е. если ограничение по цене, высчитываем лимит по времени)
        if (settings.limitedVar == 1 || settings.usePareto) //COST
        {
            this.countBudgetLimit(result, settings);
        }
        if (settings.limitedVar == 0 || settings.usePareto) //TIME
        {
            this.countTimeLimit(result, settings);
        }
        return result;
    }

    private double getLimitedValue(Alternative a, AlternativeSolverSettings settings)
    {
        switch (settings.limitedVar)
        {
            case 0: return a.getLength(); //TIME
            case 1: return a.getCost();   //COST
            default: return -1;
        }
    }

    private double getOptimizedValue(Alternative a, AlternativeSolverSettings settings)
    {
        switch (settings.optimizedVar)
        {
            case 0: return a.getLength(); //TIME
            case 1: return a.getCost();   //COST
            default: return -1;
        }
    }

    private double getAltValue(UserJob job, AlternativeSolverSettings settings, int num)
    {
        switch (settings.limitedVar)
        {
            case 0: return job.getAlternative(num).getLength(); //TIME
            case 1: return job.getAlternative(num).getCost();   //COST
            default: return -1;
        }
   }

    public void solve(SchedulerSettings settings, VOEnvironment voenv, ArrayList<UserJob> batch) {
        solve((AlternativeSolverSettings)settings, batch);
    }
    
    //Основная процедура
    //ex-buildTables
    public AlternativeSolverResult solve(AlternativeSolverSettings settings, ArrayList<UserJob> batch) //throws Exception
    {
            //очищаем от пустых реквестов
            ArrayList<UserJob> jobs = prepareRequests(settings, batch);
            if (jobs.isEmpty()){
                return new AlternativeSolverResult(false);
            }
            tables = new ArrayList<AltTable>();
            //Table limit value, row count
            int limit = 0;
            if (settings.limitedVar == 0) //TIME
            {
                 limit = this.timeLimit;
            }
            if (settings.limitedVar == 1) //COST
            {
                 limit = this.budgetLimit;
            }
            if(jobs.size() == 1){
                solveIfSingleJob(jobs, settings, limit);
                return null;
            }
            if (!settings.random)
            {
                UserJob job = jobs.get(jobs.size() - 1);
                AltTable table = new AltTable(job.alternatives.size(), limit + 1);
                table.altColumns = new AltColumn[job.alternatives.size()];
                // Last metatask
                int i = 0;
                for (Alternative alternative : job.alternatives)
                {
                    AltColumn column = new AltColumn(alternative.num, limit + 1);
                    for (int j = 0; j <= limit; j++)
                    {
                        if (j< getLimitedValue(alternative, settings))
                            column.value[j] = Integer.MAX_VALUE;
                        else
                            column.value[j] = getOptimizedValue(alternative, settings);
                    }
                    table.altColumns[i] = column;
                    i++;
                }
                table.fillResults(limit, job.alternatives.size(), settings.optType);
                tables.add(table);
                // Metatasks in the middle
                for (int count = jobs.size() - 2; count > 0; count--)
                {
                    job = jobs.get(count);
                    table = new AltTable(jobs.size(), limit + 1);
                    table.altColumns = new AltColumn[job.alternatives.size()];
                    i = 0;
                    for (Alternative alternative : job.alternatives)
                    {
                        AltColumn column = new AltColumn(alternative.num, limit + 1);
                        for (int j = 0; j <= limit; j++)
                        {
                            if (j < getLimitedValue(alternative, settings))
                            {
                                column.value[j] = Integer.MAX_VALUE;
                            }
                            else
                            {
                                int reserve = (int)tables.get(tables.size() - 1).criteriaValues[j - (int) (getLimitedValue(alternative, settings))];
                                if (reserve == Integer.MAX_VALUE)
                                {
                                    column.value[j] = Integer.MAX_VALUE;
                                }
                                else
                                    column.value[j] = getOptimizedValue(alternative, settings) + reserve;
                            }
                      }
                      table.altColumns[i] = column;
                      i++;
                    }
                    table.fillResults(limit, job.alternatives.size(), settings.optType);
                    tables.add(table);
                }
    // First Metatask
                job = jobs.get(0);
                table = new AltTable(jobs.size(), limit + 1);
                table.altColumns = new AltColumn[job.alternatives.size()];
                i = 0;
                for (Alternative alternative : job.alternatives)
                {
                    AltColumn column = new AltColumn(alternative.num, limit + 1);
                    int j = limit;// - 1;
                    if (j < getLimitedValue(alternative, settings))
                    {
                        column.value[j] = Integer.MAX_VALUE;
                    }
                    else
                    {
                        int reserve = (int)tables.get(tables.size() - 1).criteriaValues[j -  (int)(getLimitedValue(alternative, settings))];
                        if (reserve == Integer.MAX_VALUE)
                        {
                            column.value[j] = Integer.MAX_VALUE;
                        }
                        else
                            column.value[j] = getOptimizedValue(alternative, settings) + reserve;
                    }
                    table.altColumns[i] = column;
                    i++;
                }
                table.fillResults(limit, job.alternatives.size(), settings.optType);
                table.lastTable(limit);
                tables.add(table);
            }
            if (settings.usePareto)
            {
                return makeParetoTable(formVariants(settings, jobs));
            }
            else
                return formVariants(settings, jobs);
    }

    private AlternativeSolverResult makeParetoTable(AlternativeSolverResult result)
    {
        if (!result.valid) return result;
        ParetoTable table = new ParetoTable(result.condOptimalVariants.size());
        //Basic values
        for (int i=0; i<result.condOptimalVariants.size(); i++)
        {
            AltDistrib var = result.condOptimalVariants.get(i);
            table.VarIds[i] = var.id;
            table.CColumn[i] = result.getTotalCash(var);
            table.TColumn[i] = result.getTotalTime(var);            
        }
        //derived limit initialization
        if (result.settings.limitedVar == 0) //TIME
        {
           result.budgetLimit = table.findMaxBudget();
        }
        if (result.settings.limitedVar == 1) //COST
        {
           result.timeLimit = table.findMaxTime();
        }
        //derivative values
        for (int i=0; i<result.condOptimalVariants.size(); i++)
        {
            AltDistrib var = result.condOptimalVariants.get(i);
            table.DColumn[i] = result.getLostProfit(var);
            table.IColumn[i] = result.getIdleTime(var);
        }
        table.normalize();
        for (int i=0; i<result.condOptimalVariants.size(); i++)
        {
            table.UFColumn[i] = (table.CColumn[i]*result.settings.C_weight +
                                table.DColumn[i]*result.settings.D_weight +
                                table.TColumn[i]*result.settings.T_weight +
                                table.IColumn[i]*result.settings.I_weight)/
                    (result.settings.C_weight + result.settings.D_weight + result.settings.T_weight + result.settings.I_weight);
        }
        double min = Integer.MAX_VALUE;
        for (int i=0; i<result.condOptimalVariants.size(); i++)
        {
            if (table.UFColumn[i]<min)
            {
                min = table.UFColumn[i];
            }
        }
        for (int i=0; i<table.size; i++)
        {
            if (table.UFColumn[i]==min)
                table.optimal[i] = true;
        }
        result.paretoTable = table;
        return result;
    }

    //ПРЯМАЯ ПРОГОНКА
    private AlternativeSolverResult formVariants(final AlternativeSolverSettings settings, final ArrayList<UserJob> jobs)
    {
        class VarTreeBuilder
        {
            //Рекурсивная процедура
            public void processVariant(AltDistrib variant,
                                        double reserve, AlternativeSolverResult result, int requestNumber)
             {
                //хвост рекурсии
                if (requestNumber == jobs.size())
                {
                    result.condOptimalVariants.add(variant);
                    return;
                }
                boolean doNotFork = result.condOptimalVariants.size() > settings.varMax;
                //делаем слепок варианта

                AltDistrib preFork = null;
                if (!doNotFork)
                    preFork = variant.copy();
                UserJob job = jobs.get(requestNumber);
                ArrayList<Integer> alts = tables.get(tables.size() - 1 - requestNumber).suitableAlternatives[(int)reserve];
                //проверяем, не пусто ли множество подходящих альтернатив
                if (alts== null || alts.size() == 0)
                {
                    //запускаем прямую прогонку для следующего по списку реквеста
                    processVariant(variant, reserve, result, requestNumber + 1);
                }
                else
                {
                    for (int altNum : alts)
                    {
                        double reserve1 = reserve - getAltValue(job, settings, altNum);
                        //если текущий реквест еще не добавлен в распределение
                        if (variant.get(job.id) == -1)
                        {
                           variant.put(job.id, altNum);
                           //запускаем прямую прогонку для следующего по списку реквеста
                           processVariant(variant, reserve1, result, requestNumber + 1);
                        }
                        //иначе дерево поиска ветвится, текущее распределение клонируется
                        //флаг doNotFork задействуется, когда общее число распределений превышает
                        //параметр VarMax
                        else if (!doNotFork)
                        {
                            AltDistrib newVar = preFork.copy();
                            newVar.id = (new Random()).nextInt();
                            newVar.put(job.id, altNum);
                            //запускаем для следующего по списку реквеста
                            processVariant(newVar, reserve1, result, requestNumber + 1);
                        }
                    }
                }
              }
        }

        AlternativeSolverResult result = new AlternativeSolverResult();
        result.jobs = jobs;
        result.requestsWithAlternatives = jobs.size();
        result.valid=true;
        result.settings = settings;
        result.tables = tables;
        result.createIndexes();
        for (UserJob job: jobs)
        {
            result.maximumCombinations *= job.alternatives.size();            
        }
        int limit = -1;
        if (settings.limitedVar == 0)
        {    //TIME
            limit = this.timeLimit;
            result.timeLimit = this.timeLimit;
        }
        else if (settings.limitedVar == 1)
        {   //COST
            limit = this.budgetLimit;
            result.budgetLimit = this.budgetLimit;
        }
        result.limit = limit;
        //Random mode
        if (settings.random)
        {
            AltDistrib variant;
            Random r = new Random();
            boolean random_failure = true;
            do
            {
                variant = new AltDistrib(jobs.size(), result.requestIndexes);
                variant.id = (new Random()).nextInt();
                for (UserJob job: jobs)
                {
                   int altNum = r.nextInt(job.alternatives.size());
                   job.bestAlternative = altNum;
                   variant.put(job.id, altNum);
                }
                //проверяем вариант распределения на соответствие лимитам
                if (settings.limitedVar == 0)
                {    //TIME
                    if (result.getTotalTime(variant) <= limit)
                        random_failure = false;
                }
                else if (settings.limitedVar == 1)
                {   //COST
                    if (result.getTotalCash(variant) <= limit)
                        random_failure = false;
                }
            }
            while (random_failure);
            result.condOptimalVariants.add(variant);
        }
        // Optimal only mode
        else if (settings.optimalOnly)
        {
            AltDistrib variant = new AltDistrib(jobs.size(), result.requestIndexes);
            variant.id = (new Random()).nextInt();
            double reserve = limit;
            for (int i=0; i<jobs.size(); i++)
            {
                UserJob job = jobs.get(i);
                ArrayList<Integer> alts = tables.get(tables.size() - 1 - i).suitableAlternatives[(int)reserve];
                if (alts!= null && alts.size() > 0 )
                {
                    int altNum = (Integer)((alts.toArray())[0]);
                    variant.put(job.id, altNum);
                    reserve = reserve - getAltValue(job, settings, altNum);
                }
             }
             result.condOptimalVariants.add(variant);
        }
        else
        {
            UserJob firstJob = jobs.get(0);
            for (int k=0; k < firstJob.alternatives.size(); k++)
            {
                //добавляем очередную альтернативу первого реквеста в пакете!!!!
                //создание нового условно-оптимального варианта распределения
                AltDistrib variant = new AltDistrib(jobs.size(), result.requestIndexes);
                variant.id = (new Random()).nextInt();
                //номер альтернативы
                int num = firstJob.alternatives.get(k).num;
                variant.put(firstJob.id, num);
                double reserve = limit - getAltValue(firstJob, settings, num);
                //запускаем рекурсию
                (new VarTreeBuilder()).processVariant(variant, reserve, result, 1);
            }
        }
        return analyzeVariants(result);
    }

    private AlternativeSolverResult analyzeVariants(AlternativeSolverResult result)
    {
        if (!result.settings.random)
        {
            //Анализ полученных вариантов
            //1. проверка на вшивость всех вариантов распределения (не выходят ли они за лимиты)
            int i =0;
            while (i < result.condOptimalVariants.size())
            {
                AltDistrib var = result.condOptimalVariants.get(i);
                if (result.settings.limitedVar == 0 && result.getTotalTime(var) > result.timeLimit) //TIME
                {
                    result.condOptimalVariants.remove(i);
                    continue;
                }
                if (result.settings.limitedVar == 1 && result.getTotalCash(var) > result.budgetLimit) //COST
                {
                    result.condOptimalVariants.remove(i);
                    continue;
                }
                i++;
            }
            //2. В каждом ли распределении каждый реквест получил альтернативу
            i = 0;
            while (i < result.jobs.size())
            {
                UserJob job = result.jobs.get(i);
                boolean hasAlternative = false;
                boolean hasNoAlternative = false;
                for (AltDistrib var: result.condOptimalVariants)
                {
                   if (var.get(job.id) > -1)
                       hasAlternative = true;
                   else
                       hasNoAlternative = true;
                }
                //2.1. Везде проставлены альтернативы - ничего не делаем
                //2.2. есть как явные альтернативы, так и минус единицы - удаляем варианты с "-1"
                if (hasAlternative && hasNoAlternative)
                {
                    int j=0;
                    while (j < result.condOptimalVariants.size())
                    {
                        AltDistrib var = result.condOptimalVariants.get(j);
                        if (var.get(job.id) == -1)
                            result.condOptimalVariants.remove(j);
                        else
                            j++;
                    }
                }
                //2.3 для одного из реквестов отсутствуют альтернативы везде - удаляем реквест из результата
                if (!hasAlternative && hasNoAlternative)
                {
                    result.requestIndexes.remove(job.id);
                    result.jobs.remove(job);
                    continue;
                }
                i++;
            }
        }
        //Если в итоге ни одного реквеста не распределено или нет ни одного варианта - FAIL
        if (result.jobs.size() == 0 || result.condOptimalVariants.size()==0)
        {
            result.valid = false;
            return result;
        }
        //проставление поля bestAlternative в реквестах
        AltDistrib bestVariant = result.findOptimalDistribution();
        for (UserJob job: result.jobs)
        {
            job.bestAlternative = bestVariant.get(job.id);
        }
        return result;
    }


    //удаление идентичных альтернатив
    private void removeIdenticalVariants(AlternativeSolverResult result)
    {
        ArrayList<String> condStrings = new ArrayList<String>();
        for (int i=0; i< result.condOptimalVariants.size(); i++)
        {
            condStrings.add(result.condOptimalVariants.get(i).toString());
        }
        int i = 0;
        while (i< condStrings.size())
        {
            for (int j=i+1; j<condStrings.size(); j++)
            {
                if (condStrings.get(i).equals(condStrings.get(j)))
                {
                    condStrings.remove(j);
                    result.condOptimalVariants.remove(j);
                }
            }
            i++;
        }
    }

    //OBSOLETE, now implemented with settings
    public AlternativeSolverResult fakeSolve(ArrayList<UserJob> jobs)
    {
        AlternativeSolverResult result = new AlternativeSolverResult();
        result.valid = true;
        result.averageTime=0;
        result.averageCash=0;
        budgetLimit = 0;
        countBudgetLimit(jobs, null);
        for (UserJob job : jobs)
        {
           Random r = new Random();
           int num = r.nextInt(job.alternatives.size());
           job.bestAlternative = job.alternatives.get(num).num;
        }
        if (result.getTotalCash() > budgetLimit)
        {
            result.valid=false;
        }
        else 
        {
            result = countStatistics(jobs, result);
        }
        return result;
    }


    private AlternativeSolverResult countStatistics(ArrayList<UserJob> jobs, AlternativeSolverResult result)
    {
        result.averageTime=0;
        result.averageCash=0;
        for (UserJob job: jobs)
        {
            if (job.bestAlternative != -1)
            {
                result.averageTime+=job.getBestTime();
                result.averageCash+=job.getBestCost();
            }
        }
        result.averageTime /= jobs.size();
        result.averageCash /= jobs.size();
        return result;
    }

    private void solveIfSingleJob(ArrayList<UserJob> jobs, AlternativeSolverSettings settings, int limit){
        UserJob job = jobs.get(0);
        double curBestValue;
        boolean minmax;

        if("MIN".equals(settings.optType)){
            minmax = false;
            curBestValue = Double.POSITIVE_INFINITY;
        }
        else if("MAX".equals(settings.optType)){
            minmax = true;
            curBestValue = Double.NEGATIVE_INFINITY;
        }
        else
            return;

        for(int i=0;i<job.alternatives.size();i++){
            Alternative a = job.alternatives.get(i);
            double limVar = getLimitedValue(a, settings);
            if(limVar <= limit){
                double optVar = getOptimizedValue(a, settings);
                if((minmax)&&(optVar > curBestValue)||
                        (!minmax)&&(optVar < curBestValue)){
                    curBestValue = optVar;
                    job.bestAlternative = i;
                }
            }
        }
    }

    @Override
    public void flush() {
        tables = new ArrayList<AltTable>();
        budgetLimit = 0;
        timeLimit = 0;
    }

}
