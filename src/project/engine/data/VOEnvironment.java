package project.engine.data;

import java.io.Serializable;
import project.engine.data.environmentGenerator.EnvironmentPricingSettings;

import java.util.ArrayList;
import java.util.Random;
import project.math.distributions.GaussianFacade;
import project.math.distributions.GaussianSettings;

/**
 * Created by IntelliJ IDEA.
 * User: Rookie
 * Date: 07.04.2010
 * Time: 2:00:42
 * To change this template use File | Settings | File Templates.
 */
public class VOEnvironment implements Serializable
{
    public ArrayList<ResourceLine> resourceLines;
    public EnvironmentPricingSettings pcSettings;

    public VOEnvironment()
    {
        resourceLines = new ArrayList<ResourceLine>();
    }

    public double getTotalOccupancy()
    {
        double s = 0;
        for (ResourceLine rLine: resourceLines)
        {
            s += rLine.getTotalOccupancyTime();
        }
        return s;
    }

    private void calculateNLConsts()
    {
        if (pcSettings != null && pcSettings.lengthLevels != null && pcSettings.lengthLevels.length > 1 &&
            pcSettings.lengthPriceQuotients != null && pcSettings.lengthPriceQuotients.length == pcSettings.lengthLevels.length)
        {
            pcSettings.lengthConsts = new double[pcSettings.lengthLevels.length];
            pcSettings.lengthConsts[0] = 0.0;
            for (int i=1; i< pcSettings.lengthConsts.length; i++)
            {
                pcSettings.lengthConsts[i] = -pcSettings.lengthLevels[i]*pcSettings.lengthPriceQuotients[i] +
                                             pcSettings.lengthLevels[i]*pcSettings.lengthPriceQuotients[i-1] +
                                             pcSettings.lengthConsts[i-1];
            }
        }
    }

    public void applyPricing(EnvironmentPricingSettings pcSettings)
    {
       for (ResourceLine rline: resourceLines)
       {
           GaussianSettings gs = new GaussianSettings(-pcSettings.priceMutationFactor, 0, pcSettings.priceMutationFactor);
           GaussianFacade generator = new GaussianFacade(gs);
           //double mutationPriceCoef = pcSettings.priceMutationFactor*(1 - 2*(new Random().nextDouble())) + 1;
           double mutationPriceCoef = 1 + generator.getRandom();
           if(mutationPriceCoef<0)
               mutationPriceCoef=0;

           if (pcSettings != null && pcSettings.speedLevels != null && pcSettings.speedLevels.length > 1 &&
            pcSettings.speedPriceQuotients != null && pcSettings.speedPriceQuotients.length == pcSettings.speedLevels.length)
          {
              for (int i=pcSettings.speedLevels.length - 1; i>=0; i--)
              {
                  if (rline.getSpeed()>= pcSettings.speedLevels[i])
                  {
                      mutationPriceCoef = pcSettings.speedPriceQuotients[i];
                      rline.price = rline.getSpeed()*mutationPriceCoef;
                      break;
                  }
              }
          }
           else{
              //rline.price = rline.getSpeed()*pcSettings.priceQuotient*mutationPriceCoef;
              //rline.price*= (1+(rline.getSpeed()*pcSettings.speedExtraCharge)/100);
              rline.price = rline.getSpeed()*Math.exp(pcSettings.speedExtraCharge*(rline.getSpeed()-1));
              rline.price *= pcSettings.priceQuotient*mutationPriceCoef;
              int b = 0;
           }
              
       }
       this.pcSettings = pcSettings;
       calculateNLConsts();
    }

    public double getSlotCost(double price, double length)
    {
        if (pcSettings != null && pcSettings.lengthLevels != null && pcSettings.lengthLevels.length > 1 &&
            pcSettings.lengthPriceQuotients != null && pcSettings.lengthPriceQuotients.length == pcSettings.lengthLevels.length)
        {
            for (int i=pcSettings.lengthLevels.length -1; i>=0; i--)
            {
                if (length >= pcSettings.lengthLevels[i])
                {
                    return (price*pcSettings.lengthPriceQuotients[i] + pcSettings.lengthConsts[i])*length;
                }
            }
        }
        return price*length;
    }

   public void debugInfo() {
       for (ResourceLine rline: resourceLines)
       System.out.println(rline.debugInfo());
   }  

}
