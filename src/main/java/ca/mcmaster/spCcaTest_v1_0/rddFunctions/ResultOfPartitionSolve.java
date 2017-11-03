/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spCcaTest_v1_0.rddFunctions;

import static ca.mcmaster.spCcaTest_v1_0.ConstantsAndParameters.*;
import java.io.Serializable;

/**
 *
 * @author tamvadss
 * 
 * return value of solving a partition
 * 
 */
public class ResultOfPartitionSolve implements Serializable {
    public Boolean isComplete = false;
    public Double bestKnownSolution = DOUBLE_ZERO + (IS_MAXIMIZATION ? MINUS_INFINITY: PLUS_INFINITY);
    /*public ResultOfPartitionSolve (Boolean isComplete,      Double localIncumbent){
        this.isComplete=isComplete;
        this.bestKnownSolution=localIncumbent;
    }*/
    public ResultOfPartitionSolve (){
        
    }
}
