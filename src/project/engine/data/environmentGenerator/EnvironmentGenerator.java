package project.engine.data.environmentGenerator;

import project.engine.data.ResourceLine;
import project.engine.data.Resource;
import project.engine.data.VOEnvironment;

import java.util.ArrayList;
import java.util.Random;
import project.math.utils.MathUtils;

/**
 * Created by IntelliJ IDEA.
 * User: Rookie
 * Date: 30.03.2010
 * Time: 23:52:03
 * To change this template use File | Settings | File Templates.
 */
public class EnvironmentGenerator
{
    public String localTaskName = "task";//"local task";
    public VOEnvironment generate(EnvironmentGeneratorSettings settings, ArrayList<Resource> resourceTypes)
    {
        if (resourceTypes == null)
        {
            resourceTypes = generateResourceTypes(settings);
        }

        VOEnvironment env = new VOEnvironment();
        env.resourceLines = new ArrayList<ResourceLine>();
        for (Resource res: resourceTypes)
        {
            double occupancyLevel = settings.occupancyLevel;
            if(settings.occupGenerator!=null){
                occupancyLevel = settings.occupGenerator.getRandom();   // different occupancy for each line
            }
            ResourceLine rLine = new ResourceLine(res);
            if(res.index==0){        //if in the init state
                rLine.id = (new Random()).nextInt();
            }
            else{ 
                rLine.id = res.index;
            }
            rLine.environment = env;
            int failure_counter;
            //dumb algorithm
            if (occupancyLevel > 0)
            {
            do
            {
                double taskStart;
                double taskLength;
                failure_counter = 0;
                do
                {
                   taskStart = (int)MathUtils.getUniform(0, settings.timeInterval);      //unco 04.05.2010
                   taskLength = (int)MathUtils.getGaussian(settings.minTaskLength, settings.maxTaskLength, (settings.minTaskLength + settings.maxTaskLength)/2);
                   failure_counter++;
                }
                while ((rLine.isReserved(taskStart, taskStart+taskLength) ||
                       taskStart + taskLength > settings.timeInterval)
                        && failure_counter < 100000);
                if (failure_counter < 100000){
                    rLine.AddTask(localTaskName, taskStart, taskStart + taskLength);
                }
            }
            while (rLine.getTotalOccupancyTime()/settings.timeInterval*10 < occupancyLevel && failure_counter < 100000);
            }
            env.resourceLines.add(rLine);
        }
        return env;
    }



    public ArrayList<Resource> generateResourceTypes(EnvironmentGeneratorSettings settings)
    {
        ArrayList<Resource> resList = new ArrayList<Resource>();
        for (int i=0; i<settings.resourceLineNum; i++)
        {
            double sp =0;
            if(settings.perfGenerator!=null){
                sp = settings.perfGenerator.getRandomInteger();
            }else if (settings.minResourceSpeed != -1 && settings.maxResourceSpeed != -1)
                sp = MathUtils.getUniform(settings.minResourceSpeed, settings.maxResourceSpeed);
            else
                sp = 10;
            String name = String.valueOf((int)sp);
            Resource r = new Resource("res"+i+"_"+name, (int)sp);
            resList.add(r);
        }
        return resList;        
    }
}
