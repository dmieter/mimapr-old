package project.engine.scheduler.alternativeSolver.v1;

import java.util.HashMap;
import java.util.Arrays;

/**
 * Created by IntelliJ IDEA.
 * User: Rookie
 * Date: 04.05.2010
 * Time: 23:11:23
 * To change this template use File | Settings | File Templates.
 */
public class AltDistrib
{
    public int id;
    private int[] data;
    HashMap<Integer, Integer> indexes;

    public AltDistrib(int size, HashMap<Integer, Integer> indexes)
    {
        data = new int[size];
        this.indexes = indexes;
        for (int i=0; i<size; i++)
        {
            data[i] = -1;
        }
    }

    public int get(int requestNum)
    {
        int index = indexes.get(requestNum);
        return data[index];
    }

    public void put(int requestNum, int altNum)
    {
        int index = indexes.get(requestNum);
        data[index] = altNum;
    }

    public String debugInfoShort()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Variant: (");
        for (Integer id :indexes.keySet())
        {
           sb.append(id+":");
           sb.append(get(id)+"; ");
        }
        sb.append(")\n\n");
        return sb.toString();
    }

    public AltDistrib copy()
    {
        AltDistrib newObj = new AltDistrib(this.data.length, this.indexes);
        System.arraycopy(this.data, 0, newObj.data, 0, this.data.length);
        return newObj;
    }

    public boolean isAllRequests()
    {
        for (Integer id :indexes.keySet())
        {
           if (get(id) == -1)
               return false;
        }
        return true;
    }
}
