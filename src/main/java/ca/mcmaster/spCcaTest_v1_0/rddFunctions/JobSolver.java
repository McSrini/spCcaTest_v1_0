/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spCcaTest_v1_0.rddFunctions;
 
import static ca.mcmaster.spCcaTest_v1_0.ConstantsAndParameters.ZERO;
import ca.mcmaster.spCcaTest_v1_0.cplex.ActiveSubtreeCollection;
import ca.mcmaster.spCcaTest_v1_0.cplex.NodeSelectionStartegyEnum;
import org.apache.spark.api.java.function.Function;

/**
 *
 * @author tamvadss
 * solvers work assigned to a partition using cplex
 * solving strategies could be cb, or sbf, or bef
 *  
 * returns whether ASTC is completely solved
 * 
 */
public class JobSolver implements Function <ActiveSubtreeCollection, ResultOfPartitionSolve>{
    
    private Double cutoff;
    private  double solutionCycleTimeMinutes;
    private    double timeSlicePerTreeInMInutes ;
    private             NodeSelectionStartegyEnum nodeSelectionStartegy ;
    
    
    public JobSolver (Double cutoff,  double solutionCycleTimeMinutes,   double timeSlicePerTreeInMInutes ,  
            NodeSelectionStartegyEnum nodeSelectionStartegy  ){
        
        this.cutoff=cutoff;
        this.solutionCycleTimeMinutes=solutionCycleTimeMinutes;
        this.nodeSelectionStartegy=nodeSelectionStartegy;
        this.timeSlicePerTreeInMInutes=timeSlicePerTreeInMInutes;              
    }
 
    public ResultOfPartitionSolve   call(ActiveSubtreeCollection astc) throws Exception {
        
        astc.setCutoff( cutoff);
        
        ResultOfPartitionSolve result = new ResultOfPartitionSolve();
        
        if (ZERO!=astc.getNumTrees()+astc.getPendingRawNodeCount()){
            astc.solve( solutionCycleTimeMinutes,     timeSlicePerTreeInMInutes , nodeSelectionStartegy  );              
        }
        
        result.isComplete= (ZERO==astc.getNumTrees()+astc.getPendingRawNodeCount());
        result.bestKnownSolution=astc.getIncumbentValue();
        
        return result;
    }
    
}
