package project.engine.slot.slotProcessor;

import project.engine.alternativeStats.AlternativeStats;

/**
 * Created by IntelliJ IDEA.
 * User: Rookie
 * Date: 16.05.2010
 * Time: 15:24:53
 * To change this template use File | Settings | File Templates.
 */
public class SlotProcessorResult
{
    public int alternativesFound;
    public int FindNextWindowCalls;
    public int slotsProcessed;
    public int alternativesCleaned;
    public AlternativeStats altStats;

    public String debugInfo()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("===== SLOT PROCESSOR RESULT =====\n\n");
        sb.append("Total slots processed: "+slotsProcessed+"\n");
        sb.append("Total FindNextWindow calls: "+FindNextWindowCalls + "\n");
        sb.append("Total alternatives found: "+ alternativesFound+"\n");
        sb.append("Identical alternatives cleaned: "+alternativesCleaned + "\n");
        return sb.toString();
    }
}
