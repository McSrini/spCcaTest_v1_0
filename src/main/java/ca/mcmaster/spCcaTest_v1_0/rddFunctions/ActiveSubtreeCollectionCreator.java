/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spCcaTest_v1_0.rddFunctions;
 
import static ca.mcmaster.spCcaTest_v1_0.ConstantsAndParameters.ZERO;
import ca.mcmaster.spCcaTest_v1_0.cca.CCANode;
import ca.mcmaster.spCcaTest_v1_0.cplex.ActiveSubtreeCollection;
import ca.mcmaster.spCcaTest_v1_0.cplex.datatypes.BranchingInstruction;
import java.util.*;
import org.apache.spark.api.java.function.Function;

/**
 *
 * @author tamvadss
 * 
 * takes a cca node and an optional cb intsruction pair 
 * and creates a active subtree collection out of it
 * 
 */
public class ActiveSubtreeCollectionCreator implements Function <List<CCANode>, ActiveSubtreeCollection>{
    
    private  double cutoff;
    private boolean useCutoff;
    
    public ActiveSubtreeCollectionCreator(  double cutoff, boolean useCutoff) {
         this.useCutoff=   useCutoff;
        this. cutoff= cutoff;
    }
 
    public ActiveSubtreeCollection call(List<CCANode> wa) throws Exception {
                
        return new   ActiveSubtreeCollection (wa, new ArrayList<BranchingInstruction> (),   cutoff,   useCutoff,  /*unused id*/ ZERO)  ;
         
    }
 
    
    
}
