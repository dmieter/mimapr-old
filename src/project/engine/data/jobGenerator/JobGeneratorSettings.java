package project.engine.data.jobGenerator;



import java.util.*;
import project.math.distributions.DistributionGenerator;
import project.math.distributions.HyperGeometricSettings;
/**
 * Created by IntelliJ IDEA.
 * User: Rookie
 * Date: 16.11.2009
 * Time: 16:05:17
 * To change this template use File | Settings | File Templates.
 */
public class JobGeneratorSettings
{
    //resource request number in a batch
    public int taskNumber;
    //request priceMax parametrization
    public boolean useSpeedPriceFactor = false;
    public double minPrice;
    public double maxPrice;
    public double avgPrice;
    public DistributionGenerator maxPriceGen;
    //request time parametrization
    public double minTime;
    public double maxTime;
    public double avgTime;
    public DistributionGenerator timeGen;
    //request speed parametrization
    public double minSpeed;
    public double maxSpeed;
    public double avgSpeed;
    public DistributionGenerator perfGen;
    //request resourceNeed parametrization
    public int minCPU;
    public int maxCPU;
    public double avgCPU;
    public DistributionGenerator cpuNumGen;
    public DistributionGenerator timeCorrectiveCoefGen;
    //flags determining whether use the gaussian probability function during generation
    public boolean useGaussianPrice = false;
    public boolean useGaussianTime = false;
    public boolean useGaussianCPU = false;
    public boolean useGaussianSpeed = false;
    public Date absoluteStart; //absolute moment of generation start, optional parameter
    public int sortBy;

}
