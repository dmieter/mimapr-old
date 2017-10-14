
package project.engine.alternativeStats;

/**
 *
 * @author emelyanov
 */
public class AverageDouble {
    protected Double value;
    protected int hits;
    
    public AverageDouble(){
        init();
    }
    
    public AverageDouble(Double value){
        hits = 1;
        this.value = value;
    }
    
    protected void init(){
        hits = 0;
        value = 0d;
    }
    
    public void putValue(Double nextValue){
        if(hits == 0){
            value = nextValue;
        }else{
            value = (value*hits + nextValue)/(hits + 1);
        }
        hits++;
    }
    
    public String toString(){
        return value + " over "+hits+" values";
    }
    
    public void reset(){
        init();
    }
}
