package project.engine.data.environmentGenerator;

import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: Rookie
 * Date: 18.05.2010
 * Time: 18:54:43
 * To change this template use File | Settings | File Templates.
 */
public class EnvironmentPricingSettings implements Serializable
{
    public double priceQuotient;           //general coefficient of price, depending on resource speed
    public double priceMutationFactor = 0;  //*100%, 0 is normal price, else is randomized
    public double speedExtraCharge = 0; //% of extra charge for every speed unit
    public double[] lengthLevels = null;
    public double[] lengthPriceQuotients = null;
    public double[] speedLevels = null;
    public double[] speedPriceQuotients = null;
    public double[] lengthConsts = null;

    public EnvironmentPricingSettings clone(){
        EnvironmentPricingSettings newEps = new EnvironmentPricingSettings();
        newEps.priceQuotient = priceQuotient;
        newEps.priceMutationFactor = priceMutationFactor;
        if(lengthLevels!=null) newEps.lengthLevels = lengthLevels.clone();
        if(lengthPriceQuotients!=null) newEps.lengthPriceQuotients = lengthPriceQuotients.clone();
        if(speedLevels!=null) newEps.speedLevels = speedLevels.clone();
        if(speedPriceQuotients!=null) newEps.speedPriceQuotients = speedPriceQuotients.clone();
        if(lengthConsts!=null) newEps.lengthConsts = lengthConsts.clone();
        return newEps;
    }
}
