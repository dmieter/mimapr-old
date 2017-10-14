/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package project.engine.alternativeStats;

import java.util.HashMap;
import project.math.distributions.DistributionGenerator;

/**
 *
 * @author emelyanov
 */
public class DistributionStats {
    public int expNum = 0;
    public HashMap<Integer, Integer> values = new HashMap<Integer, Integer>();

    public void clearStats(){
        expNum = 0;
        values = new HashMap<Integer, Integer>();
    }

    public void testGenerator(int expNum, DistributionGenerator gen){
        for(int i=0;i<expNum;i++){
            int value = gen.getRandomInteger();
            simpleProcessValue(value);
        }
    }

    public void simpleProcessValue(int nextValue){
        expNum++;
        if(values.containsKey(nextValue)){
            int num = values.get(nextValue);
            values.put(nextValue, ++num);
        }else{
            values.put(nextValue, 1);
        }
    }
}