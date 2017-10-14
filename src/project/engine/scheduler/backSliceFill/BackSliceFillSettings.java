/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package project.engine.scheduler.backSliceFill;

import project.engine.scheduler.SchedulerSettings;
import project.engine.scheduler.backFill.BackfillSettings;
import project.engine.scheduler.batchSlicer.BatchSlicerSettings;

/**
 *
 * @author emelyanov
 */
public class BackSliceFillSettings  extends SchedulerSettings {

    public double slicerQuotient = 0;           //must be in [0;1] from full backfilling -> to full batch slicing

    public BackfillSettings bfs;
    public BatchSlicerSettings bss;
    
    @Override
    public void setSchedulingInterval(int startInterval, int endInterval) {
        super.setSchedulingInterval(startInterval, endInterval);
        bss.setSchedulingInterval(startInterval, endInterval);
        bfs.setSchedulingInterval(startInterval, endInterval);
    }
}
