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
public class ActiveSubtreeCollectionEnder implements Function <ActiveSubtreeCollection, ActiveSubtreeCollection>{
    
    
 
    public ActiveSubtreeCollection call(ActiveSubtreeCollection wa) throws Exception {
                
       wa.endAll();
       return wa;
         
    }
 
    
    
}
