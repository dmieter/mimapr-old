package project.engine.scheduler.alternativeSolver.v1.data;

/**
 * Created by IntelliJ IDEA.
 * User: Rookie
 * Date: 08.05.2010
 * Time: 0:26:18
 * To change this template use File | Settings | File Templates.
 */
public class ParetoTable
{
    public int[] VarIds;
    public double[] TColumn;
    public double[] CColumn;
    public double[] DColumn;
    public double[] IColumn;
    public double[] UFColumn;
    public boolean[] optimal;
    public int size;

    public ParetoTable(int size)
    {
        VarIds = new int[size];
        TColumn = new double[size];
        CColumn = new double[size];
        IColumn = new double[size];
        DColumn = new double[size];
        UFColumn = new double[size];
        optimal = new boolean[size];
        this.size = size;
    }

    public void normalizeColumn(double[] column)
    {
        double min = Integer.MAX_VALUE;
        double max = Integer.MIN_VALUE;
        for (int i=0; i<size; i++)
        {
            if (column[i]<min) min = column[i];
            if (column[i]>max) max = column[i];
        }
        for (int i=0; i<size; i++)
        {
            if (max != min)
                column[i] = (column[i] - min)/(max - min);
            else
                column[i] = 1;
        }
    }

    public void normalize()
    {
        normalizeColumn(TColumn);
        normalizeColumn(CColumn);
        normalizeColumn(DColumn);
        normalizeColumn(IColumn);
    }

    public double findMaxTime()
    {
        double max = Integer.MIN_VALUE;
        for (int i=0; i<size; i++)
        {
            if (TColumn[i]> max) max = TColumn[i];
        }
        return max;
    }

    public double findMaxBudget()
    {
        double max = Integer.MIN_VALUE;
        for (int i=0; i<size; i++)
        {
            if (CColumn[i]> max) max = CColumn[i];
        }
        return max;
    }
}
