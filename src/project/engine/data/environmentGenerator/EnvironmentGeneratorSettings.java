package project.engine.data.environmentGenerator;

import project.math.distributions.DistributionGenerator;
import project.math.distributions.HyperGeometricSettings;

/**
 * Created by IntelliJ IDEA.
 * User: Rookie
 * Date: 30.03.2010
 * Time: 23:52:41
 * To change this template use File | Settings | File Templates.
 */
public class EnvironmentGeneratorSettings
{
    //not used currently, resourceLines correspond to resource types array
    public int resourceLineNum;
    //occupancy level 1-10, an unit corresponds roughly to 10%
    public int occupancyLevel;
    //occupancy dustribution (variable - level for each line)
    public DistributionGenerator occupGenerator; 
    //interval to generate (local model time)
    public double timeInterval;
    //minimum task length
    public double minTaskLength;
    //maximum task length
    public double maxTaskLength;


    //Resource generation
    public double minResourceSpeed = -1;
    public double maxResourceSpeed = -1;
    public DistributionGenerator perfGenerator;

}
