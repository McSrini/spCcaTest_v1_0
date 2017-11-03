/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spCcaTest_v1_0;
  
import static ca.mcmaster.spCcaTest_v1_0.ConstantsAndParameters.*;
import ca.mcmaster.spCcaTest_v1_0.cb.*;
import ca.mcmaster.spCcaTest_v1_0.cca.CCANode;
import ca.mcmaster.spCcaTest_v1_0.cplex.*;
import ca.mcmaster.spCcaTest_v1_0.cplex.datatypes.*;
import ca.mcmaster.spCcaTest_v1_0.rddFunctions.ActiveSubtreeCollectionCreator;
import ca.mcmaster.spCcaTest_v1_0.rddFunctions.ActiveSubtreeCollectionEnder;
import ca.mcmaster.spCcaTest_v1_0.rddFunctions.JobSolver;
import ca.mcmaster.spCcaTest_v1_0.rddFunctions.PartitionIdAppender;
import ca.mcmaster.spCcaTest_v1_0.rddFunctions.ResultOfPartitionSolve;
import ilog.concert.IloException;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import static java.lang.System.exit;
import java.lang.management.*;
import java.net.InetAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import org.apache.log4j.*;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaSparkContext;
import scala.Tuple2;
  

/**
 *
 * @author tamvadss
 * 
 *   A simple spark driver for testing the CCA scheme
 *  Assumes that no leafs are left behind in the ramped up tree
 * 
 */
public class TestDriver_CCA_DynamicSearch_Spark {
    
    private static  Logger logger = null;
     
     
    public static void main(String[] args) throws Exception {
       
        if (! isLogFolderEmpty()) {
            System.err.println("\n\n\nClear the log folder before starting the test." + LOG_FOLDER);
            exit(ONE);
        }
            
        logger=Logger.getLogger(TestDriver_CCA_DynamicSearch_Spark.class);
        logger.setLevel(Level.DEBUG);
        
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            RollingFileAppender rfa = new  RollingFileAppender(layout,LOG_FOLDER+TestDriver_CCA_DynamicSearch_Spark.class.getSimpleName()+ LOG_FILE_EXTENSION);
           
            rfa.setMaxBackupIndex(TEN*TEN);
            logger.addAppender(rfa);
            logger.setAdditivity(false);
            
        } catch (Exception ex) {
            System.err.println("Exit: unable to initialize logging");        
            exit(1);
        }
         
        MPS_FILE_ON_DISK =   MIP_NAME_UNDER_TEST +".mps";  //linux
      
        logger.debug ("starting ramp up " + MPS_FILE_ON_DISK + " Solution cycle time " + SOLUTION_CYCLE_TIME_MINUTES+
                      " tree time slice " + TIME_SLICE_IN_MINUTES_PER_ACTIVE_SUBTREE + " partitions " + NUM_PARTITIONS+
                " on host " + InetAddress.getLocalHost().getHostName()) ;  
        ActiveSubtree activeSubtreeForRampUp = new ActiveSubtree () ;
        activeSubtreeForRampUp.solve( RAMP_UP_TO_THIS_MANY_LEAFS, PLUS_INFINITY, MILLION, true, false); 
        logger.debug ("Ramp up created " +activeSubtreeForRampUp.getActiveLeafCount() +" leafs.") ;
                              
        //find the best known solution after ramp up
        SolutionVector bestKnownSolutionAfterRampup  = null;
        double incumbentValueAfterRampup = IS_MAXIMIZATION ? MINUS_INFINITY : PLUS_INFINITY;
        if (activeSubtreeForRampUp.isFeasible()) {
            bestKnownSolutionAfterRampup =             activeSubtreeForRampUp.getSolutionVector();
            incumbentValueAfterRampup = activeSubtreeForRampUp.getObjectiveValue();
            logger.debug("best known solution after ramp up is "+ activeSubtreeForRampUp.getObjectiveValue()) ;
        } else {
            logger.debug("NO known solution after ramp up   " ) ;
        }
        
         
        logger.debug("getting CCA candidates ...") ;
        
        //get CCA condidates
        List<CCANode> candidateCCANodes = activeSubtreeForRampUp.getCandidateCCANodesPostRampup(NUM_PARTITIONS);

        //if any leafs are left in the ramped up tree, get them individually as CCA nodes
        //These CCA nodes are fake CCA nodes, similar to what we do with every individual leaf
        List<CCANode> leftoverCandidateCCANodes = getadditionalCandidateCCANodes(activeSubtreeForRampUp, candidateCCANodes) ;
         
        if (candidateCCANodes.size() + leftoverCandidateCCANodes.size() < NUM_PARTITIONS) {
            logger.error("this splitToCCAPostRampup partitioning cannot be done  , try ramping up to  a larger number of leafs ");
            exit(ZERO);
        }
         
      
        
        //we accept all generated candidates, see later
        //accept candidates into a list of lists, each list destined for a partition, and having only 1 element in it
        List<List<CCANode>> nodeListCollectionForCCA = new ArrayList<List<CCANode>> ();
        //for each CCA node, put all its leafs in another collection
        //these will be solved using strict best first
        List<List<CCANode>> ccaLeafNodeListCollectionForSBF = new ArrayList<List<CCANode>> ();
        //
        for (CCANode ccaNode: candidateCCANodes  ){
            List<CCANode> temp = new ArrayList<CCANode>();
            temp.add(ccaNode);
            nodeListCollectionForCCA.add( temp  );
            ccaLeafNodeListCollectionForSBF.add(activeSubtreeForRampUp.getActiveLeafsAsCCANodes( ccaNode.pruneList)   );
        }
        
        //we must append the leftoverCandidateCCANodes
        for (  CCANode ccaNode :  leftoverCandidateCCANodes){
            List<CCANode> appendageSBF = new ArrayList<CCANode> ();
            appendageSBF.add(ccaNode);
            ccaLeafNodeListCollectionForSBF.add(appendageSBF); 
            
            List<CCANode> appendageCCA= new ArrayList<CCANode> ();
            appendageCCA.add(ccaNode);
            nodeListCollectionForCCA.add( appendageCCA  );
        }
         
        
        
        
        //at this point, we have farmed out CCA nodes, and also
        //have the corresponding subtree collections for comparision [ each subtree collection has all the leafs of the corresponding CCA]                 
        logger.debug ("number of CCA nodes collected = "+candidateCCANodes.size()  ) ;            
        for (  int index = ZERO; index <  candidateCCANodes.size(); index++){
            logger.debug("CCA node is : " + candidateCCANodes.get(index) + 
                    " and its prune list size is " + candidateCCANodes.get(index).pruneList.size()) ;
            logger.debug ("number of leafs in corresponding ccaLeafNodeList is = " +     
                    ccaLeafNodeListCollectionForSBF.get(index).size() );
        }
         
              
 
        
        //PREPARATIONS COMPLETE
        logger.debug ("Ramp up created " +activeSubtreeForRampUp.getActiveLeafCount() +" leafs.") ;
        logger.debug ("ccaLeafNodeListCollectionForSBF size " + ccaLeafNodeListCollectionForSBF.size()) ;
        NUM_PARTITIONS = ccaLeafNodeListCollectionForSBF.size();
        logger.warn  ("NUM_PARTITIONS is actually " +NUM_PARTITIONS);
        activeSubtreeForRampUp.end();
        
        
        /*
        SPARK DRIVER BEGINS NOW
        
        DISTRIBUTE CCA NODES ACROOS THE CLUSTER AND SOLVE THEM USING STATIC LOAD BALANCING. tHEN REPEAT WITH 
        INDIVIDUAL LEAFS DISTRBUTED
        
        */
        
        //Driver for distributing the CPLEX  BnB solver on Spark
        SparkConf conf = new SparkConf().setAppName("SparcPlex CCA dynamic V1.0");
        JavaSparkContext sc = new JavaSparkContext(conf);
        //these are the RDDs that need to be prepared and then solved
        //we use pair RRDs and partition preserving operation ssince ilocplex objects are not serializable
        JavaPairRDD < Integer, ActiveSubtreeCollection > frontierCCA ; 
        JavaPairRDD < Integer,  ActiveSubtreeCollection > frontierSBF ; 
       
        
        //TEST 1 uses CCA
        //LAter on , TEST 2 will use individual leafs
        
        
        //TEST 1
        
        //init the best known solution value and vector which will be updated as the solution progresses
        //Initialize them to values after ramp up
        //SolutionVector  bestKnownSolution = bestKnownSolutionAfterRampup ==null? null : activeSubtreeONE.getSolutionVector();
        Double  incumbentGlobal= incumbentValueAfterRampup;
         
         
        //the first test uses CCA , the second test will use raw leafs
        frontierCCA = sc.parallelize(nodeListCollectionForCCA)
                /* create pair rdd */
                .mapToPair(new PartitionIdAppender() )
                /*convert into active subtree collection*/
                .mapValues(new ActiveSubtreeCollectionCreator(  incumbentValueAfterRampup,   true  ))  
                //Frontier is used many times, so cache it.
                .cache();
        
        //TEST 1 : with CCA
        int iterationNumber=ZERO;
        for (;   ;iterationNumber++){ 
               
            if(isHaltFilePresent())  break; //halt!
            logger.debug("starting CCA iteration Number "+iterationNumber);
                 
            //solve every partition for SOLUTION_CYCLE_TIME
            JobSolver jobSolver = new JobSolver(incumbentGlobal,   SOLUTION_CYCLE_TIME_MINUTES,  TIME_SLICE_IN_MINUTES_PER_ACTIVE_SUBTREE , NodeSelectionStartegyEnum.STRICT_BEST_FIRST  );
            List<Tuple2<Integer,ResultOfPartitionSolve>> resultList= frontierCCA.mapValues(jobSolver ).collect();
                
            int numRemainingPartitions = ZERO;
            for (Tuple2<Integer,ResultOfPartitionSolve> tuple: resultList){
                
                if (!tuple._2.isComplete) numRemainingPartitions++;
                
                //update driver's copy of incumbent
                if (IS_MAXIMIZATION) {
                    incumbentGlobal= Math.max(incumbentGlobal,  tuple._2.bestKnownSolution);
                }else {
                    incumbentGlobal= Math.min(incumbentGlobal,  tuple._2.bestKnownSolution);
                }
            }
            
            logger.debug ( "Number of reamining partitions is "+ numRemainingPartitions);                

            logger.debug ( "Global incumbent is "+ incumbentGlobal);        
            
            //do another iteration involving every partition, unless we are done
            if (ZERO==numRemainingPartitions)  break;
            
        }//for greenFlagForIterations
        
        logger.info(" CCA test ended at iteration Number "+iterationNumber + " with incumbent "+incumbentGlobal);
        frontierCCA.mapValues( new ActiveSubtreeCollectionEnder());
         
         
        
         
        //HERE is part 2 of the test, where we run individual leafs and compare results with CCA    
        frontierSBF = sc.parallelize(ccaLeafNodeListCollectionForSBF)
            /* create pair rdd */
            .mapToPair(new PartitionIdAppender() )
            /*convert into active subtree collection*/
            .mapValues(new ActiveSubtreeCollectionCreator(  incumbentValueAfterRampup,   true  ))  
            //Frontier is used many times, so cache it.
            .cache();
         
        //repeat test for all node selection strategies
        for(NodeSelectionStartegyEnum nodeSelectionStrategy  :NodeSelectionStartegyEnum.values()){
            
            //skip LSI and BEF
            if(NodeSelectionStartegyEnum.LOWEST_SUM_INFEASIBILITY_FIRST.equals(nodeSelectionStrategy ))  continue; 
            if(NodeSelectionStartegyEnum.BEST_ESTIMATE_FIRST.equals(nodeSelectionStrategy ))  continue; 
            
            logger.info(" \n\n\ntest started for Selection Strategy " + nodeSelectionStrategy  );
            
            //reset incumbent to value after ramp up
            incumbentGlobal= incumbentValueAfterRampup;
            
            //now run the iterations
                 
            for (  iterationNumber=ZERO;   ; iterationNumber++){
   
                if(isHaltFilePresent())  break;//halt
                logger.debug("starting "+nodeSelectionStrategy+" iteration Number "+iterationNumber);
                
                //solve every partition for SOLUTION_CYCLE_TIME
                JobSolver jobSolver = new JobSolver(incumbentGlobal,   SOLUTION_CYCLE_TIME_MINUTES,  TIME_SLICE_IN_MINUTES_PER_ACTIVE_SUBTREE , NodeSelectionStartegyEnum.STRICT_BEST_FIRST  );
                List<Tuple2<Integer,ResultOfPartitionSolve>> resultList= frontierSBF.mapValues(jobSolver ).collect();
             

                int numRemainingPartitions = ZERO;
                for (Tuple2<Integer,ResultOfPartitionSolve> tuple: resultList){

                    if (!tuple._2.isComplete) numRemainingPartitions++;

                    //update driver's copy of incumbent
                    if (IS_MAXIMIZATION) {
                        incumbentGlobal= Math.max(incumbentGlobal,  tuple._2.bestKnownSolution);
                    }else {
                        incumbentGlobal= Math.min(incumbentGlobal,  tuple._2.bestKnownSolution);
                    }
                }
                
                logger.debug ( "Number of reamining partitions is "+ numRemainingPartitions);  
                logger.debug ( "Global incumbent is "+ incumbentGlobal);        
            
                //do another iteration involving every partition, unless we are done
                if (ZERO==numRemainingPartitions)  break;

            }//end     of iterations

            logger.debug(" Individual solve test with "+ nodeSelectionStrategy +" ended at iteration Number "+iterationNumber+ " with incumbent "+incumbentGlobal);
            frontierSBF.mapValues( new ActiveSubtreeCollectionEnder());
            
            logger.info(" test completed Selection Strategy for " + nodeSelectionStrategy);
            
        }//for all node sequencing strategies
        
        
        logger.info("all parts of the test completed");
        
    } //end main
        
    private static boolean isHaltFilePresent (){
        File file = new File("haltfile.txt");
         
        return file.exists();
    }
    
    private static boolean isLogFolderEmpty() {
        File dir = new File (LOG_FOLDER );
        return (dir.isDirectory() && dir.list().length==ZERO);
    }
            
    private static List<CCANode> getadditionalCandidateCCANodes(ActiveSubtree activeSubtreeForRampUp, List<CCANode> candidateCCANodes) throws IloException {
        
        List<NodeAttachment> rampedUpLeafList=        activeSubtreeForRampUp.getActiveLeafList() ;
        List<String> rampedUpLeafListIDs=       new ArrayList<String>() ;        
        for (NodeAttachment na : rampedUpLeafList){
            rampedUpLeafListIDs.add(na.nodeID );
        }
        
        for (CCANode ccaNode: candidateCCANodes) {
            rampedUpLeafListIDs.removeAll( ccaNode.pruneList );
        }
        
        return activeSubtreeForRampUp.getActiveLeafsAsCCANodes( rampedUpLeafListIDs);     
    }
        
}

