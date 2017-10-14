package project.engine.data;

import java.io.Serializable;
import project.engine.data.DistributedTask;

import java.util.ArrayList;


/**
 * <p>Title: MIMAPR task</p>
 * <p/>
 * <p>Description: </p>
 * <p/>
 * <p>Copyright: Copyright (c) 2007</p>
 * <p/>
 * <p>Company: </p>
 *
 * @author GPUZE
 * @version 1.0
 */
public class Resource implements Serializable {

    private String name;
    private double speed;
    //compatibility issue
    public int index;

      public Resource(String name, double speed)
      {
          this.name = name;
          this.speed = speed;
      }

    public double getSpeed()
    {
        return speed;        
    }

    public String getName()
    {
        return name;
    }

    /**
     * @param speed the speed to set
     */
    public void setSpeed(double speed) {
        this.speed = speed;
    }

//    OLD MIMAPR CODE
//    public  String processorName;
//    private final int[] processorSpeed;
//    private int[] processorVolume;
//    public int index;
//    public ArrayList<DistributedTask> tasks;
//
//    public int occupiedFor;
//
//
//    public Resource() {
//        processorName = "Null processor";
//        int tasksNumber = 0;
//        processorSpeed = new int[tasksNumber];
//        processorVolume = new int[tasksNumber];
//        occupiedFor = 0;
//        tasks = new ArrayList<DistributedTask>();
//
//    }
//
//    public Resource(int numVertices, String Name) {
//        processorName = Name;
//        int tasksNumber = numVertices;
//        processorSpeed = new int[tasksNumber];
//        processorVolume = new int[tasksNumber];
//        occupiedFor = 0;
//        tasks = new ArrayList<DistributedTask>();
//
//    }
//
//    public String getName() {
//        return this.processorName;
//    }
//
//    public int getSpeed(int task) {
//        return processorSpeed[task];
//    }
//
//    public double getSpeed()
//    {
//        double result = 0;
//        for (int aProcessorSpeed : processorSpeed) {
//            result += aProcessorSpeed;
//        }
//        return result/processorSpeed.length;
//    }
//
//    public void setSpeed(int task, int speed) {
//        processorSpeed[task] = speed;
//    }
//
//    public int getVolume(int task) {
//        return processorVolume[task];
//    }
//
//    public void setVolume(int task, int speed) {
//        processorVolume[task] = speed;
//    }
//
//    public boolean isAvailable(int startTime,int taskIndex) {
//        boolean isAvailable = true;
//        int endTime = startTime+getSpeed(taskIndex);
//        for (DistributedTask task : tasks) {
//            if (endTime >= task.startTime && endTime <= task.endTime) {
//                isAvailable = false;
//                break;
//            }
//            if (startTime <= task.endTime && startTime >= task.startTime) {
//                isAvailable = false;
//                break;
//            }
//            if (startTime <= task.startTime && endTime >= task.endTime) {
//                isAvailable = false;
//                break;
//            }
//        }
//        return isAvailable;
//    }
//
//    public void shiftTails(int currentTime, int lastGraphTime) {
//        for (int j = tasks.size() - 1; j >= 0; j--) {
//            if (tasks.get(j).endTime - (currentTime - lastGraphTime) > 0) {
//                tasks.get(j).endTime -= (currentTime - lastGraphTime);
//                tasks.get(j).startTime -= (currentTime - lastGraphTime);
//                if (tasks.get(j).startTime < 0)
//                    tasks.get(j).startTime = 0;
//            } else {
//                tasks.remove(j);
//            }
//        }
//    }

}
