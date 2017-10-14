package project.engine.scheduler.alternativeSolver.v1.data;
/**
 * Created by IntelliJ IDEA.
 * User: unco
 * Date: Oct 18, 2009
 * Time: 10:36:59 PM
 * To change this template use File | Settings | File Templates.
 */
public class AltColumn {
    int alternativeNumber;
    int size;
    public double[] value;

    public AltColumn(int altNumber, int altSize)
    {
        alternativeNumber = altNumber;
        size = altSize;
        value = new double[size];
    }


}