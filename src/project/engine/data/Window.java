/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package project.engine.data;

import java.util.*;

import project.engine.data.ResourceRequest;
import project.engine.data.Slot;


public class Window
{
    //window start time
    public double start;
    //window volume
    public double volume;
    //window height = slot need
    public int slotsNeed;
    //window length
    public double length;
    //maximum resource price constraint
    public double priceMax;
    //speed constraint
    public double speed;

    public double time;

    public double maxCost;

    public ArrayList<Slot> slots = new ArrayList<Slot>();

    public boolean squareWindow = false;

    public Window(int id, int slotsNumber)
    {     //other parametres will be initialised during the work
        this.slotsNeed = slotsNumber;
    }
    @Override
    public Window clone(){
        Window w = new Window(0, this.slotsNeed);
        w.length = this.length;
        w.maxCost = this.maxCost;
        w.priceMax = this.priceMax;
        w.speed = this.speed;
        w.start = this.start;
        w.time = this.time;
        w.volume = this.volume;
        w.slots = VOEHelper.copySlotList(this.slots);

        return w;
    }
    public Window(ResourceRequest rr)
    {
        slotsNeed = rr.resourceNeed;
        time = rr.time;
        priceMax = rr.priceMax;
        speed = rr.resourceSpeed;
        volume = time*speed;
        maxCost = slotsNeed * priceMax * time;
    }

    public double getTotalVolumeCost()
    {
        double s = 0;
        for (Slot slot: slots)
        {
           s += slot.getVolumeCost(this);
        }
        return s;
    }

    public void sortSlotsByCost()
    {
       final Window thisWindow = this;
       Collections.sort(slots,new Comparator<Slot>()
       {
            public final int compare ( Slot a, Slot b )
            {
                return Double.compare(a.getVolumeCost(thisWindow), b.getVolumeCost(thisWindow));
            }
        });
    }
    
    public void sortSlotsByCostDesc()
    {
       final Window thisWindow = this;
       Collections.sort(slots,new Comparator<Slot>()
       {
            public final int compare ( Slot a, Slot b )
            {
                return Double.compare(b.getVolumeCost(thisWindow), a.getVolumeCost(thisWindow));
            }
        });
    }

    public boolean checkForCost(){
        double sum = 0;
        for(int i=0;i<slotsNeed;i++){
            Slot s = slots.get(i);
            sum += s.getVolumeCost(this);
        }

        return (sum <= maxCost);
    }
}
