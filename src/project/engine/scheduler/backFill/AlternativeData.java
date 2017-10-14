/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package project.engine.scheduler.backFill;

/**
 *
 * @author Stanislav
 */
public class AlternativeData {
    //id задания
    public int jobId; 
    //номер альтернативы данного задания, порядковый
    public int altId;
    //количество процессоров
    public int width = 0;
    //длина
    public double length = 0;
    //площадь
    public double totalLength = 0;
    //стоимость
    public double cost = 0;
    
}
//этот класс используется при политике GREEDY - из объектов данного класса составляются наборы альтернатив, которые затем анализируются на соответствие метрике
