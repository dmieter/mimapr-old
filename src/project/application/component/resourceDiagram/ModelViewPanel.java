/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * cpuChartPanel.java
 *
 * Created on 15.04.2010, 21:11:02
 */

package project.application.component.resourceDiagram;

import java.util.ArrayList;
import java.util.HashMap;
import java.awt.*;
import javax.swing.JScrollBar;
import project.experiment.processor.DynamicProcessorHelper;
import project.engine.data.ResourceLine;
import project.engine.data.Alternative;
import project.engine.data.UserJob;
import project.engine.data.VOEHelper;
import project.engine.data.VOEnvironment;
import project.engine.scheduler.SchedulerOperations;

/**
 *
 * @author Р С’Р Т‘Р С�Р С‘Р Р…Р С‘РЎРѓРЎвЂљРЎР‚Р В°РЎвЂљР С•РЎР‚
 */
public class ModelViewPanel extends javax.swing.JPanel {

    protected ResourceLineChart cpuChart;
    protected JobBatchChart jobChart;
    protected VOEnvironment env;
    protected ArrayList<DistributionResultsDraw> distributions;
    protected ArrayList<UserJob> jobs;
    protected HashMap<UserJob, Color> colorMap;
    protected Color bestAlternativeColor = Color.WHITE;

    protected int selectedJob;
    protected int altDrawingMode;   //0 - nothing, 1 - selected alt, 2 - all of selected job, 3 - all alts
    protected int nextcolor;

    protected int currentTime;
    protected int scheduleLength;
    
    protected DynamicProcessorHelper manualProcessor;


    private void myInit(){
        cpuChart = new ResourceLineChart();
        jScrollPaneChart.setViewportView(cpuChart);

        jobChart = new JobBatchChart();
        jScrollPaneJobs.setViewportView(jobChart);

        env = new VOEnvironment();
        distributions = new ArrayList<DistributionResultsDraw>();
        jobs = new ArrayList<UserJob>();

        selectedJob = 0;
        altDrawingMode = 0;

        nextcolor = 4;

        currentTime = 0;
        scheduleLength = 500;

        buttonGroupAlts.add(jRadioButtonAll);
        buttonGroupAlts.add(jRadioButtonJobAll);
        buttonGroupAlts.add(jRadioButtonOne);
        buttonGroupAlts.add(jRadioButtonDrawBest);
        buttonGroupAlts.add(jRadioButtonNone);


    }
    
    public void SetEnvironment(VOEnvironment voe){
        env = VOEHelper.copyEnvironment(voe);
        cpuChart.SetVOEnvironment(env);
    }
    
    public void setModelTime(int modelTime){
        currentTime = modelTime;
        cpuChart.SetCurrentTime(currentTime);
    }

    public void LoadJobAlternatives(ArrayList<UserJob> batch, boolean clear){
        if(clear){
            jobs.clear();
        }
        
        if(colorMap == null) {
            colorMap = new HashMap<UserJob, Color>();
        }
        
        Color c;
        for(int i=0;i<batch.size();i++){
            UserJob job = batch.get(i);
            jobs.add(job);
            if(!colorMap.containsKey(job)){
                c = GetNextColor();
                colorMap.put(job, c);
            }
        }
        jobChart.SetJobs(jobs, colorMap);
    }

    //Gets next colour in color cycle
    private Color GetNextColor() {
        Color c;
        switch (nextcolor) {
            case 0:
                c = Color.YELLOW;
                break;
            case 1:
                c = new Color(180,120,0);
                break;
            case 2:
                c = Color.GREEN;
                break;
            case 3:
                c = Color.CYAN;
                break;
            case 4:
                c = Color.BLUE;
                break;
            case 5:
                c = Color.MAGENTA;
                break;
            case 6:
                c = Color.RED;
                break;
            default:
                c = Color.YELLOW;
        }
        nextcolor++;
        if (nextcolor == 7) {
            nextcolor = 0;
        }
        return c;
    }
    //calculates what job is selected after mouse clicking to (x,y)
    private void SelectJob(int x, int y){
      // int hOffset = i*(jobSize+jobSpacing) + jobSpacing;
        int hOffset;
        if((y<jobChart.vPosition)||(y>(jobChart.vPosition+jobChart.jobSize)))
            return;
        for(int i=0;i<jobs.size();i++){
            hOffset = i*(jobChart.jobSize+jobChart.jobSpacing)+jobChart.jobSpacing;
            if((x>=hOffset)&&(x<=(hOffset+jobChart.jobSize))){
                selectedJob = i;
                jobChart.SetSelectedJob(selectedJob);
                UpdateAlternativesList();
                updateJobCriteriaLabel();
                break;
            }
        }
    }

    private void UpdateAlternativesList(){
        //String huy[];
        UserJob J = jobs.get(selectedJob);
        Alternative A;
        String altStrings[] = new String[J.alternatives.size()];
        for(int i=0;i < J.alternatives.size();i++){
            A = J.alternatives.get(i);
            altStrings[i] = "Alternative " + A.num;
        }
        jListJobAlternatives.setListData(altStrings);
    }
    
    private void updateJobCriteriaLabel(){
        UserJob J = jobs.get(selectedJob);
        String criteria;
        if(J.resourceRequest.criteria!=null){
            criteria = J.resourceRequest.criteria.getDescription();
        }else{
            criteria = "min Start";
        }
        jLabelCriteria.setText("Criteria: "+criteria);
    }

    private void DrawAlternatives(){
        UserJob J;
        Alternative A;
        ArrayList<Alternative> alts = new ArrayList<Alternative>();
       
        switch(altDrawingMode){
            case 0:{
                cpuChart.SetAlternativesToDraw(alts, Color.red);        //erasing
                cpuChart.DrawChart();
                break;
            }
            case 1:{
                J = jobs.get(selectedJob);
                A = J.alternatives.get(jListJobAlternatives.getSelectedIndex());
                DrawOneAlternative(A, J);
                break;
            }
            case 2:{
                J = jobs.get(selectedJob);
                DrawJobAlternatives(J);
                break;
            }
            case 3:{
                DrawAllAlternatives();
                break;
            }
            case 4:{
                DrawAllBestAlternatives();
                break;
            }
        }
        return;
    }

    private void DrawOneAlternative(Alternative A, UserJob J){
        Color c = colorMap.get((Object)J);
        ArrayList<Alternative> alts = new ArrayList<Alternative>();
        alts.add(A);
        cpuChart.SetAlternativesToDraw(alts, c);
        cpuChart.DrawChart();
    }

    private void DrawJobAlternatives(UserJob J){
        Alternative A;
        ArrayList<Alternative> alts = new ArrayList<Alternative>();
        ArrayList<Alternative> bestAlt = new ArrayList<Alternative>();
        for(int i=0;i<J.alternatives.size();i++){
            A = J.alternatives.get(i);
            if(i==J.bestAlternative)
                bestAlt.add(A);                   //Best Alternative will be in another color
            else
                alts.add(A);
        }
        Color c = colorMap.get((Object)J);
        cpuChart.SetAlternativesToDraw(alts, c);

        if(bestAlt.size()>0)
            cpuChart.AddAlternativesToDraw(bestAlt, bestAlternativeColor);

            cpuChart.DrawChart();
    }
    private void DrawAllAlternatives(){
        Alternative A;
        cpuChart.SetAlternativesToDraw(new ArrayList<Alternative>(), Color.red);    //erasing all
        for(int j=0;j<jobs.size();j++){
            UserJob J = jobs.get(j);                                //one job
            ArrayList<Alternative> alts = new ArrayList<Alternative>();
            for(int i=0;i<J.alternatives.size();i++){
                        A = J.alternatives.get(i);
                        alts.add(A);
            }
            Color c = colorMap.get((Object)J);
            cpuChart.AddAlternativesToDraw(alts, c);                           //one job's alternatives in the same color
        }
        cpuChart.DrawChart();
    }

    private void DrawAllBestAlternatives(){
        Alternative A;
        cpuChart.SetAlternativesToDraw(new ArrayList<Alternative>(), Color.red);    //erasing all
        for(int j=0;j<jobs.size();j++){
            UserJob J = jobs.get(j);                                //one job
            ArrayList<Alternative> alts = new ArrayList<Alternative>();
            if(J.bestAlternative>=0)
                alts.add(J.getAlternative(J.bestAlternative));
            Color c = colorMap.get((Object)J);
            cpuChart.AddAlternativesToDraw(alts, c);                           //one job's alternatives in the same color
        }
        cpuChart.DrawChart();
    }
    
    protected void reloadSchedulingState(){
        setModelTime(manualProcessor.getModelTime());
        SchedulerOperations.nameBatchAlternatives(manualProcessor.getBatch());
        
        SetEnvironment(manualProcessor.getEnvironment());
        LoadJobAlternatives(manualProcessor.getBatch(), true);
        
        
        jTextAreaData.setText(manualProcessor.getDebugData());
    }
    

    /** Creates new form cpuChartPanel */
    public ModelViewPanel() {
        initComponents();
        myInit();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroupAlts = new javax.swing.ButtonGroup();
        jScrollPaneJobs = new javax.swing.JScrollPane();
        jScrollPaneChart = new javax.swing.JScrollPane();
        jTextFieldMouseTime = new javax.swing.JTextField();
        jButtonNextEvent = new javax.swing.JButton();
        jRadioButtonAll = new javax.swing.JRadioButton();
        jRadioButtonJobAll = new javax.swing.JRadioButton();
        jRadioButtonOne = new javax.swing.JRadioButton();
        jRadioButtonNone = new javax.swing.JRadioButton();
        jLabel1 = new javax.swing.JLabel();
        jScrollPaneAltsList = new javax.swing.JScrollPane();
        jScrollPane1 = new javax.swing.JScrollPane();
        jListJobAlternatives = new javax.swing.JList();
        jRadioButtonDrawBest = new javax.swing.JRadioButton();
        jLabelCriteria = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTextAreaData = new javax.swing.JTextArea();

        jScrollPaneJobs.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jScrollPaneJobsMouseClicked(evt);
            }
        });

        jScrollPaneChart.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                jScrollPaneChartMouseMoved(evt);
            }
        });

        jButtonNextEvent.setText("Next Event");
        jButtonNextEvent.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonNextEventActionPerformed(evt);
            }
        });

        jRadioButtonAll.setText("Draw all alternatives");
        jRadioButtonAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadioButtonAllActionPerformed(evt);
            }
        });

        jRadioButtonJobAll.setText("Draw Job Alternatives");
        jRadioButtonJobAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadioButtonJobAllActionPerformed(evt);
            }
        });

        jRadioButtonOne.setText("Draw Alternative Chosen");
        jRadioButtonOne.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadioButtonOneActionPerformed(evt);
            }
        });

        jRadioButtonNone.setSelected(true);
        jRadioButtonNone.setText("Draw None");
        jRadioButtonNone.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadioButtonNoneActionPerformed(evt);
            }
        });

        jLabel1.setFont(new java.awt.Font("Tahoma", 1, 13)); // NOI18N
        jLabel1.setText("Job Alternatives");

        jListJobAlternatives.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        jListJobAlternatives.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jListJobAlternatives.setMaximumSize(new java.awt.Dimension(100, 100));
        jListJobAlternatives.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                AlternativeCLicked(evt);
            }
        });
        jScrollPane1.setViewportView(jListJobAlternatives);

        jScrollPaneAltsList.setViewportView(jScrollPane1);

        jRadioButtonDrawBest.setText("Draw Best Alternatives");
        jRadioButtonDrawBest.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadioButtonDrawBestActionPerformed(evt);
            }
        });

        jLabelCriteria.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        jLabelCriteria.setText("Criteria: ");

        jTextAreaData.setColumns(20);
        jTextAreaData.setRows(5);
        jScrollPane2.setViewportView(jTextAreaData);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jButtonNextEvent, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jTextFieldMouseTime, javax.swing.GroupLayout.Alignment.TRAILING))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 275, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPaneAltsList, javax.swing.GroupLayout.PREFERRED_SIZE, 144, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(layout.createSequentialGroup()
                                .addGap(25, 25, 25)
                                .addComponent(jLabel1)))
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabelCriteria)
                            .addComponent(jRadioButtonNone)
                            .addComponent(jRadioButtonDrawBest)
                            .addComponent(jRadioButtonOne)
                            .addComponent(jRadioButtonJobAll)
                            .addComponent(jRadioButtonAll)))
                    .addComponent(jScrollPaneJobs, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 682, Short.MAX_VALUE)
                    .addComponent(jScrollPaneChart, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 682, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPaneJobs, javax.swing.GroupLayout.PREFERRED_SIZE, 136, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPaneChart, javax.swing.GroupLayout.DEFAULT_SIZE, 142, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jButtonNextEvent)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextFieldMouseTime, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel1)
                            .addComponent(jLabelCriteria))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jRadioButtonAll)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jRadioButtonJobAll)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jRadioButtonOne)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jRadioButtonDrawBest)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jRadioButtonNone))
                            .addComponent(jScrollPaneAltsList, javax.swing.GroupLayout.PREFERRED_SIZE, 135, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 135, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(9, 9, 9)))
                .addGap(13, 13, 13))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void jScrollPaneChartMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jScrollPaneChartMouseMoved
        int x = evt.getX();
        int y = evt.getY();
        JScrollBar jsb = jScrollPaneChart.getHorizontalScrollBar();
        int f = jsb.getValue();
        f += x - cpuChart.cpuinfolength - 1;
        int cp = (y - 3) / (cpuChart.rowheight);
        int foundtask = 1;//chart.MousePoint(f, y);
        String info = "Time: " + f + " CPU" + (cp+1);
        jTextFieldMouseTime.setText(info);
    }//GEN-LAST:event_jScrollPaneChartMouseMoved

    private void jButtonNextEventActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonNextEventActionPerformed
        manualProcessor.next();
        reloadSchedulingState();
    }//GEN-LAST:event_jButtonNextEventActionPerformed

    private void jScrollPaneJobsMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jScrollPaneJobsMouseClicked
       int x = evt.getX();
       int y = evt.getY();
       JScrollBar jsb = jScrollPaneJobs.getHorizontalScrollBar();
       int f = jsb.getValue();
       x+=f;
       SelectJob(x,y);
       DrawAlternatives();
    }//GEN-LAST:event_jScrollPaneJobsMouseClicked

    private void jRadioButtonAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRadioButtonAllActionPerformed
        altDrawingMode = 3;
        DrawAlternatives();
    }//GEN-LAST:event_jRadioButtonAllActionPerformed

    private void jRadioButtonNoneActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRadioButtonNoneActionPerformed
        altDrawingMode = 0;
        DrawAlternatives();
    }//GEN-LAST:event_jRadioButtonNoneActionPerformed

    private void jRadioButtonOneActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRadioButtonOneActionPerformed
        altDrawingMode = 1;
        DrawAlternatives();
    }//GEN-LAST:event_jRadioButtonOneActionPerformed

    private void jRadioButtonJobAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRadioButtonJobAllActionPerformed
        altDrawingMode = 2;
        DrawAlternatives();
    }//GEN-LAST:event_jRadioButtonJobAllActionPerformed

    private void jRadioButtonDrawBestActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRadioButtonDrawBestActionPerformed
        altDrawingMode = 4;
        DrawAlternatives();
    }//GEN-LAST:event_jRadioButtonDrawBestActionPerformed

    private void AlternativeCLicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_AlternativeCLicked
        DrawAlternatives();
    }//GEN-LAST:event_AlternativeCLicked


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup buttonGroupAlts;
    private javax.swing.JButton jButtonNextEvent;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabelCriteria;
    private javax.swing.JList jListJobAlternatives;
    private javax.swing.JRadioButton jRadioButtonAll;
    private javax.swing.JRadioButton jRadioButtonDrawBest;
    private javax.swing.JRadioButton jRadioButtonJobAll;
    private javax.swing.JRadioButton jRadioButtonNone;
    private javax.swing.JRadioButton jRadioButtonOne;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPaneAltsList;
    private javax.swing.JScrollPane jScrollPaneChart;
    private javax.swing.JScrollPane jScrollPaneJobs;
    private javax.swing.JTextArea jTextAreaData;
    private javax.swing.JTextField jTextFieldMouseTime;
    // End of variables declaration//GEN-END:variables

    /**
     * @param manualProcessor the manualProcessor to set
     */
    public void setManualProcessor(DynamicProcessorHelper manualProcessor) {
        this.manualProcessor = manualProcessor;
        reloadSchedulingState();
    }

}
