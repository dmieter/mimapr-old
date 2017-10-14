package project.engine.scheduler.alternativeSolver.v1.data;

import java.util.HashSet;
import java.util.ArrayList;


/**
 * Created by IntelliJ IDEA.
 * User: unco
 * Date: 18.10.2009
 * Time: 15:02:23
 * To change this template use File | Settings | File Templates.
 */


public class AltTable
{
    int level = 0;

    public AltColumn[] altColumns;
    public double[] criteriaValues;
    public ArrayList<Integer>[] suitableAlternatives;
    int tableSize;
    int alternativeCount;


    public AltTable(int taskCount, int size)
    {
        tableSize = size;
        alternativeCount = taskCount;
        altColumns = new AltColumn[alternativeCount];
        criteriaValues = new double[size];
        suitableAlternatives = new ArrayList[size];
    }

    public String printTable()
    {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tableSize; i++)
        {
            sb.append( String.valueOf(i) + ";");
            for (AltColumn altColumn : altColumns)
            {
                String sValue;
                if (altColumn.value[i] == Integer.MAX_VALUE)
                {
                    sValue = "Inf";
                } else
                    sValue = String.valueOf(altColumn.value[i]);

                sb.append( "t" + String.valueOf(altColumn.alternativeNumber) + "=" + sValue + ";");
            }
            String sValue;
            if (criteriaValues[i] == Integer.MAX_VALUE) {
                sValue = "Inf";
            } else
                sValue = String.valueOf(criteriaValues[i]);

            sb.append( "f=" + sValue + ";n=" + suitableAlternatives[i]);
            sb.append( "\n");
        }

        return sb.toString();

    }


    public void fillResults(int maxValue, int altCount, String criteriaType)
    {

        for (int i = 0; i <= maxValue; i++)
        {
            int optIndex = 0;
            //find a minimum on i-th row of table from values of all alternatives
            if (criteriaType.equals("MIN"))
            {
                for (int j = 1; j < altCount; j++)
                {
                    if (altColumns[j].value[i] < altColumns[optIndex].value[i])
                    {
                        optIndex = j;
                    }
                }
            }
            //find a maximum on i-th row of table from values of all alternatives
            if (criteriaType.equals("MAX"))
            {
                boolean maxFound=false;
                int firstMaxIndex=0;
                for (int j=0; j < altCount; j++)
                {
                    if (altColumns[j].value[i] != Integer.MAX_VALUE && !maxFound)
                    {
                        firstMaxIndex=j;
                        optIndex=j;
                        maxFound=true;
                    }
                }
                if (maxFound)
                {
                    for (int j = firstMaxIndex; j < altCount; j++)
                    {
                        if (altColumns[j].value[i] > altColumns[optIndex].value[i] && altColumns[j].value[i]<Integer.MAX_VALUE) {
                            optIndex = j;
                        }
                    }
                }
                else 
                {
                    optIndex=0;
                }
            }
            criteriaValues[i] = altColumns[optIndex].value[i];
            suitableAlternatives[i] = new ArrayList<Integer>();
            if (criteriaValues[i] != Integer.MAX_VALUE)
            {
                for (int k=0; k<altCount; k++)
                {
                    if (altColumns[k].value[i] == altColumns[optIndex].value[i])
                    suitableAlternatives[i].add(altColumns[k].alternativeNumber);
                }
            }
        }
    }

    public void lastTable(int limit)
    {
        for (int i = 0; i < limit; i++)
        {
            criteriaValues[i] = Integer.MAX_VALUE;
            suitableAlternatives[i] = null;
        }
    }


}