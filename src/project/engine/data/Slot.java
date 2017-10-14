/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package project.engine.data;

import java.io.Serializable;
import project.engine.data.ResourceLine;
import java.util.Random;

/**
 *
 * @author Administrator
 */
public class Slot implements Serializable
{
    public long id;
    //Resource on which this slot is allocated
    public ResourceLine resourceLine;
    //start time
    public double start;
    //end time
    public double end;

    public Slot(double start, double end, ResourceLine rLine)
    {
        this.id = (new Random()).nextLong();
        this.start = start;
        this.end = end;
        this.resourceLine = rLine;
        //this.id = -1;
    }

    public double getVolumeCost(double volume)
    {
        return resourceLine.getVolumeCost(volume);
    }
    
    public double getVolumeCost(Window w)
    {
        if(w.squareWindow){
            return resourceLine.getSlotCost(w.length);
        }else{
            return getVolumeCost(w.volume);
        }
    }

    public double getCost()
    {
        return resourceLine.getSlotCost(getLength());
    }
    
    public double getLengthCost(double length)
    {
        return resourceLine.getSlotCost(length);
    }

    public double getVolumeTime(double volume)
    {
        return resourceLine.getPerformanceTime(volume);
    }
    
    public double getVolumeTime(Window w)
    {
        if(w.squareWindow){
            return w.length;
        }else{
            return resourceLine.getPerformanceTime(w.volume);
        }
    }

    public double getSpeed()
    {
        return resourceLine.getSpeed();
    }
    
    public double getLength()
    {
        return end - start;
    }

    @Override
    public Slot clone()
    {
        Slot newobj = new Slot(this.start, this.end, this.resourceLine);
        newobj.id = this.id;
        return newobj;
    }

    public void refreshId()
    {
      this.id = (new Random()).nextLong();
    }

    public double getPrice()
    {
        return resourceLine.price;
    }
}