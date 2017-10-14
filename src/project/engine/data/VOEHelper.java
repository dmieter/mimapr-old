/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package project.engine.data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

/**
 *
 * @author Magica
 */
public class VOEHelper {

    //adds alternative as a tasks to the environment
    public static void addAlternativeToVOE(VOEnvironment voe, Alternative a, String name) {
        for (Slot s : a.window.slots) {
            addSlotToVOE(voe, s, name);
        }
    }

    private static void addSlotToVOE(VOEnvironment voe, Slot s, String name) {
        ResourceLine rl;
        for (int i = 0; i < voe.resourceLines.size(); i++) {
            rl = voe.resourceLines.get(i);
            if (rl.id == s.resourceLine.id) {               //found cpu with the same id
                rl.AddTask(name, s.start, s.end);
                break;
            }
        }
    }

    public static ArrayList<Slot> getSlotsFromVOE(VOEnvironment voe, double start, double end) {
        VOEnvironment copyEnv = VOEHelper.copyEnvironment(voe);     //working with copy
        VOEHelper.updateSlots(copyEnv);
        VOEHelper.trimSlots(copyEnv, start, end);                   //trimming to the needed interval
        //ArrayList<Slot> sortedSlots = new ArrayList<Slot>();
        ArrayList<ArrayList<Slot>> material = new ArrayList<ArrayList<Slot>>();
        for (Iterator it = copyEnv.resourceLines.iterator(); it.hasNext();) {
            ResourceLine rl = (ResourceLine) it.next();
            ArrayList<Slot> sline = null;
            if (rl.slots != null) {
                sline = copySlotList(rl.slots);
                material.add(sline);
            }
        }
        return getSortedSlotsFromMaterial(material);
    }

    private static ArrayList<Slot> getSortedSlotsFromMaterial(ArrayList<ArrayList<Slot>> material) {
        ArrayList<Slot> sortedSlots = new ArrayList<Slot>();
        boolean slotExist = true;
        while (slotExist) {
            slotExist = false;
            int minNum = -1;
            double minValue = Double.MAX_VALUE;
            for (int i = 0; i < material.size(); i++) {
                ArrayList<Slot> rl = material.get(i);   //next slot line
                if (rl.size() > 0) {                        //if it has slots
                    slotExist = true;
                    if (rl.get(0).start < minValue) {
                        minValue = rl.get(0).start;
                        minNum = i;
                    }
                }
            }
            if (minNum != -1) {
                ArrayList<Slot> rl = material.get(minNum);
                Slot s = rl.get(0).clone();
                sortedSlots.add(s);
                rl.remove(0);
            }
        }
        return sortedSlots;
    }

    public static void trimSlots(VOEnvironment voe, double start, double end) {      //trims slot lists to the borders (works best after update slots or same kind)
        for (ResourceLine rl : voe.resourceLines) {
            trimResourceLineSlots(rl, start, end);
        }
    }

    public static void updateSlots(VOEnvironment voe) {          //creates new dull slot lists
        for (ResourceLine rl : voe.resourceLines) {
            updateResourceLineSlots(rl);
        }
    }

    private static void updateResourceLineSlots(ResourceLine rl) {
        rl.slots = new ArrayList<Slot>();   //clear old data
        DistributedTask t1, t2;
        Slot s;
        if (rl.tasks.size() >= 1) {
            t1 = rl.getTask(0);                    //1st slot - before first task
            s = new Slot(0, t1.startTime, rl);
            rl.slots.add(s);                     //adding
            if (rl.tasks.size() > 1) {               //then we have to find slots between tasks
                for (int j = 0; j < rl.tasks.size() - 1; j++) {    //through all neighbour tasks inside cpu(pairs of tasks)
                    t1 = rl.getTask(j);
                    t2 = rl.getTask(j + 1);
                    s = new Slot(t1.endTime, t2.startTime, rl);      //slot between tasks
                    rl.slots.add(s);             //adding
                }
            }
            t2 = rl.getTask(rl.tasks.size() - 1);       //last task - to get last infinite slot
            s = new Slot(t2.endTime, Double.POSITIVE_INFINITY, rl);
            rl.slots.add(s);                     //adding
        } else {                                      //only one task
            s = new Slot(0, Double.POSITIVE_INFINITY, rl);
            rl.slots.add(s);                     //only one slot in cpuslot - it's infinite
        }
        cleanZeroSlots(rl.slots);
    }

    private static void cleanZeroSlots(ArrayList<Slot> slots) {
        for (Iterator it = slots.iterator(); it.hasNext();) {
            Slot s = (Slot) it.next();
            if ((s.end - s.start) <= 0) {
                it.remove();
            }
        }
    }

    private static void trimResourceLineSlots(ResourceLine rl, double start, double end) {
        for (Iterator it = rl.slots.iterator(); it.hasNext();) {
            Slot s = (Slot) it.next();
            if (s.start > end) {
                it.remove();
                continue;
            }
            if (s.end < start) {
                it.remove();
                continue;
            }
            if ((s.start < start) && (s.end >= start)) {
                s.start = start;
            }
            if ((s.start <= end) && (s.end > end)) {
                s.end = end;
            }
        }

    }

    public static VOEnvironment copyEnvironment(VOEnvironment etalon) {
        if (etalon == null) {
            return null;
        }

        VOEnvironment newVOE = new VOEnvironment();
        newVOE.resourceLines = new ArrayList<ResourceLine>();
        for (Iterator it = etalon.resourceLines.iterator(); it.hasNext();) {
            ResourceLine rl = new ResourceLine((ResourceLine) it.next());
            rl.environment = newVOE;
            newVOE.resourceLines.add(rl);
        }
        if (etalon.pcSettings != null) {
            newVOE.pcSettings = etalon.pcSettings.clone();
        }
        return newVOE;
    }

    public static ArrayList<Slot> copySlotList(ArrayList<Slot> etalon) {
        ArrayList<Slot> out = new ArrayList<Slot>();
        for (Iterator it = etalon.iterator(); it.hasNext();) {
            Slot s = (Slot) it.next();
            out.add(s.clone());
        }
        return out;
    }

    public static ArrayList<UserJob> copyJobBatchList(ArrayList<UserJob> etalon) {
        ArrayList<UserJob> out = new ArrayList<UserJob>();
        for (Iterator<UserJob> it = etalon.iterator(); it.hasNext();) {
            UserJob job = it.next();
            out.add(job.clone());
        }
        return out;
    }

    public static ArrayList<Slot> getSlotsFromVOE(VOEnvironment voe, double start, double end, Alternative a) {
        VOEnvironment copyEnv = VOEHelper.copyEnvironment(voe);     //working with copy
        VOEHelper.updateSlots(copyEnv);
        VOEHelper.trimSlots(copyEnv, start, end);                   //trimming to the needed interval

        HashSet<Integer> set = new HashSet<Integer>();              //Storing ids used in Alternative
        for (Iterator it = a.window.slots.iterator(); it.hasNext();) {
            Slot s = (Slot) it.next();
            set.add(s.resourceLine.id);
        }

        ArrayList<ArrayList<Slot>> material = new ArrayList<ArrayList<Slot>>();
        for (Iterator it = copyEnv.resourceLines.iterator(); it.hasNext();) {
            ResourceLine rl = (ResourceLine) it.next();
            if (!set.contains(rl.id)) {
                continue;
            }
            ArrayList<Slot> sline = null;
            if (rl.slots != null) {
                sline = copySlotList(rl.slots);
                material.add(sline);
            }
        }
        return getSortedSlotsFromMaterial(material);
    }

    public static void applyBestAlternativesToVOE(ArrayList<UserJob> subBatch, VOEnvironment env) {
        for (UserJob job : subBatch) {
            applyBestAlternativeToVOE(job, env);
        }
    }

    public static void applyBestAlternativeToVOE(UserJob job, VOEnvironment env) {

        if (!job.alternatives.isEmpty() && job.bestAlternative >= 0) {         //if we found a best alternative, by default it -1
            addAlternativeToVOE(env, job.alternatives.get(job.bestAlternative), "Job " + job.name);
        }

    }

    public static boolean checkBatchIntersectionsWithVOE(ArrayList<UserJob> batch, VOEnvironment env) {
        for (UserJob job : batch) {
            if (checkJobIntersectionsWithVOE(job, env)) {
                return true;
            }
        }
        return false;
    }

    public static boolean checkJobIntersectionsWithVOE(UserJob job, VOEnvironment env) {
        if (job.alternatives == null) {
            return false;
        }

        for (Alternative a : job.alternatives) {
            if (checkAlternativeIntersectionsWithVOE(a, env)) {
                return true;
            }
        }
        return false;
    }

    public static boolean checkAlternativeIntersectionsWithVOE(Alternative a, VOEnvironment env) {
        for (Slot s : a.window.slots) {
            if (checkSlotIntersectionWithVOE(s, env)) {
                return true;
            }
        }
        return false;
    }

    private static boolean checkSlotIntersectionWithVOE(Slot s, VOEnvironment env) {
        ResourceLine slotline = null;
        for (ResourceLine line : env.resourceLines) {
            if (line.id == s.resourceLine.id) {
                slotline = line;
            }
        }
        if (slotline == null) {
            return false;
        }

        for (DistributedTask t : slotline.tasks) {
            if (t.startTime < s.start && t.endTime > s.start || t.startTime < s.end && t.endTime > s.end
                    || s.start <= t.startTime && s.end >= t.endTime) {
                return true;
            }
        }
        return false;
    }
    
    public static ResourceLine getResourceLineByID(VOEnvironment voe, int resourceID){
        for(ResourceLine line : voe.resourceLines){
            if(line.id == resourceID){
                return line;
            }
        }
        
        return null;
    }
    
    public static boolean isJobRunsOnResource(UserJob job, ResourceLine resource, int time){
        if(job == null || job.bestAlternative == -1){
            return false; /* seems job isn't running */
        }
        
        for(Slot slot : job.getBestAlternative().window.slots){
            if(slot.resourceLine.id == resource.id && slot.start <= time && slot.end >= time){
                return true;
            }
        }
        
        return false;
    }
}
