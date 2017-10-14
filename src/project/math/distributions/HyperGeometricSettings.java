
package project.math.distributions;

/**
 *
 * @author Magica
 */
public class HyperGeometricSettings extends DistributionSettings{

    public final static int DEFAULT_DISTRIBUTION = 0;
    public final static int MINMAX_DISTRIBUTION = 1;
    
    public int distributionType = DEFAULT_DISTRIBUTION;
    
    public int M;       // Hypergeometric distribution main params
    public int D;
    public int n;      //N = 10000 D = 5000 n = 5000 mean = 2500.0 e = 20     [0;10]

    public int kMin;    // represent initial interval
    public int kMax;
    
    public int min;     // represent out interval
    public int max;




    public HyperGeometricSettings(int min, int max){
        distributionType = MINMAX_DISTRIBUTION;
        setParameters(10000, 5000, 5000, 2470, 2530, min, max);
    }

    public HyperGeometricSettings(int M, int D, int n){
        distributionType = DEFAULT_DISTRIBUTION;
        this.M = M;
        this.D = D;
        this.n = n;
    }

    public HyperGeometricSettings(int M, int D, int n, int kMin, int kMax, int min, int max){
        distributionType = MINMAX_DISTRIBUTION;
        setParameters(M, D, n, kMin, kMax, min, max);
    }

    private void setParameters(int M, int D, int n, int kMin, int kMax, int min, int max){
        this.M = M;
        this.D = D;
        this.n = n;
        this.kMin = kMin;
        this.kMax = kMax;
        this.min = min;
        this.max = max;
    }
}
