/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * JobBatchChart.java
 *
 * Created on 26.04.2010, 16:10:19
 */

package project.application.component.resourceDiagram;


import java.awt.*;
import java.util.*;
import project.engine.data.UserJob;
/**
 *
 * @author max
 */
public class JobBatchChart extends javax.swing.JPanel {

    private Dimension DrawingArea;
    protected ArrayList<UserJob> jobs;

    private HashMap<UserJob, Color> colorMap;

    public int jobSize;
    public int jobSpacing;
    public int vPosition;
    public int currentJob;


    private void myInit(){
        jobs = new ArrayList<UserJob>();
        DrawingArea = new Dimension(800,120);
        colorMap = new HashMap<UserJob, Color>();

        jobSize = 60;
        jobSpacing = 30;
        vPosition = 40;
        currentJob = 0;
    }

    protected void paintComponent(Graphics g) {
        setPreferredSize(DrawingArea);
        super.paintComponent(g);
        DrawArea(g);
        DrawBatch(g);
    }

    public void DrawChart(){
        Graphics g = this.getGraphics();
        paintComponent(g);
    }

    /** Creates new form JobBatchChart */
    public JobBatchChart() {
        initComponents();
        myInit();
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
    }

    public void SetJobs(ArrayList<UserJob> batch, HashMap<UserJob, Color> cMap){
        jobs.clear();
        UserJob J;
        for(int i=0;i<batch.size();i++){
            J = batch.get(i);
            jobs.add(J);
        }
        colorMap = cMap;
        DrawingArea = GetCurrentDimension();
        DrawChart();
    }

    public void SetSelectedJob(int n){
        currentJob = n;
        DrawChart();
    }

    private Dimension GetCurrentDimension(){
        int h = 117;
        int w = (jobs.size()+1)*(jobSize + jobSpacing) + jobSpacing;
        if(w<800) w=800;

        return new Dimension(w,h);
    }

    private void DrawBatch(Graphics g){
        Graphics2D g2 = (Graphics2D)g;
        int bWidth = jobs.size()*(jobSize + jobSpacing) + jobSpacing;
        if(bWidth<780) bWidth = 780;
        int bHeight = DrawingArea.height - 40;

        g2.setColor(new Color(0.4f, 0.4f, 0.4f));
        g2.setStroke(new BasicStroke(1));
        g2.drawRoundRect(10, 30, bWidth, bHeight, 20, 20);
        g2.setStroke(new BasicStroke(1));

        UserJob J;
        int hOffset;
        for(int i=0;i<jobs.size();i++){
            J = jobs.get(i);
            hOffset = i*(jobSize+jobSpacing) + jobSpacing;
            DrawJob(J, hOffset, new Color(0.1f, 0.7f, 0.1f), g);
        }

        DrawJobCursor(g);

    }

    private void DrawJob(UserJob J, int hOffset, Color c, Graphics g){
        Graphics2D g2 = (Graphics2D)g;
        Color col = colorMap.get((Object)J);
        g2.setColor(col);                   //Draw Body
        g2.fillRoundRect(hOffset, vPosition, jobSize, jobSize, 40, 20);

        
        g.setColor(Color.BLACK);
        String criteria;                //Draw Criteria
        if(J.resourceRequest.criteria!=null){
            criteria = J.resourceRequest.criteria.getDescription();
        }else{
            criteria = "min Start";
        }
        g.setFont(new Font("SansSerif", Font.BOLD, 10));
        g2.drawString(criteria, hOffset+jobSize/10, vPosition - (int)(1*jobSize/5));
        
        
        g.setFont(new Font("SansSerif", Font.BOLD, 12));            //Draw task name
        g2.drawString(J.name, hOffset+jobSize/5, vPosition + (int)(2.83*jobSize/5));

        g2.setColor(new Color(0.4f, 0.4f, 0.4f));                   //DrawBorders
        g2.setStroke(new BasicStroke(1));
        g2.drawRoundRect(hOffset, vPosition, jobSize, jobSize, 40, 20);
        g2.setStroke(new BasicStroke(1));
    }

    private void DrawJobCursor(Graphics g){
        Graphics2D g2 = (Graphics2D)g;

        if(currentJob!=-1){             //if picked anything

            int hOffset = currentJob*(jobSize+jobSpacing) + jobSpacing;
            g2.setColor(new Color(0.1f, 0.7f, 0.1f));                   //DrawBorders
            //g2.setColor(Color.GRAY);
            g2.setStroke(new BasicStroke(2));
            g2.drawRoundRect(hOffset-5, vPosition-5, jobSize+10, jobSize+10, 20, 20);
            g2.setColor(Color.WHITE);
            g2.drawLine(hOffset-5, vPosition + 12, hOffset-5, vPosition + jobSize - 10);
            g2.drawLine(hOffset + jobSize + 5, vPosition + 12, hOffset + jobSize + 5, vPosition + jobSize - 12);
            g2.drawLine(hOffset + 12, vPosition - 5, hOffset + jobSize -12, vPosition - 5);
            g2.drawLine(hOffset + 12, vPosition + jobSize + 5, hOffset + jobSize -12, vPosition + jobSize + 5);
            g2.setStroke(new BasicStroke(1));

        }
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
            .addGap(0, 486, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 134, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables

}
