/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package project.math.utils;

import java.util.Random;

/**
 *
 * @author Rookie
 */
public class MathUtils {

   public static double getGaussian(double min, double max, double avg)
    {
        Random rand = new Random();
        double maxDistance = Math.max(Math.abs(avg - min), Math.abs(avg - max));
            double newValue;
            do
            {
             newValue = (rand.nextGaussian())*maxDistance/3 + avg;
            }
            while (newValue > max || newValue < min);
        return newValue;
    }


    public static int getUniform(int min, int max)
    {
        if (min == max)
            return min;
        Random rand = new Random();
         return rand.nextInt(max - min + 1) + min;
    }

    public static double getUniform(double min, double max)
    {
        Random rand = new Random();
        return rand.nextDouble()*(max - min) + min;
    }

    public static double nextUp(double value) {
        return -Math.floor(-value);
    }

}
