/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package project.application.component.resourceDiagram;

/**
 *
 * @author Administrator
 */
import project.engine.data.DistributedTask;
import project.engine.data.Resource;

import java.awt.*;
import java.util.*;

public class DistributionResultsDraw {
    private Color color;
    private String name = "";
    private String Description = "";
    private double startTime;
    private double endTime;
    private double arrivalTime;     //time, the distribution arrived
    public ArrayList<DistributedTask> tasks;

    /**
     * @return the color
     */
    public Color getColor() {
        return color;
    }

    /**
     * @param color the color to set
     */
    public void setColor(Color color) {
        this.color = color;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the Description
     */
    public String getDescription() {
        return Description;
    }

    /**
     * @param Description the Description to set
     */
    public void setDescription(String Description) {
        this.Description = Description;
    }

    /**
     * @return the startTime
     */
    public double getStartTime() {
        return startTime;
    }

    /**
     * @return the endTime
     */
    public double getEndTime() {
        return endTime;
    }

    /**
     * @param endTime the endTime to set
     */
    public void setEndTime(double endTime) {
        this.endTime = endTime;
    }

    /**
     * @param startTime the startTime to set
     */
    public void setStartTime(double startTime) {
        this.startTime = startTime;
    }

    /**
     * @return the arrivalTime
     */
    public double getArrivalTime() {
        return arrivalTime;
    }

    /**
     * @param arrivalTime the arrivalTime to set
     */
    public void setArrivalTime(double arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

            /**
     * calculates start and end time from tasks ArrayList
     */
    public void calculateTimeFrames(){
        startTime = Double.MAX_VALUE;
        endTime = Double.MIN_VALUE;
        for(DistributedTask dt: tasks)
        {
            if(dt.startTime<startTime)
                startTime = dt.startTime;
            if(dt.endTime>endTime)
                endTime = dt.endTime;
        }
    }

//    public DistributionResultsDraw(DistributionResults results){
//        DistributedTask task;
//        tasks = new ArrayList<DistributedTask>();        //copying tasks
//        for(DistributedTask dt : results.tasks){
//            task = new DistributedTask(dt.taskName, dt.startTime, dt.endTime);
//            task.cpu = new Resource(0,dt.cpu.getName());
//            task.cpu.index = dt.cpu.index;
//            tasks.add(task);
//        }
//
//        color = Color.BLACK;                //copying fields
//        collisionNumber = results.collisionNumber;
//        outcome = results.outcome;
//        textOutcome = results.textOutcome;
//        arrivalTime = 0;
//
//        calculateTimeFrames();              //calculating time frames
//    }
//
//    public DistributionResultsDraw(DistributionResults results, int currenttime){
//        DistributedTask task;
//        tasks = new ArrayList<DistributedTask>();        //copying tasks
//        for(DistributedTask dt : results.tasks){
//            task = new DistributedTask(dt.taskName, dt.startTime+currenttime, dt.endTime+currenttime);
//            //now times are adjusted and absolute!!!
//            task.cpu = new Resource(0,dt.cpu.getName());
//            task.cpu.index = dt.cpu.index;
//            tasks.add(task);
//        }
//
//        color = Color.BLACK;                //copying fields
//        collisionNumber = results.collisionNumber;
//        outcome = results.outcome;
//        textOutcome = results.textOutcome;
//
//        arrivalTime = currenttime;
//
//        calculateTimeFrames();              //calculating time frames
//    }
//
//    public DistributionResultsDraw(DistributionResults results, Color c){
//        DistributedTask task;
//        tasks = new ArrayList<DistributedTask>();        //copying tasks
//        for(DistributedTask dt : results.tasks){
//            task = new DistributedTask(dt.taskName, dt.startTime, dt.endTime);
//            task.cpu = new Resource(0,dt.cpu.getName());
//            task.cpu.index = dt.cpu.index;
//            tasks.add(task);
//        }
//
//        color = c;                //copying fields
//        collisionNumber = results.collisionNumber;
//        outcome = results.outcome;
//        textOutcome = results.textOutcome;
//        arrivalTime = 0;
//
//        calculateTimeFrames();              //calculating time frames
//    }

}
