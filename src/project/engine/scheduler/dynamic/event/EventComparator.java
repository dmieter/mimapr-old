
package project.engine.scheduler.dynamic.event;

import java.util.Comparator;

/**
 *
 * @author magica
 */
public class EventComparator implements Comparator<Event> {

    public int compare(Event first, Event second) {
        return new Integer(first.getTime()).compareTo(new Integer(second.getTime()));
    }  
}
