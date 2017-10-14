/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package project.math.distributions;

import project.math.utils.MathUtils;

/**
 *
 * @author emelyanov
 */
public class UniformFacade extends DistributionGenerator{

    int min, max;

    public UniformFacade(int min, int max){
        this.min = min;
        this.max = max;
    }

    public void setInterval(int min, int max){
        this.min = min;
        this.max = max;
    }

    @Override
    public double getRandom() {
        return MathUtils.getUniform((double)min, (double)max);
    }

    @Override
    public int getRandomInteger() {
        return MathUtils.getUniform(min, max);
    }

}
