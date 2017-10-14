
package project.engine.scheduler.dynamic.event;


/**
 *
 * @author Emelyanov
 */
public class Event implements Comparable {
    protected Integer time;
    
    public Event(int time){
        this.time = time;
    }
    
    public void perform(){
        
    }
    
    public int getTime(){
        return time;
    }

    @Override
    public int compareTo(Object o) {
        if(o instanceof Event){
            Event event = (Event)o;
            return time.compareTo(event.getTime());
        }else{
            return 0;
        }
    }
}
