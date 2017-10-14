package project.engine.data;

import java.io.Serializable;
import project.engine.data.Resource;

/**
 * Created by IntelliJ IDEA.
 * User: unco
 * Date: 22.03.2009
 * Time: 20:50:41
 * To change this template use File | Settings | File Templates.
 */

public class DistributedTask implements Serializable {
    public double startTime = 0;
    public double endTime = 0;
    public Resource cpu;
    public String taskName = "";
          

    public DistributedTask(String n, double start, double end) {
        taskName = n;
        startTime = start;
        endTime = end;
        cpu = null;
    }
     public DistributedTask(){
          startTime = 0;
          endTime = 0;
          taskName = "";
     }

    @Override
     public DistributedTask clone(){
         DistributedTask newDt = new DistributedTask(taskName, startTime, endTime);
         newDt.cpu = cpu;
         return newDt;
     }

}
