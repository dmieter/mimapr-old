/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * ResourceLineChart.java
 *
 * Created on 15.04.2010, 21:46:59
 */

package project.application.component.resourceDiagram;

import java.awt.*;
import java.util.*;
import project.engine.data.ResourceLine;
import project.engine.data.Slot;
import project.engine.data.DistributedTask;
import project.engine.data.Alternative;
import project.engine.data.VOEnvironment;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;



/**
 *
 * @author РђРґРјРёРЅРёСЃС‚СЂР°С‚РѕСЂ
 */
public class ResourceLineChart extends javax.swing.JPanel {

    private ArrayList<ResourceLine> cpus;
    private ArrayList<Alternative> alternatives;
    public int currentTime;
    public int scheduleLength;

    private Dimension DrawingArea;
    private HashMap<Alternative, Color> colorMapAlts;
    private HashMap<Alternative, Color> colorMapJobs;

    int drawScale;
    private BufferedImage imChip;
    public int rowheight;
    public int rowdelta;
    public int cpuinfolength;
    public int cpunamepos;
    public int paramspos;

    protected void myInit(){

        cpus = new ArrayList<ResourceLine>();
        alternatives = new ArrayList<Alternative>();

        colorMapAlts = new HashMap<Alternative, Color>();
        colorMapJobs = new HashMap<Alternative, Color>();

        currentTime = 0;
        scheduleLength = 600;

        drawScale = 1;
        rowheight = 50;
        rowdelta = 8;
        cpuinfolength = 240;
        cpunamepos = 50;
        paramspos = 160;

        DrawingArea = new Dimension(0,0);

        LoadImages();
    }

    private void LoadImages(){
        try
        {
            // the line that reads the image file
            imChip = ImageIO.read(new File("images//chip.jpg"));
            // work with the image here ...
        }
        catch (IOException e)
        {
            // log the exception
            // re-throw if desired
        }

    }

    /** Creates new form ResourceLineChart */
    public ResourceLineChart() {
        initComponents();
        myInit();
    }

    //inherited paint method
    protected void paintComponent(Graphics g) {
        setPreferredSize(DrawingArea);
        super.paintComponent(g);
        DrawArea(g);
        DrawEnvironment(g);
        DrawAlternatives(g);
        DrawLines(g);
        }

    public void DrawChart(){
        Graphics g = this.getGraphics();
        paintComponent(g);
    }

    //Calculates current dimensions of the chart - w*h rectangle
    private Dimension GetCurrentDimension(){
      Dimension d;
      int h = 400,w = 100, maxt=0;
      maxt = GetLastTaskEnd();
      if(currentTime>maxt)
          maxt = currentTime;
      w = maxt+cpuinfolength+20;
      h = (cpus.size()+1)*rowheight;
      //w = (int)(drawScale*maxt) + beginpixels + 15;
      //h = (cpus.size()+2)*rowheight*2;
      d = new Dimension(w,h);
      return d;
  }
    private int GetLastTaskEnd(){
        double sEnd = 400, tEnd = 400;
        Slot s;
        DistributedTask t;
        for(ResourceLine rl : cpus){
            if(rl.slots.size()>0){
                s = rl.slots.get(rl.slots.size()-1);
                if(s.end > sEnd)
                    sEnd = s.end;
            }
            if(rl.tasks.size()>0){
                t = rl.tasks.get(rl.tasks.size()-1);
                if(t.endTime>tEnd)
                    tEnd = t.endTime;
            }
        }

        if(tEnd>sEnd)
           return (int)tEnd;
        else
           return (int)sEnd;
    }

    private void DrawArea(Graphics g){
        Graphics2D g2 = (Graphics2D)g;
        float grad = 0;
        float start = 0.82f;
        float end = 0.99f;
        /*for(int i=0;i<DrawingArea.height;i++){
            grad = start + i*(end-start)/DrawingArea.height;
            g2.setColor(new Color(grad, grad, grad, 0.9f));
            g2.drawLine(0, i, DrawingArea.width, i);
        }*/
//        float start = 0.9f;
//        float end = 0.95f;
//        for(int i=0;i<DrawingArea.height;i++){
//            grad = start + i*(end-start)/DrawingArea.height;
//            g2.setColor(new Color(grad, grad, grad, 1f));
//            g2.drawLine(0, i, DrawingArea.width, i);
//        }
    }
    private void DrawLines(Graphics g){
        Graphics2D g2 = (Graphics2D)g;
        g2.setColor(new Color(0.0f, 0.3f, 0.1f));
        g2.setStroke(new BasicStroke(2));
        g2.drawLine(cpuinfolength+1, 0, cpuinfolength+1, DrawingArea.height);
        g2.setStroke(new BasicStroke(1));
    }

    private void DrawEnvironment(Graphics g){
        ResourceLine cpu;
        int offset = 0;                 //vertical offset
        for(int i=0;i<cpus.size();i++){
            offset = (i+1)*rowheight;
            cpu = cpus.get(i);
            DrawCpuLine(cpu, offset, g);
        }
        drawModelTime(g);
    }
    
    private void drawModelTime(Graphics g){
        Graphics2D g2 = (Graphics2D)g;
        g2.setColor(new Color(0.0f, 0.3f, 0.1f));
        //g2.setStroke(new BasicStroke(2));
        g2.drawLine(cpuinfolength+currentTime, 0, cpuinfolength+currentTime, DrawingArea.height);
        //g2.setStroke(new BasicStroke(1));
    }

    private void DrawCpuLine(ResourceLine cpu, int vOffset, Graphics g){
        Graphics2D g2 = (Graphics2D)g;
        g2.setColor(new Color(0.01f, 0.65f, 0.1f, 1f));         //Body 1
        g2.fillRect(0, vOffset-rowheight, cpuinfolength, rowheight);

        g2.setColor(new Color(1f, 1f, 1f, 0.4f));               //Body 2
        g2.fillRoundRect(0, vOffset-rowheight, cpuinfolength, rowheight-1*rowdelta, 15, 15);

        g2.drawImage(imChip,5,vOffset-rowheight+rowdelta/2,null);

        g2.setColor(new Color(1f, 1f, 1f));                     //Name
        g2.setFont(new Font("SansSerif", Font.BOLD,15));
        g2.drawString(cpu.name, cpunamepos, vOffset-rowheight/2.2f);

        g2.setFont(new Font("SansSerif", Font.BOLD,12));        //Price + Speed
        g2.drawString("Speed: "+ cpu.resourceType.getSpeed(), paramspos, vOffset - 2*rowdelta);
        g2.drawString("Price: "+ String.format("%.2f", cpu.price), paramspos, vOffset-rowheight+2*rowdelta);

        DistributedTask t;                      //Draw Tasks
        for(int i=0;i<cpu.tasks.size();i++){
            t = cpu.tasks.get(i);
            DrawTask(t, vOffset, g);
        }

        Slot s;                                 //Draw Slots
        for(int i=0;i<cpu.slots.size();i++){
            s = cpu.slots.get(i);
            DrawSlot(s, vOffset, g);
        }

        g2.setColor(new Color(0.2f, 0.2f, 0.2f));       //Draw Time Line
        g2.drawLine(0, vOffset-rowdelta, DrawingArea.width, vOffset-rowdelta);
    }
    private void DrawTask(DistributedTask t, int vOffset, Graphics g){
        Graphics2D g2 = (Graphics2D)g;
                                                //task start-end parametres init
         int start = (int)(t.startTime*drawScale);      //SCALE!!!!
         int end = (int)(t.endTime*drawScale);
                                                //Draw task
         g2.setColor(new Color(0.1f, 0.7f, 0.1f));      //Green
         g2.fillRoundRect(start+cpuinfolength, vOffset-rowheight/2-rowdelta, end-start, rowheight/2, 10, 10);

         g.setColor(Color.BLACK);               //Draw task name
         g.setFont(new Font("SansSerif", Font.BOLD, 12));
         int textposition = start+cpuinfolength+5;
         g.drawString(t.taskName, textposition, vOffset-rowheight/3);

         g2.setColor(Color.GRAY);               //Draw borders
         g2.setStroke(new BasicStroke(1));
         g2.drawRoundRect(start+cpuinfolength, vOffset-rowheight/2-rowdelta, end-start, rowheight/2, 10, 10);
         g2.setStroke(new BasicStroke(1));
    }
    private void DrawColoredTask(DistributedTask t, int vOffset, Color c, Graphics g){
        Graphics2D g2 = (Graphics2D)g;
                                                //task start-end parametres init
         int start = (int)(t.startTime*drawScale);      //SCALE!!!!
         int end = (int)(t.endTime*drawScale);
                                                //Draw task
         g2.setColor(c);
         g2.fillRoundRect(start+cpuinfolength, vOffset-rowheight/2-rowdelta, end-start, rowheight/2, 8, 8);

         g.setColor(Color.WHITE);               //Draw task name
         g.setFont(new Font("SansSerif", Font.BOLD, 12));
         int textposition = start+cpuinfolength+5;
         g.drawString(t.taskName, textposition, vOffset-rowheight/3);

         g2.setColor(new Color(0f,0f,0.4f));               //Draw borders
         g2.setStroke(new BasicStroke(1));
         g2.drawRoundRect(start+cpuinfolength, vOffset-rowheight/2-rowdelta, end-start, rowheight/2, 8, 8);
         g2.setStroke(new BasicStroke(1));
    }
    private void DrawSlot(Slot s, int vOffset, Graphics g){
        Graphics2D g2 = (Graphics2D)g;
                                                //task start-end parametres init
         int start = (int)(s.start*drawScale);      //SCALE!!!!
         int end = (int)(s.end*drawScale);

         g.setColor(Color.BLACK);               //Draw slot name
         g.setFont(new Font("SansSerif", Font.BOLD, 12));
         int textposition = start+cpuinfolength+5;
         //g.drawString("" + s.id, textposition, vOffset-rowheight/3);

         g2.setColor(new Color(0.4f, 0.4f, 0.4f, 1f));               //Draw borders
         g2.setStroke(new BasicStroke(1));
         g2.drawRoundRect(start+cpuinfolength, vOffset-rowheight/2-rowdelta, end-start, rowheight/2, 20, 20);
         g2.setStroke(new BasicStroke(1));
    }
    private void DrawAlternativeSlot(Slot s, int vOffset, String jobName, Color c, boolean stroke, Graphics g){
        Graphics2D g2 = (Graphics2D)g;
                                                //task start-end parametres init
         int start = (int)(s.start*drawScale);      //SCALE!!!!
         int end = (int)(s.end*drawScale);
                                                //Draw task
         g2.setColor(c);
         g2.fillRoundRect(start+cpuinfolength, vOffset-rowheight/2-rowdelta, end-start, rowheight/2, 8, 8);

         g.setColor(Color.BLACK);               //Draw task name
         g.setFont(new Font("SansSerif", Font.BOLD, 10));
         int textposition = start+cpuinfolength+5;
         g.drawString(jobName, textposition, vOffset-rowheight/3);

         g2.setColor(new Color(0.4f,0.4f,0.4f));               //Draw borders
         float[] dashPattern = {5,3};

         if(stroke)
             g2.setStroke(new BasicStroke(2, BasicStroke.JOIN_MITER, BasicStroke.JOIN_ROUND, 10, dashPattern, 0));
         g2.drawRoundRect(start+cpuinfolength, vOffset-rowheight/2-rowdelta, end-start, rowheight/2, 8, 8);
         g2.setStroke(new BasicStroke(1));
    }
    private void DrawAlternatives(Graphics g){
        for(Alternative A : alternatives){
            Color c = colorMapAlts.get(A);
            DrawAlternative(A, A.name, c, true, g);     //drawing with strokes
        }
    }
    private void DrawAlternative(Alternative A, String jobName, Color c, boolean stroke, Graphics g){
        Slot s;
        ResourceLine rl;
        int vOffset;
        for(int i=0;i<A.window.slots.size();i++){           //drawing all slots from Alternative window
            s = A.window.slots.get(i);
            for(int j=0;j<cpus.size();j++){                 // finding where to draw this slot
                rl = cpus.get(j);
                if(rl.id == s.resourceLine.id){             //found the same cpu
                    vOffset = (j+1)*rowheight;              //calculating offset
                    DrawAlternativeSlot(s, vOffset, jobName, c, stroke, g);     //drawing there
                    break;                                  //to next slot in Alternative
                }
            }
        }
    }
    //Sets current time variable
    public void SetCurrentTime(int time){
        currentTime = time;
    }

    //Sets cpu lines - environment
    public void SetVOEnvironment(VOEnvironment env){
        ResourceLine cpu;
        cpus = new ArrayList<ResourceLine>();       //erasing
        for (ResourceLine rl : env.resourceLines){
            cpu = new ResourceLine(rl);
            cpus.add(cpu);
        }
        DrawingArea = GetCurrentDimension();
        DrawChart();
    }

    public void SetAlternativesToDraw(ArrayList<Alternative> alts, Color c){
        Alternative A;
        colorMapAlts = new HashMap<Alternative, Color>();
        alternatives = new ArrayList<Alternative>();
        for(int i=0;i<alts.size();i++){
            A = alts.get(i);
            alternatives.add(A);
            colorMapAlts.put(A, c);
        }
        DrawingArea = GetCurrentDimension();
    }

     public void AddAlternativesToDraw(ArrayList<Alternative> alts, Color c){
        Alternative A;
        for(int i=0;i<alts.size();i++){
            A = alts.get(i);
            alternatives.add(A);
            colorMapAlts.put(A, c);
        }
        DrawingArea = GetCurrentDimension();
    }
     
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 424, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 311, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables

}
