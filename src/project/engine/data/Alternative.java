/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package project.engine.data;

import project.math.utils.MathUtils;

/**
 *
 * @author Administrator
 */


public class Alternative
{
    public int num;                   // alternative number
    
    private double sumLength;
    private double runtime;
    private double cost;               // alternative cost
    private double userRating = 0;
    
    public Window window;
    public String name;
    public double userTimeCorrectiveCoef = 1;  // coefficient to shrink alternative for real time

    public Alternative(Window w)
    {
        cost = 0;
        window = w;

        runtime = 0;
        for (Slot s: window.slots)
        {
            cost += s.getCost();
        }
        for (Slot s: window.slots)
        {
            sumLength += s.getLength();

            if(s.getLength()>runtime)
                runtime = s.getLength();
        }
        
        cost = MathUtils.nextUp(cost);
        runtime = MathUtils.nextUp(runtime);
        sumLength = MathUtils.nextUp(sumLength);
    }
    
    //For test purposes
    public Alternative(double length, double cost)
    {
        this.sumLength = length;
        this.cost = cost;        
    }

    @Override
    public Alternative clone(){
        Alternative a = new Alternative(this.sumLength, this.cost);
        a.runtime = this.runtime;
        a.name = this.name;
        a.num = this.num;
        a.setUserRating(this.userRating);
        a.window = this.window.clone();
        a.userTimeCorrectiveCoef = this.userTimeCorrectiveCoef;
        return a;
    }

    public double getLength()
    {
        //if (window != null)
        //return window.sumLength;
        return sumLength;
        //return runtime;
    }
    
    public void roundLength(){
        sumLength = -Math.floor(-sumLength);
    }
    
    public void roundCost(){
        cost = -Math.floor(-cost);
    }

    public double getRuntime(){
        return runtime;
    }

    public double getCost()
    {
        return cost;
    }

    public double getStart() {
        if (window != null)
        return window.start;
        else return 0;
    }

    /**
     * @return the userEstimation
     */
    public double getUserRating() {
        return userRating;
    }
    
    public void setUserRating(double userRating){
        this.userRating = userRating;
    }

    public void calculateUserRating(){
        userRating = num;   /* Simple rule: first alternative is the best, generally we need to minimize ratings */
    }
    
}
