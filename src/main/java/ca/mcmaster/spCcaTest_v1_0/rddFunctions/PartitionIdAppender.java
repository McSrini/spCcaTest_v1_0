/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spCcaTest_v1_0.rddFunctions;
  
import static ca.mcmaster.spCcaTest_v1_0.ConstantsAndParameters.ZERO;
import ca.mcmaster.spCcaTest_v1_0.cca.CCANode;
import java.util.List;
import org.apache.spark.api.java.function.PairFunction;
import scala.Tuple2;

/**
 *
 * @author tamvadss
 * 
 * used to create a pair RDD by extracting out the id
 * 
 */
public class PartitionIdAppender implements PairFunction <List<CCANode>, Integer, List<CCANode>>{
 
    public Tuple2<Integer, List<CCANode>> call(List<CCANode> wa) throws Exception {
        return new Tuple2<Integer, List<CCANode>> (/*unused id*/ ZERO, wa) ;
    }
 
     
    
}
