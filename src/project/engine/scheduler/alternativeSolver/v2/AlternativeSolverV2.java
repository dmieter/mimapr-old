
package project.engine.scheduler.alternativeSolver.v2;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import project.engine.data.UserJob;
import project.engine.data.VOEnvironment;
import project.engine.scheduler.Scheduler;
import project.engine.scheduler.SchedulerSettings;
import project.engine.scheduler.alternativeSolver.v2.data.OptimizationEntity;
import project.engine.scheduler.alternativeSolver.v2.data.Option;
import project.math.utils.MathUtils;

/**
 *
 * @author emelyanov
 */
public class AlternativeSolverV2 extends Scheduler {

    /* input/output objects */
    protected AlternativeSolverSettingsV2 settings;
    protected List<UserJob> batchToProcess;
    
    /* Abstract structure for dynamic optimization: entities with options */
    protected List<OptimizationEntity> optimizationStructure;
    
    /* The table of options columns for backward and forward inductions */
    public OptimizationEntity[] optimizationTable;
    protected boolean isSchedulingSuccessful = false;
    
    /* We have a double limit now, need to have some steps for the table.. 
    Generally the result precision inversly depends on the step value */
    protected Double limit;
    protected Double limitStep;
    
    @Override
    public void solve(SchedulerSettings settings, VOEnvironment voenv, ArrayList<UserJob> batch) {
        this.settings = (AlternativeSolverSettingsV2)settings;
        this.batchToProcess = getJobsWithAlternatives(batch);
        
        optimizationStructure = new LinkedList<OptimizationEntity>();
        optimizationTable = new OptimizationEntity[batchToProcess.size()];
        
        isSchedulingSuccessful = false;
        
        if(prepareOptimizationProblem()){   /* Prepare optimization structure and limit params */
            performBackwardInduction();
            isSchedulingSuccessful = performForwardInduction();
        }else{
            System.out.println("Can't prepare optimization environment.");
        }
    }

    @Override
    public void flush() {
        isSchedulingSuccessful = false;
        /* everything is reinitialized inside solve mrthod */
    }
     
    protected boolean prepareOptimizationProblem(){
        if(batchToProcess.isEmpty()){
            System.out.println("Input job batch is empty. Return.");
            return false;
        }
        if(!checkAlternativesFound(batchToProcess)){
            System.out.println("Some jobs don't have any alternative executions.");
            return false;
        }
        
        for(UserJob job : batchToProcess){
            OptimizationEntity entity = new OptimizationEntity(job, settings);
            
            if(AlternativeSolverSettingsV2.MIN == settings.optType){
                entity.inverseValues(); /* MIN(f) ~ MAX(-f); By default we maximize! */
            }
            if(AlternativeSolverSettingsV2.MIN == settings.secondaryOptType){
                entity.inverseSecondaryValues(); /* MIN(f) ~ MAX(-f); By default we maximize! */
            }
            
            entity.orderOptionsByWeightAsc();
            
            optimizationStructure.add(entity);
        }
        
        prepareLimit();
        
        if(settings.cleanUpInferiorAlternatives){
            for(OptimizationEntity e : optimizationStructure){
                cleanUpInferiorOptions(e);
            }
        }
        
        return true;
    }

    /* Removing options providing worse value with bigger weight compared to others. Assuming options are ordered by weight! */
    protected void cleanUpInferiorOptions(OptimizationEntity entity){
        Double curValue = Double.NEGATIVE_INFINITY;
        
        for(Iterator<Option> it = entity.options.iterator();it.hasNext();){
            
            Option opt = it.next();
            if(opt.weight > limit){
                it.remove(); /* Don't check larger limits */
                continue;
            }
            
            if(opt.value >= curValue){
                curValue = opt.value;
            }else{
                it.remove();
            }
            
        }
    }
    
    private List<UserJob> getJobsWithAlternatives(ArrayList<UserJob> batch) {
        List<UserJob> batchWithAlts = new ArrayList<>(batch.size());
        
        boolean hasAlternatives = true;
        int failsNum = 0;
        
        for(UserJob job : batch){
            if(job.alternatives != null && !job.alternatives.isEmpty()){
                batchWithAlts.add(job);
            }else{
                hasAlternatives = false;
                failsNum++;
            }
        }
        
        if(!hasAlternatives){
            System.out.println(failsNum +" jobs don't have any alternative executions. \nRemoving them from scheduling list."
                    + "\nBe careful with limit calculation.");
            
        }
        
        return batchWithAlts;
    }
    
    private boolean checkAlternativesFound(List<UserJob> batch) {
        for(UserJob job : batch){
            if(job.alternatives == null || job.alternatives.isEmpty()){
                return false;
            }
        }
        
        return true;
    }
    
    protected void prepareLimit() {
        LimitSettings ls = settings.limitSettings;
        
        /* Limit calculation by type */
        switch(ls.limitType){
            case LimitSettings.LIMIT_TYPE_AVERAGE: limit = countAverageLimit(); break;
            case LimitSettings.LIMIT_TYPE_CONST: limit = ls.constLimit; break;
            case LimitSettings.LIMIT_TYPE_CONST_PROPORTIONAL: limit = ls.constLimit*optimizationStructure.size(); break;
            case LimitSettings.LIMIT_TYPE_EXTERNAL: limit = countExternalLimit(ls.externalJobs); break;
            default: limit = countAverageLimit();
        }
        
        /* Fine Tuning */
        limit *= ls.limitQuotient; 
        
        /* Limit step for table */
        if(ls.limitStep != null && ls.limitStep <= limit){
            this.limitStep = ls.limitStep;
        }else if(ls.limitStepQuotient > 0){
            this.limitStep = limit/ls.limitStepQuotient;
        }else{
            throw new RuntimeException("Limit settings are invalid!");
        }
        
        if(ls.roundLimitUp){
            limit = MathUtils.nextUp(limit);
            limitStep = MathUtils.nextUp(limitStep);
        }
    }
    
    protected Double countAverageLimit(){
        
        double averageLimit = 0d;
        
        for (OptimizationEntity entity : optimizationStructure)
        {
            double partialLimit = 0;
            for (Option o : entity.options)
            {
                partialLimit += o.weight;
            }
            
            /* Do we need to round partial average limits? Old AS makes this */
            if(settings.limitSettings.roundLimitUp){
                partialLimit = MathUtils.nextUp(partialLimit);
            }
            averageLimit += (partialLimit / entity.options.size());
        }
        
        return averageLimit;
    }

    protected Double countExternalLimit(List<UserJob> externalJobs) {
        double externalLimit = 0;
        
        if(externalJobs == null || externalJobs.isEmpty())
            throw new RuntimeException("AlternativeSolver: No external requests to count budget limit");

        
        for(Iterator<OptimizationEntity> it = optimizationStructure.iterator();it.hasNext();){      //through all job batch
            OptimizationEntity curEntity = it.next();
            boolean foundPairedRequest = false;                          
            for(Iterator<UserJob> extIt = externalJobs.iterator(); extIt.hasNext();){   //through all externel jobs to find the match an get value from it
                UserJob extJob = extIt.next();
                if(extJob.id == curEntity.job.id){                              // need to find external job with same id
                    if(extJob.bestAlternative < 0){
                        throw new RuntimeException("AlternativeSolver: bestAlternative for external job is null, id: "+extJob.id);
                    }
                    double aLimit = settings.optimizationConfig.getLimitValue(extJob.getBestAlternative());
                    /* Do we need to round it in case of round setting? */
                    if(settings.limitSettings.roundLimitUp){
                        aLimit = MathUtils.nextUp(aLimit);
                    }
                    externalLimit += aLimit;
                    foundPairedRequest = true;
                    break;
                }
            }
            if(!foundPairedRequest){
                throw new RuntimeException("AlternativeSolver: Can't find External Request with same id: "+curEntity.job.id);
            }
        }

        return externalLimit;
    }

    private void performBackwardInduction() {
        OptimizationEntity prevOptColumn = null; 
        /* Iterating over optimization structure backwards */
        for(int i = optimizationStructure.size() - 1; i >= 0; i--){
            OptimizationEntity entity = optimizationStructure.get(i);
            
            /* Calculating next options column for optimization table (with use of previous options) */
            OptimizationEntity nextOptColumn = createNextOptionsColumnCombinatorial(entity, prevOptColumn);
//            if(nextOptColumn.options.size() > settings.maxOptionsProcessing){
//                nextOptColumn.options = nextOptColumn.options.subList(0, (int)settings.maxOptionsProcessing);
//                System.out.println("AS2 cut options for column as the size was " + nextOptColumn.options.size());
//            }
            
            /* Adding it accordingly, backwards */
            optimizationTable[i] = nextOptColumn;
            prevOptColumn = nextOptColumn;
        }
    }

    private boolean performForwardInduction() {
        Double limitLeft = limit;
        //System.out.println("AS2 strategy consists of " + optimizationTable[0].options.size()+" different options");
        for(int i = 0; i < optimizationTable.length; i++){
            OptimizationEntity column = optimizationTable[i];   /* contains options with accumulated weights */
            Option bestOption = column.getBestOptionByWeightAndSecondaryValue(limitLeft, true);
            
            /* saving number of best option */
            if(bestOption != null){
                OptimizationEntity entity = optimizationStructure.get(i);
                entity.bestOptionNum = bestOption.alternativeNum;
                Option bestEntityOption = entity.getBestOption(); /* real option with real weight (not accumualted) */
                limitLeft -= bestEntityOption.weight;
            }else{
                return false;   /* For some column we don't have best option for the limit left */
            }
        }
        
        /* Success. Setting final result to jobs. */
        for(OptimizationEntity entity: optimizationStructure){
            if(entity.job != null){ /* for tests job may be null */
                entity.job.bestAlternative = entity.bestOptionNum;
            }
        }
        
        return true;
    }
    
    /* First approach with slow limit incrementation */
    protected OptimizationEntity createNextOptionsColumnIncremental(OptimizationEntity entity, OptimizationEntity prevColumn){
        OptimizationEntity column = new OptimizationEntity();
        
        double startingLimit = 0;
        if(prevColumn != null){
            startingLimit = prevColumn.minRequiredWeight;   /* To optimize calculations */
        }
        
        /* incrementing the current limit and searching for best options for each curLimit value */
        for(Double curLimit = startingLimit; curLimit <= limit; curLimit += limitStep){
            
            Option nextOption = null;
            
            if(prevColumn == null){
                /* Initial column */
                nextOption = getBestOptionForLimit(curLimit, entity);
            }else{
                /* The column is based on previous one */
                nextOption = getBestOptionForLimit(curLimit, entity, prevColumn);
            }
            
            if(nextOption != null){
                if(column.options.isEmpty()){       /* Adding first option */
                    column.minRequiredWeight = curLimit;
                    column.options.add(nextOption);
                }else{                              /* Checking if previous option is the same */
                    Option lastAddedOption = column.options.get(column.options.size()-1);
                    if(!lastAddedOption.equalsByCharacteristics(nextOption)){       /* We don't need identical options for similar limits */
                        column.options.add(nextOption);
                    }
                }
            }
        }
        
        return column;
    }
    
    
    protected Option getBestOptionForLimit(Double curLimit, OptimizationEntity entity){
        /* Best option and value for current limit weight */
        Option bestOption = null;
        Double bestValue = Double.NEGATIVE_INFINITY;
        
        for(Option opt : entity.options){
            if(opt.weight < curLimit){
                if(opt.value > bestValue){
                    bestValue = opt.value;
                    bestOption = opt;
                }
            }else{
                break;  /* Following options will be larger */
            }
        }
        
        if(bestOption != null){
            return new Option(bestOption);
        }else{
            return null;
        }
    }
    
    protected Option getBestOptionForLimit(Double curLimit, OptimizationEntity entity, OptimizationEntity prevColumn){
        /* Best option and value for current limit weight */
        
        Option bestOption = null;
        Option prevBestOption = null;
        Double bestValue = Double.NEGATIVE_INFINITY;

        for(Option opt : entity.options){
            if(opt.weight < curLimit){

                /* Found option, checking for limit left for the following options */
                Double limitLeft = curLimit - opt.weight;

                /* getting best option by the limit left (if there are any) */
                Option prevOption = prevColumn.getBestOptionByWeight(limitLeft, true);

                if(prevOption != null){
                    /* the total value for current option and best following option */
                    Double curValue = opt.value + prevOption.value;

                    /* Searching for maximum available total value among weight satisfying options */
                    if(curValue > bestValue){
                        bestOption = opt;
                        prevBestOption = prevOption;
                        bestValue = curValue;
                    }    
                }else{
                    break;  /* there's no option found for cur option and cur limit -> nex option will be heavier */
                }
            }else{
                break; /* assuming options are sorted by weight ascendingly -> next options will be even larger */
            }
        }

        if(bestOption != null){
            Option newOption = new Option(bestOption);
            if(prevBestOption != null){ /* should be null for first column */
                newOption.add(prevBestOption);
            }
            return newOption;
        }else{
            return null;
        }
    }
    
    
    /* This second approach doesn't use limit step, uses only possible options combinations */
    protected OptimizationEntity createNextOptionsColumnCombinatorial(OptimizationEntity entity, OptimizationEntity prevColumn){
        
        /* Should save us in case we have really many options */
        if(prevColumn != null && prevColumn.options.size()*entity.options.size() > settings.maxOptionsCombinatorialProcessing){
            //System.out.println("Switching to incremental approach: " + prevColumn.options.size());
            return createNextOptionsColumnIncremental(entity, prevColumn);
        }
        
        OptimizationEntity column = new OptimizationEntity();
        
        /* 1. Initialize new column with all possible combinations of current and previous step options */
        
        if(prevColumn == null){ /* First initial column */
            column.options.addAll(entity.options);
        }else{
            for(Option optCur : entity.options){
                for(Option optPrev : prevColumn.options){
                    Option optNew = new Option(optCur);
                    optNew.add(optPrev);
                    column.options.add(optNew);
                }
            }
        }
        
        /* 2. Sort all options by weight */
        column.orderOptionsByWeightAsc();
        
        
        /* 3. Remove options we don't need: having worse value with larger weight (compared to previous options in the list)*/
        cleanUpInferiorOptions(column);
        
        return column;    
    }
    
    public Double getOptimizedValue(double limit){
        
        OptimizationEntity firstColumn = optimizationTable[0];
        Option baseOption = firstColumn.getBestOptionByWeight(limit, true);
        
        if(baseOption != null){
            Double totalValue = baseOption.value;
            if(AlternativeSolverSettingsV2.MIN == settings.optType){
                totalValue *= -1;   /* MIN(f) ~ MAX(-f); By default we maximize! */
            }
            
            return totalValue;
        }
        
        return null;
    }
    
    public void printResult(){
        StringBuilder result = new StringBuilder();
        
        if(isSchedulingSuccessful){
            result.append("The scheduling is SUCCESSFUL!\n");
        }else{
            result.append("The scheduling FAILED!\n");
        }
        result.append("Total optimized value: ").append(getOptimizedValue(limit)).append("\n");
        result.append("The final combination for ").append(limit).append(" limit is: ");
        for(int i=0;i<optimizationStructure.size(); i++){
            OptimizationEntity e = optimizationStructure.get(i);
            result.append(e.bestOptionNum);
            if(i != optimizationStructure.size() - 1){
                result.append(" - ");
            }
        }
        
        System.out.println(result);
    }
    
    
    
    /************************************** TESTING ************************************************************/
    
    public void test(){
        prepareTestOptimizationProblem2();
        performBackwardInduction();
        isSchedulingSuccessful = performForwardInduction();
        printResult();
    }
    
    protected boolean prepareTestOptimizationProblem2(){
        
        /* the problem from Bobchenkov's paper 
        МЕТОД ПОИСКА ОПТИМАЛЬНОГО ПЛАНА ВЫПОЛНЕНИЯ 
        В МОДЕЛИ УПРАВЛЕНИЯ ПОТОКАМИ ЗАДАНИЙ 
        В РАСПРЕДЕЛЕННОЙ ВЫЧИСЛИТЕЛЬНОЙ СРЕДЕ */
        
        settings = new AlternativeSolverSettingsV2();
        settings.optType = AlternativeSolverSettingsV2.MAX;
        
        optimizationStructure = new ArrayList<OptimizationEntity>(3); 
        optimizationTable = new OptimizationEntity[3];
        
        limit = 10d;
        limitStep = 0.1153443d;
        
        OptimizationEntity entity1 = new OptimizationEntity();
                                 /* num weight value*/
        entity1.options.add(new Option(1, 7, 7));
        entity1.options.add(new Option(2, 5, 10));
        entity1.options.add(new Option(3, 3, 9));
        entity1.orderOptionsByWeightAsc();
        optimizationStructure.add(entity1);
        
        OptimizationEntity entity2 = new OptimizationEntity();
                                  /* num weight value*/
        entity2.options.add(new Option(1, 2, 3));
        entity2.options.add(new Option(2, 1, 2));
        entity2.orderOptionsByWeightAsc();
        optimizationStructure.add(entity2);
        
        OptimizationEntity entity3 = new OptimizationEntity();
                                 /* num weight value*/
        entity3.options.add(new Option(1, 5, 5));
        entity3.options.add(new Option(2, 4, 8));
        entity3.options.add(new Option(3, 2, 6));
        entity3.options.add(new Option(4, 1, 4));
        entity3.orderOptionsByWeightAsc();
        optimizationStructure.add(entity3);
        
        return true;
    }
    
    protected boolean prepareTestOptimizationProblem(){
        
        settings = new AlternativeSolverSettingsV2();
        settings.optType = AlternativeSolverSettingsV2.MAX;
        
        optimizationStructure = new ArrayList<OptimizationEntity>(3); 
        optimizationTable = new OptimizationEntity[3];
        
        //limit = 13.05d;
        //limitStep = 0.09d;
        
        limit = 10d;
        limitStep = 0.5d;
        
        OptimizationEntity entity1 = new OptimizationEntity();
                                 /* num weight value*/
        entity1.options.add(new Option(1, 5, 2));
        entity1.options.add(new Option(2, 2, 1));
        entity1.orderOptionsByWeightAsc();
        optimizationStructure.add(entity1);
        
        OptimizationEntity entity2 = new OptimizationEntity();
                                  /* num weight value*/
        entity2.options.add(new Option(1, 3, 2));
        entity2.options.add(new Option(2, 4, 3));
        entity2.options.add(new Option(3, 1, 1));
        entity2.orderOptionsByWeightAsc();
        optimizationStructure.add(entity2);
        
        OptimizationEntity entity3 = new OptimizationEntity();
                                 /* num weight value*/
        entity3.options.add(new Option(1, 5, 1));
        entity3.options.add(new Option(2, 4, 3));
        entity3.orderOptionsByWeightAsc();
        optimizationStructure.add(entity3);
        
        return true;
    }
    
}
