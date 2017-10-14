package project.engine.data;


/**
 *
 * @author Dmitry
 */

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

//Resource Line in Virtual Organization
public class ResourceLine implements Serializable
{
    public ArrayList<DistributedTask> tasks = new ArrayList<DistributedTask>();
    public ArrayList<Slot> slots = new ArrayList<Slot>();
    public float load=0;
    public float averagetasklength=0;
    public int id;
    public String name;
    public Resource resourceType;
    public double price = 0;               //cash per time 4 using this resource

    public VOEnvironment environment;

    public ResourceLine(int cpuid, String cname)
    {
        name = cname;
        id = cpuid;
        load = 0;
        averagetasklength = 0;
    }

    public ResourceLine(ResourceLine c)
    {
        load = c.load;
        averagetasklength = c.averagetasklength;
        name = c.name;
        id = c.id;
        price = c.price;
        resourceType = c.resourceType;

        environment = c.environment;

        tasks = new ArrayList<DistributedTask>();
        for(int i=0;i<c.tasks.size();i++)
        {
            DistributedTask t1 = c.tasks.get(i).clone();
            tasks.add(t1);
        }

        slots = new ArrayList<Slot>();
        Slot s;
        for(int i=0;i<c.slots.size();i++){
            s = c.slots.get(i).clone();
            s.resourceLine = this;
            slots.add(s);
        }
    }

    public ResourceLine(Resource rc)
    {
        load = 0;
        averagetasklength = 0;
        resourceType = rc;
        name = rc.getName();
        id = rc.index;
        price = 0;
    }

    public void AddTask(String n, double start, double end)
    {
        DistributedTask t2, t3;
        if(tasks.size()>0)
        {
            t2 = tasks.get(0);
            if(start < t2.startTime)
            {
                tasks.add(0, new DistributedTask(n,start,end));    //adding as first task
                return;
            }
        }
        for(int i=0; i<tasks.size() - 1; i++)
        {        //finding a hole to add task
            t2 = tasks.get(i);
            t3 = tasks.get(i+1);
            if (t2.endTime <= start && end <= t3.startTime)
            {      //we have ok logic, so we have to insert t between t2 and t3
                tasks.add(i+1, new DistributedTask(n,start,end));             //soinserting it to i position
                return;
            }
        }
        tasks.add(new DistributedTask(n,start,end));        //inserting to end if not to hole
    }


    public double getTotalOccupancyTime()
    {
        double s= 0;
        for (DistributedTask t: tasks)
        {
           s += (t.endTime - t.startTime);
        }
        return s;
    }

    public boolean isReserved(double start, double end)
    {
        for (DistributedTask t: tasks)
        {
           if (start >= t.startTime && end <= t.endTime  ||    //inside
               start<= t.startTime && end>= t.endTime    ||         //outside
               start <= t.startTime && end>=t.startTime && end<=t.endTime ||
               start >=t.startTime && start<=t.endTime && end>= t.endTime)
               return true;
        }
        return false;
    }

    //Testing purposes
    public boolean tasksDoNotIntersect()
    {
        Collections.sort(tasks, new Comparator<DistributedTask>()
       {
            public final int compare ( DistributedTask a, DistributedTask b )
            {
               return Double.compare(a.startTime, b.startTime);
            }
        });
        for (int i=0; i<tasks.size()-1; i++)
        {
            if (tasks.get(i).endTime > tasks.get(i+1).startTime)
                return false;
        }
        return true;
    }

    public DistributedTask getTask(int i){              //get i-th task in cpu local schedule
        if(i<tasks.size())
            return(tasks.get(i));
        else
            return null;
    }

    public void getStats(int time){
        int sum = 0;
        int count = 0;
        DistributedTask t;
        for(int i=0;i<tasks.size();i++){
            t = tasks.get(i);
            if(time>=t.endTime){
                sum+=t.endTime-t.startTime;
                count++;
            }
            else if(t.startTime<time){
                sum+=time-t.startTime;
                count++;
                break;
            }
            else
                break;
        }
        if(time!=0)
            load = sum*100/time;
        else load = 0;
        if(count!=0)
            averagetasklength = sum/count;
        else averagetasklength = 0;
    }
    


    public ArrayList<Slot> getSlots(){
        DistributedTask t1,t2;
        Slot s;
        ArrayList<Slot> slots = new ArrayList<Slot>();
        double infinity_value = Integer.MAX_VALUE;
        if (this.tasks.size() == 0)
        {
            s = new Slot(0, infinity_value, this);
            slots.add(s);
        }
        if(this.tasks.size() >= 1)
        {
            t1 = this.getTask(0);                    //1st slot - before first task
            s = new Slot(0, t1.startTime, this);
            slots.add(s);
            if(this.tasks.size() > 1)
            {
               for(int j=0; j < this.tasks.size() - 1;j++)
               {
                    s = new Slot(this.getTask(j).endTime, this.getTask(j+1).startTime, this);      //slot between tasks
                    slots.add(s);             //adding
                }
             }
             t2 = this.getTask(this.tasks.size()-1);       //last task - to get last infinite slot
             s = new Slot(t2.endTime, infinity_value, this);
             slots.add(s);                     //adding
         }
        return slots;
    }

    public double getSpeed()
    {
        return resourceType.getSpeed();
    }

    public String debugInfo()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("##### RESOURCE LINE #####\n");
        for (int i=0; i< this.tasks.size(); i++)
        {
            sb.append("task #"+i+"  start = "+this.tasks.get(i).startTime+"; end = "+this.tasks.get(i).endTime+"\n");
        }
        sb.append("speed: " + this.getSpeed() + "\n");
        sb.append("price: " + this.price + "\n");
        return sb.toString();
    }

    //Aiming for future non-linear slot cost

    public double getPerformanceTime(double volume)
    {
       return Math.ceil(volume/ getSpeed());
    }

    public double getSlotCost(double length)
    {
        return environment.getSlotCost(price, length);
    }

    public double getVolumeCost(double volume)
    {
        return environment.getSlotCost(price, getPerformanceTime(volume));
    }

}




