
package project.math.distributions;

/**
 *
 * @author Magica
 */
import cern.jet.random.HyperGeometric;
import cern.jet.random.AbstractDistribution;
import cern.jet.random.engine.RandomEngine;

public class HyperGeometricFacade extends DistributionGenerator{

    HyperGeometricSettings settings;
    double mean, e;
    HyperGeometric hp;// = new HyperGeometric(500, 200, 100, r);

    public HyperGeometricFacade(HyperGeometricSettings set){
        settings = set;
        setParams();
    }

    public void setDistributionParameters(HyperGeometricSettings set){
        settings = set;
        setParams();
    }

    private void setParams(){
        RandomEngine r = AbstractDistribution.makeDefaultGenerator();
        
        hp = new HyperGeometric(settings.M, settings.D, settings.n, r);
        mean = settings.D*settings.n/settings.M;
        e = Math.sqrt((settings.n*((double)settings.D/settings.M)*
                (1-(double)settings.D/settings.M)*(settings.M-settings.n))/(settings.M-1));
    }

    public double getRandom(){
        
       double res = 0;
//       System.out.println("HyperGeometricFacade N = "+settings.M+" D = "+settings.D
//               +" n = "+settings.n+" mean = "+mean+
//               " e = "+e);


       if(settings.distributionType == HyperGeometricSettings.DEFAULT_DISTRIBUTION){
           res = hp.nextInt();
           System.out.println("Real Random: "+res);
       }else if(settings.distributionType == HyperGeometricSettings.MINMAX_DISTRIBUTION){
           int depth = 50;
           while(!((res>settings.kMin)&&(res<settings.kMax))){
               res = hp.nextInt();
               depth--;
               if(depth == 0){
                   throw new RuntimeException("Eternal cycle: Incorrect input parameters!");
               }
           }
           //System.out.println("Real Random: "+res);
           res-=settings.kMin;
           res/=(settings.kMax-settings.kMin);
           res*=(settings.max-settings.min);
           res+=settings.min;
           //System.out.println("Min: "+settings.min+" Max: "+settings.max);
       }

//       System.out.println("Random: "+res);
        return res;
    }

     public int getRandomInteger(){

        int res = (int)getRandom();

        return res;
    }
    
}
