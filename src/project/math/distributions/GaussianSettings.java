/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package project.math.distributions;

/**
 *
 * @author Magica
 */
public class GaussianSettings extends DistributionSettings{

    public double max;
    public double min;
    public double avg;

    public GaussianSettings(double min, double avg, double max){
        this.min = min;
        this.max = max;
        this.avg = avg;
    }

}
