/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package project.application.component.resourceDiagram;

import project.engine.data.DistributedTask;
import project.engine.data.Resource;

import java.awt.Color;

/**
 *
 * @author Администратор
 */
public class DistributedTaskDraw extends DistributedTask {
        public Color c;
        public int borderwidth;
        public DistributedTaskDraw(String n, double start, double end, Color c){
            taskName = n;
            startTime = start;
            endTime = end;
            this.c = c;
            //cpu = new Resource();
            borderwidth = 1;
         }
        public DistributedTaskDraw(DistributedTask t, Color c){
            startTime = t.startTime;
            endTime = t.endTime;
            //cpu = new Resource();
            //cpu.index = t.cpu.index;
            taskName = t.taskName;
            this.c = c;
            borderwidth = 1;
        }
    }
