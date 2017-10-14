package project.engine.scheduler.alternativeSolver.v2.data;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import project.engine.data.Alternative;
import project.engine.data.UserJob;
import project.engine.scheduler.alternativeSolver.v2.AlternativeSolverSettingsV2;
import project.math.utils.MathUtils;


public class OptimizationEntity {
    public UserJob job;
    public List<Option> options;
    
    public int bestOptionNum = -1;
    public Double minRequiredWeight = 0d;

    public OptimizationEntity(UserJob job, AlternativeSolverSettingsV2 settings)
    {
        this.job = job;
        options = new LinkedList<Option>();
        
        for(Alternative a : job.alternatives){
            Double aWeight = settings.optimizationConfig.getLimitValue(a);
            Double aVal = settings.optimizationConfig.getOptimizedValue(a);
            Double aVal2 = settings.optimizationConfig.getOptimizedSecondaryValue(a);
            
            if(settings.limitSettings.roundLimitUp){
                aWeight = MathUtils.nextUp(aWeight);
            }
            Option o = new Option(a.num, aWeight, aVal, aVal2);
            options.add(o);
        }
    }
    
    public OptimizationEntity(){
        options = new LinkedList<Option>();
    }
    
    /* Searching for best option by value (max) which satisfies the weight limit */
    public Option getBestOptionByWeight(Double weight, boolean assumeSortedByWeight){
        Option bestOption = null;
        Double bestValue = Double.NEGATIVE_INFINITY;
        for(Option opt : options){
            if(opt.weight <= weight){
                if(bestValue < opt.value){
                    bestValue = opt.value;
                    bestOption = opt;
                }
            }else if(assumeSortedByWeight){   
                break; /* If options are sorted by weight then all following options will be bigger then limit */
            }
        }
        return bestOption;
    }
    
    /* Searching for best option by value (max) and secondary value (max) which satisfies the weight limit */
    public Option getBestOptionByWeightAndSecondaryValue(Double weight, boolean assumeSortedByWeight){
        Option bestOption = null;
        Double bestValue = Double.NEGATIVE_INFINITY;
        Double curSecondaryValue = Double.NEGATIVE_INFINITY;
        
        for(Option opt : options){
            if(opt.weight <= weight){
                if(bestValue < opt.value){
                    bestValue = opt.value;
                    bestOption = opt;
                    curSecondaryValue = opt.secondaryValue;
                }else if(bestValue.equals(opt.value)){
                    if(opt.secondaryValue > curSecondaryValue){
                        bestOption = opt;
                        curSecondaryValue = opt.secondaryValue;
                    }
                }
            }else if(assumeSortedByWeight){   
                break; /* If options are sorted by weight then all following options will be bigger then limit */
            }
        }
        return bestOption;
    }
    
    public Option getOptionByNum(int num){
        if(options != null){
            for(Option opt : options){
                if(opt.alternativeNum == num) {
                    return opt;
                }
            }
        }
        
        return null;
    }
    
    public Option getBestOption(){
        return getOptionByNum(bestOptionNum);
    }
    
    public void inverseValues(){
        for(Option opt : options){
            opt.value *= -1;
        }
    }
    
    public void inverseSecondaryValues(){
        for(Option opt : options){
            opt.secondaryValue *= -1;
        }
    }
    
    public void orderOptionsByWeightAsc(){
        Collections.sort(options ,new Comparator<Option>()
           {
                public final int compare ( Option a, Option b )
                {
                    return Double.compare(a.weight, b.weight);
                }
            });
    }
}