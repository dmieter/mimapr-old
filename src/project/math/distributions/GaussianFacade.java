/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package project.math.distributions;

import java.util.Random;

/**
 *
 * @author Magica
 */
public class GaussianFacade extends DistributionGenerator{

    GaussianSettings settings;

    public GaussianFacade(GaussianSettings settings){
        this.settings = settings;
    }

    public double getRandom(GaussianSettings set)
    {
        Random rand = new Random();
        double maxDistance = Math.max(Math.abs(set.avg - set.min), Math.abs(set.avg - set.max));
            double newValue;
            do
            {
             newValue = (rand.nextGaussian())*maxDistance/3 + set.avg;
            }
            while (newValue > set.max || newValue < set.min);
        return newValue;
    }

    public double getRandom()
    {
        Random rand = new Random();
        double maxDistance = Math.max(Math.abs(settings.avg - settings.min), Math.abs(settings.avg - settings.max));
        double newValue;
        do
        {
            newValue = (rand.nextGaussian())*maxDistance/3 + settings.avg;
        }
        while (newValue > settings.max || newValue < settings.min);

//        System.out.println("GaussianFacade min = "+settings.min+" max = "+settings.max+
//               " avg = "+settings.avg+" Random = "+newValue);

        return newValue;
    }

    public int getRandomInteger()
    {
        return (int)getRandom();
    }
}
