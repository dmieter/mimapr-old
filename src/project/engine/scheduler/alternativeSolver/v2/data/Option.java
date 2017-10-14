package project.engine.scheduler.alternativeSolver.v2.data;

import project.engine.data.Alternative;
import project.engine.scheduler.alternativeSolver.v2.optimization.OptimizationConfig;

/* Base entity for Dynamic Programming induction */
public class Option {
    public int alternativeNum;
    
    public Double weight;
    public Double value;
    public Double secondaryValue;
    
    public Option(Alternative a, OptimizationConfig config){
        alternativeNum = a.num;
        weight = config.getLimitValue(a);
        value = config.getOptimizedValue(a);
    }
    
    public Option(int alternativeNum, double weight, double value){
        this.alternativeNum = alternativeNum;
        this.weight = weight;
        this.value = value;
        this.secondaryValue = value;
    }
    
    public Option(int alternativeNum, double weight, double value, double secondaryValue){
        this.alternativeNum = alternativeNum;
        this.weight = weight;
        this.value = value;
        this.secondaryValue = secondaryValue;
    }
    
    public Option(Option option){
        this.alternativeNum = option.alternativeNum;
        this.weight = option.weight;
        this.value = option.value;
        this.secondaryValue = option.secondaryValue;
    }
    
    public void add(Option option){
        if(option != null){
            this.weight += option.weight;
            this.value += option.value;
            this.secondaryValue += option.secondaryValue;
        }
    }
    
    public boolean equalsByCharacteristics(Option option){
        return (option != null && weight.equals(option.weight) && value.equals(option.value) 
                                                && secondaryValue.equals(option.secondaryValue));
    }
}
