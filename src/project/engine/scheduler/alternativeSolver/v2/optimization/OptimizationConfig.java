
package project.engine.scheduler.alternativeSolver.v2.optimization;

import project.engine.data.Alternative;

/**
 *
 * @author emelyanov
 */
public abstract class OptimizationConfig {
    public abstract double getLimitValue(Alternative a);
    public abstract double getOptimizedValue(Alternative a);
    public abstract double getOptimizedSecondaryValue(Alternative a);
}
