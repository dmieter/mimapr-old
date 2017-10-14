/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package project.engine.scheduler.alternativeSolver.v2.optimization;

import project.engine.data.Alternative;

/**
 *
 * @author emelyanov
 */
public class ConfigurableLimitedOptimization extends OptimizationConfig{

    public static final int TIME = 0;
    public static final int COST = 1;
    public static final int FINISH_TIME = 2;
    public static final int USER = 3;
    public static final int RUNTIME = 4;
    public static final int START_TIME = 5;
    
    protected int optimizationMode = TIME;
    protected int secondaryOptimizationMode = FINISH_TIME;      /* Will have the same optimization type (MIN/MAX) as a primary value */
    protected int limitMode = COST;
    
    public ConfigurableLimitedOptimization(int optimizationMode, int limitMode){
        this.optimizationMode = optimizationMode;
        this.limitMode = limitMode;
    }
    
    public ConfigurableLimitedOptimization(int optimizationMode, int secondaryOptimizationMode, int limitMode){
        this.optimizationMode = optimizationMode;
        this.secondaryOptimizationMode = secondaryOptimizationMode;
        this.limitMode = limitMode;
    }
    
    @Override
    public double getLimitValue(Alternative a) {
        return getAlternativeValueByMode(a, limitMode);
    }

    @Override
    public double getOptimizedValue(Alternative a) {
       return getAlternativeValueByMode(a, optimizationMode);
    }
    
    protected double getAlternativeValueByMode(Alternative a, int mode){
       switch(mode){
            case TIME           : return a.getLength();
            case COST           : return a.getCost();
            case FINISH_TIME    : return a.getStart() + a.getRuntime();
            case USER           : return a.getUserRating();
            case RUNTIME        : return a.getRuntime();
            case START_TIME     : return a.getStart();
            default             : return -1;
        }  
    }

    @Override
    public double getOptimizedSecondaryValue(Alternative a) {
        return getAlternativeValueByMode(a, secondaryOptimizationMode);
    }
    
}
