package project.engine.scheduler.alternativeSolver.v1;

import project.engine.scheduler.alternativeSolver.v1.data.AltTable;
import project.engine.data.ResourceRequest;
import project.engine.scheduler.alternativeSolver.v1.data.ParetoTable;

import java.util.ArrayList;
import java.util.HashMap;
import project.engine.data.UserJob;

/**
 * Created by IntelliJ IDEA.
 * User: Rookie
 * Date: 27.04.2010
 * Time: 23:21:15
 * To change this template use File | Settings | File Templates.
 */
public class AlternativeSolverResult
{
    public boolean valid=true;
  //  public double totalCash = 0;
    public double averageCash = 0;
   // public double totalTime = 0;
    public double averageTime = 0;
    public AlternativeSolverSettings settings;
    public double limit = 0;
    public double budgetLimit = 0;
    public double timeLimit = 0;
    public long maximumCombinations = 1;
    public int requestsWithAlternatives = 0;

    public HashMap<Integer, Integer> requestIndexes;
    public ArrayList<AltDistrib> condOptimalVariants = new ArrayList<AltDistrib>();
    public ArrayList<AltTable> tables;
    public ArrayList<UserJob> jobs;
    public ParetoTable paretoTable;

    public AlternativeSolverResult()
    {
        
    }

    public AlternativeSolverResult(boolean valid)
    {
        this.valid = valid;
    }

    public void createIndexes()
    {
        requestIndexes = new HashMap<Integer, Integer>();
        for (int i=0; i<jobs.size(); i++)
        {
            requestIndexes.put(jobs.get(i).id, i);
        }
    }

    private AltDistrib findVarById(int id)
    {
        for (int i=0; i<condOptimalVariants.size(); i++)
        {
            if (condOptimalVariants.get(i).id == id)
                return condOptimalVariants.get(i);
        }
        return null;
    }

    public String debugInfo()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("===== ALTERNATIVE SOLVER RESULT ===== \n\n");
        sb.append("Total requests distributed: "+jobs.size()+"\n\n");
        sb.append("$$$$ Optimal distribution $$$$\n\n");
        AltDistrib var = findOptimalDistribution();
        if (var == null)
        sb.append("\nNo variant found\n");
        else
        sb.append(var.debugInfoShort());
        if (settings.usePareto)
        {
            ArrayList<AltDistrib> varPs = findParetoOptimalDistributions();
            sb.append("\n$$$$ Pareto-Optimal distributions ("+varPs.size()+" total) $$$$\n\n");
            //for (altDistrib varP: varPs)
            //sb.append(varP.debugInfoShort());
        }
        //sb.append("\nMaximum alternative combinations: "+maximumCombinations+"\n");
        if (!settings.optimalOnly)
        {
            if (settings.usePareto)
            {
                sb.append("\n\n$$$$ Conditional-Optimal distributions ("+ condOptimalVariants.size() + " total) $$$$\n\n");
                try
                {
                    for (int i=0; i<paretoTable.size; i++)
                    {
                        sb.append(findVarById(paretoTable.VarIds[i]).debugInfoShort());
                    }
                }
                catch (Exception e)
                {
                    sb.append("<String too long>\n");
                    return sb.toString();
                }
            }
            else
            {
                sb.append("\n\n$$$$ Conditional-Optimal distributions ("+condOptimalVariants.size()+" total) $$$$\n\n");
                try
                {
                    for (AltDistrib varr : condOptimalVariants)
                    {
                        sb.append(varr.debugInfoShort());
                    }
                }
                catch (Exception e)
                {
                    sb.append("<String too long>\n");
                    return sb.toString();
                }
            }
        }
        return sb.toString();
    }

    public ArrayList<AltDistrib> findParetoOptimalDistributions()
    {
        ArrayList<AltDistrib> list = new ArrayList<AltDistrib>();
        for (int i=0; i<paretoTable.size; i++)
        {
            if (paretoTable.optimal[i])
                list.add(findVarById(paretoTable.VarIds[i]));
        }
        return list;
    }

    public AltDistrib findOptimalDistribution()
    {
        if (condOptimalVariants.size()==1)
            return condOptimalVariants.get(0);
        AltDistrib optVar = condOptimalVariants.get(0);
        for (AltDistrib var: condOptimalVariants)
        {
            if (settings.optimizedVar == 1) //COST
            {
                if (settings.optType.equals("MIN"))
                {
                    if (getTotalCash(var) < getTotalCash(optVar))
                        optVar = var;
                }
                if (settings.optType.equals("MAX"))
                {
                     if (getTotalCash(var) > getTotalCash(optVar))
                        optVar = var;
                }
            }
            if (settings.optimizedVar == 0)  //TIME
            {
                if (settings.optType.equals("MIN"))
                {
                    if (getTotalTime(var) < getTotalTime(optVar))
                        optVar = var;
                }
                if (settings.optType.equals("MAX"))
                {
                    if (getTotalTime(var) > getTotalTime(optVar))
                        optVar = var;
                }
            }
        }
        return optVar;
    }

    public AltDistrib getFirstValidVariant()
    {
        for (AltDistrib var: condOptimalVariants)
        {
            if (var.isAllRequests())
                return var;
        }
        return null;
    }


    //������-�������� C(S)
    public double getTotalCash()
    {
        double sum = 0;
        for (UserJob job: jobs)
        {
            sum += job.getBestCost();
        }
        return sum;
    }

    public double getTotalCash(AltDistrib var)
    {
        double sum = 0;
        for (UserJob job: jobs)
        {
            int num = var.get(job.id);
            if (num > -1)
            sum+= job.getAlternative(num).getCost();
        }
        return sum;
    }

    //������-�������� D(S)
    public double getLostProfit()
    {
        return budgetLimit - getTotalCash();
    }

    public double getLostProfit(AltDistrib var)
    {
        return budgetLimit - getTotalCash(var);
    }


    //������-�������� T(S)
    public double getTotalTime()
    {
        double sum = 0;
        for (UserJob job: jobs)
        {
            sum += job.getBestTime();
        }
        return sum;
    }

    public double getTotalTime(AltDistrib var)
    {
        double sum = 0;
        for (UserJob job : jobs)
        {
            int num = var.get(job.id);
            if (num > -1)
            sum += job.getAlternative(var.get(job.id)).getLength();
        }
        return sum;
    }

    //������-�������� I(S)
    public double getIdleTime()
    {
        return timeLimit - getTotalTime();
    }

    public double getIdleTime(AltDistrib var)
    {
        return timeLimit - getTotalTime(var);
    }

   
    public String getVariantsTotal()
    {
        if (condOptimalVariants.size() > settings.varMax)
            return ">" + settings.varMax;
        return String.valueOf(condOptimalVariants.size());
    }
}
