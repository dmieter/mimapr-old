package project.engine.slot.slotProcessor;

import project.engine.data.ResourceLine;

import java.util.ArrayList;
import project.engine.slot.slotProcessor.criteriaHelpers.ICriteriaHelper;

/**
 * Created by IntelliJ IDEA.
 * User: Rookie
 * Date: 22.03.2010
 * Time: 0:59:23
 * To change this template use File | Settings | File Templates.
 */
public class SlotProcessorSettings {
    //cycle start moment on absolute time axis
    public int cycleStart;
    //cycle length
    public int cycleLength;
    //algoritm to use NORMAL(limit on slots price)/MODIFIED(limit on the whole job exection cost)
    public String algorithmType = "MODIFIED";
    public static final String TYPE_NORMAL = "NORMAL";
    public static final String TYPE_MODIFIED = "MODIFIED";
    
    //algoritm concept to use COMMON(first fit)/EXTREME(according to user criteria)
    public String algorithmConcept = "COMMON";
    public static final String CONCEPT_COMMON = "COMMON";
    public static final String CONCEPT_EXTREME = "EXTREME";
    
    
    //whether to clean identical alternatives
    public boolean clean = true;
    //whether to count slotProcessor alternative statistics
    public boolean countStats = false;
    //if algorithm will search for windows with parameters different from already founded ones
    public boolean check4PreviousAlternatives = false;
    //minimum relative "distance" between acceptable alternatives
    public double alternativesMinDistance = 0;
    //option to find all (possibly intersecting) alternatives for job. Works for EXTREME algorithm only!
    public boolean findAllPossibleAlternativesForJob = false;
    //class
    public ICriteriaHelper criteriaHelper;      //General criteria (RR criterias have more priority)
}
