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
  

/**
 *
 * @author tamvadss
 * 
 *  glass4   , 50000:5000   0.3  - TEST 1 confirmed - took 7 hours
 
 *  glass4 1000000, 100 
 
 
 * 
 * run MIP on 5 simulated partitions
 * sub problems created using variable bound merging, and solved using traditional branch and bound
 * 2 ramp ups, one for using CCA and one without CCA
 */
public class TestDriver_CCA_DynamicSearch_SimulatedCluster {
    
    private static  Logger logger = null;
    
    //  50(20), 100(40) , 200(90), 250(112) parts is ok
    
    //wnq-n100-mw99-14  ru=5000, pa=100  size >= 50/4 yeilds 85 candidates with home=42   fast  big-mem-hog-cannot_simulate
    //p100x588b        ru=15000, pa=100  size >= 50/4 yeilds 97 candidates with home=53   fast  mem-hog-cannot_simulate
    //b2c1s1 ru=5000, pa=100  size >= 50/3 yeilds 94 candidates with home=48
    //seymour-disj-10 ru=5000, pa=100  size >= 50/4 yeilds 68 candidates with home=74
    //usAbbrv-8-25_70 ru=10000, pa=100  size >= 50/4 yeilds 96 candidates with home=77
    //neos-847302 ru=10000, pa=100  size >= 50/4 yeilds 94 candidates with home=50
    //janos-us-DDM ru=8000, pa=100  size >= 50/4 yeilds 90 candidates with home=30  fast   leaves lots on home, try increasing packfact to 2.0 or min allowed size
    //
    //seymour ru=8000, pa=100  size >= 50/4 yeilds 99 candidates with home=40
    //rococoB10-011000 ru=5000, pa=100  size >= 50/4 yeilds 95 candidates with home=19
    //  momentum1  ru=5000, pa=100  size >= 50/4 yeilds 90 candidates with home=88
    
    //for big partition counts
    //
    //had1 running janos with 1000 with memcheck and 2 min slices, had2 running seymour with   500 and memory and 2 min slices
    //had 3 running seymour with 250  with memcheck and 2 min slices , had 4 running seymour with 250  with memcheck and 3minute slices
    //had 5 runing ? with 250 with memcheck with 2 min slices CB+LSI+CCA
    //
    //p100x588b ru=60000, NUM_PARTITIONS = 1150 , 580, 250 , sct = 6m, ts=2m, 6m:3m 6m:1m size >=20/4, packfact=1.2
    //wnq-n100-mw99-14 ru=25000, NUM_PARTITIONS = 1150 , 550, 250 , sct = 6m, ts=2m, 6m:3m 6m:1m
    //janos-us-DDM ru=20000, NUM_PARTITIONS = 1150 , 580, 250 , sct = 6m, ts=2m, 6m:3m 6m:1m 
    //seymour ru=10000, NUM_PARTITIONS =  200 , sct = 6m, ts=2m, 6m:3m 6m:1m
    //seymour-disj_10 ru=20000, NUM_PARTITIONS =  1000 , sct = 6m, ts=2m, 6m:3m 6m:1m size >=20/10, packfact=12 
     
    
   
    
    /*
    public static   String MIP_NAME_UNDER_TEST ="glass4";
    public static   double MIP_WELLKNOWN_SOLUTION = 1200012600   ;
    public static   int RAMP_UP_TO_THIS_MANY_LEAFS = 10000;   // or 5000
    */
    
    /*
    public static   String MIP_NAME_UNDER_TEST ="timtab1";
    public static   double MIP_WELLKNOWN_SOLUTION =  764772;
    public static   int RAMP_UP_TO_THIS_MANY_LEAFS = 2000;  */
 
   
    //private static final int SOLUTION_CYCLE_Tu           fgggd hjhhIME_MINUTES = THREE;
     
    public static void main(String[] args) throws Exception {
       
        if (! isLogFolderEmpty()) {
            System.err.println("\n\n\nClear the log folder before starting the test." + LOG_FOLDER);
            exit(ONE);
        }
            
        logger=Logger.getLogger(TestDriver_CCA_DynamicSearch_SimulatedCluster.class);
        logger.setLevel(Level.DEBUG);
        
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            RollingFileAppender rfa = new  RollingFileAppender(layout,LOG_FOLDER+TestDriver_CCA_DynamicSearch_SimulatedCluster.class.getSimpleName()+ LOG_FILE_EXTENSION);
           
            rfa.setMaxBackupIndex(TEN*TEN);
            logger.addAppender(rfa);
            logger.setAdditivity(false);
            
        } catch (Exception ex) {
            System.err.println("Exit: unable to initialize logging");        
            exit(1);
        }
         
        //first run 4 identical ramp ups
        //MPS_FILE_ON_DISK =  "F:\\temporary files here\\"+MIP_NAME_UNDER_TEST+".mps"; //windows
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
        
        //for every accepted CCA node, we create an active subtree collection that has all its leafs
        List<ActiveSubtreeCollection> activeSubtreeCollectionListSBF = new ArrayList<ActiveSubtreeCollection>();
        //we also create an ActiveSubtreeCollectionlist where each collection on a partition only has 1 element, namely the CCA node assigned to it.
        List<ActiveSubtreeCollection> activeSubtreeCollectionListCCA = new ArrayList<ActiveSubtreeCollection>();
        
        //we are now adding a duplicate SBF list, so that we can repeat the SBF test with the time slice doubled if REPEAT_SBF_TEST_WITH_DOUBLED_TIMESLICE
        List<ActiveSubtreeCollection> activeSubtreeCollectionListSBF_Duplicate = new ArrayList<ActiveSubtreeCollection>();
        // another duplicate list for repeating the CCA test with doubled solution cycle time 
        //List<ActiveSubtreeCollection> activeSubtreeCollectionListCCA_Duplicate = new ArrayList<ActiveSubtreeCollection>();
        
        // now lets populate the ActiveSubtreeCollections
        //
        long acceptedCandidateWithLowestNumOfLeafs = PLUS_INFINITY;
        int index = ZERO;
        for (; index < candidateCCANodes.size(); index ++){

            CCANode ccaNode= candidateCCANodes.get(index);
            
            if (ccaNode.pruneList.size() >=  ZERO) { //accept every candidate
                
                logger.debug (""+ccaNode.nodeID + " has good factor " +ccaNode.getPackingFactor() + 
                        " and prune list size " + ccaNode.pruneList.size() + " depth from root "+ ccaNode.depthOfCCANodeBelowRoot) ; 
                 
                //          qxxy               dod     
                               
                acceptedCandidateWithLowestNumOfLeafs = Math.min(acceptedCandidateWithLowestNumOfLeafs,ccaNode.pruneList.size()  );
                
                //All leafs need to be converted into CCA node representations, since solvers deal with CCA nodes and not leafs
                //These CCA nodes are of course fake CCA nodes
                List<CCANode> ccaLeafNodeListSBF = activeSubtreeForRampUp.getActiveLeafsAsCCANodes( ccaNode.pruneList);                                      
                //now create an active subtree collection , which represents the work on one partition by doing a round-robin thru these leafs
                ActiveSubtreeCollection astc = new ActiveSubtreeCollection (ccaLeafNodeListSBF, activeSubtreeForRampUp.instructionsFromOriginalMip, incumbentValueAfterRampup, bestKnownSolutionAfterRampup!=null, index) ;
                activeSubtreeCollectionListSBF.add(astc);
                
                //populat ethe duplicate collection
                if (REPEAT_SBF_TEST_WITH_TIMESLICE_MULTIPLED_BY>ONE){
                    List<CCANode> ccaLeafNodeListSBF_Duplicate = new ArrayList<CCANode>();
                    ccaLeafNodeListSBF_Duplicate.addAll(ccaLeafNodeListSBF);
                    ActiveSubtreeCollection astc_duplicate = new ActiveSubtreeCollection (ccaLeafNodeListSBF_Duplicate, activeSubtreeForRampUp.instructionsFromOriginalMip, incumbentValueAfterRampup, bestKnownSolutionAfterRampup!=null, index) ;
                    activeSubtreeCollectionListSBF_Duplicate.add(astc_duplicate);
                }
                
                //we also create another collection list, with only the accepted CCA nodes, one on each partition
                List<CCANode> ccaSingletonLeafNodeList = new ArrayList<CCANode> ();
                ccaSingletonLeafNodeList.add(ccaNode);
                astc = new ActiveSubtreeCollection ( ccaSingletonLeafNodeList, activeSubtreeForRampUp.instructionsFromOriginalMip, incumbentValueAfterRampup, bestKnownSolutionAfterRampup!=null, index) ;
                activeSubtreeCollectionListCCA.add(astc);
                
            }               
        }
         
        //we must append the leftoverCandidateCCANodes
        for (  CCANode ccaNode :  leftoverCandidateCCANodes){
            List<CCANode> appendageSBF = new ArrayList<CCANode> ();
            appendageSBF.add(ccaNode);
            activeSubtreeCollectionListSBF.add (
                    new ActiveSubtreeCollection (  appendageSBF, activeSubtreeForRampUp.instructionsFromOriginalMip, 
                            incumbentValueAfterRampup, bestKnownSolutionAfterRampup!=null, index)
            );
            
            if (REPEAT_SBF_TEST_WITH_TIMESLICE_MULTIPLED_BY>ONE)            {
                List<CCANode> appendageSBF_duplicate = new ArrayList<CCANode> ();
                appendageSBF_duplicate.add(ccaNode);
                activeSubtreeCollectionListSBF_Duplicate.add (
                        new ActiveSubtreeCollection (  appendageSBF_duplicate, activeSubtreeForRampUp.instructionsFromOriginalMip, 
                                incumbentValueAfterRampup, bestKnownSolutionAfterRampup!=null, index)    );
            }

            
            List<CCANode> appendageCCA= new ArrayList<CCANode> ();
            appendageCCA.add(ccaNode);
            activeSubtreeCollectionListCCA.add(
                    new ActiveSubtreeCollection ( appendageCCA , activeSubtreeForRampUp.instructionsFromOriginalMip, 
                            incumbentValueAfterRampup, bestKnownSolutionAfterRampup!=null, index++)
            );
        }
        
        //at this point, we have farmed out CCA nodes, and also
        //have the corresponding subtree collections for comparision [ each subtree collection has all the leafs of the corresponding CCA]                 
        logger.debug ("number of CCA nodes collected = "+candidateCCANodes.size()  ) ;            
        for (   index = ZERO; index <  candidateCCANodes.size(); index++){
            logger.debug("CCA node is : " + candidateCCANodes.get(index).nodeID +            
                    "  and its prune list size is " + candidateCCANodes.get(index).pruneList.size()+
                    " and it spacking factor is " +  candidateCCANodes.get(index).getPackingFactor()+
                    " and distance from root is " + candidateCCANodes.get(index).depthOfCCANodeBelowRoot) ;
            //logger.debug("CCA node is : " + candidateCCANodes.get(index) +                     " and its prune list size is " + candidateCCANodes.get(index).pruneList.size()) ;
            //logger.debug ("number of leafs in corresponding active subtree collection SBF is = " +                         (activeSubtreeCollectionListSBF.get(index).getPendingRawNodeCount() + activeSubtreeCollectionListSBF.get(index).getNumTrees()) );
        }
         
              
 
        
        //PREPARATIONS COMPLETE
        logger.debug ("Ramp up created " +activeSubtreeForRampUp.getActiveLeafCount() +" leafs.") ;
        logger.debug ("activeSubtreeCollectionListSBF size " + activeSubtreeCollectionListSBF.size()) ;
        logger.debug ("activeSubtreeCollectionListCCA size " + activeSubtreeCollectionListCCA.size()) ;
        NUM_PARTITIONS = activeSubtreeCollectionListCCA.size();
        logger.warn  ("NUM_PARTITIONS is actually " +NUM_PARTITIONS);
        activeSubtreeForRampUp.end();
       
        
        //TEST 1 uses CCA
        //LAter on , TEST 2 will use individual leafs
        
        
        //TEST 1
        
        //init the best known solution value and vector which will be updated as the solution progresses
        //Initialize them to values after ramp up
        //SolutionVector  bestKnownSolution = bestKnownSolutionAfterRampup ==null? null : activeSubtreeONE.getSolutionVector();
        Double  incumbentGlobal= incumbentValueAfterRampup;
         
         
        //the first test uses CCA , the second test will use raw leafs
        
        //TEST 1 : with CCA
        int iterationNumber=ZERO;
        for (;   MAX_ITERATIONS_LIMIT >iterationNumber ;iterationNumber++){ 
               
            if(isHaltFilePresent())  break; //halt!
            logger.debug("starting CCA iteration Number "+iterationNumber);
                 
            int numRemainingPartitions = simulateOneMapIteration ( activeSubtreeCollectionListCCA, 
                                                   NodeSelectionStartegyEnum.STRICT_BEST_FIRST,    incumbentGlobal, ONE);  
                
            //update driver's copy of incumbent
            for (ActiveSubtreeCollection astc : activeSubtreeCollectionListCCA){
                if (IS_MAXIMIZATION) {
                    incumbentGlobal= Math.max(incumbentGlobal,  astc.getLocalIncumbentValue());
                }else {
                    incumbentGlobal= Math.min(incumbentGlobal,  astc.getLocalIncumbentValue());
                }
            }

            logger.debug ( "Number of reamining partitions is "+ numRemainingPartitions);                

            logger.debug ( "Global incumbent is "+ incumbentGlobal);        
            
            //do another iteration involving every partition, unless we are done
            if (ZERO==numRemainingPartitions)  break;
            
        }//for greenFlagForIterations
        
        logger.debug(" CCA test ended at iteration Number "+iterationNumber + " with incumbent "+incumbentGlobal);
        //print status of every partition
        for (int partitionNumber = ZERO;partitionNumber < NUM_PARTITIONS; partitionNumber++ ){

            ActiveSubtreeCollection astc= activeSubtreeCollectionListCCA.get(partitionNumber);
            logger.debug ("partition "+partitionNumber   +
                    " trees count " + astc.getNumTrees()+" raw nodes count "+ astc.getPendingRawNodeCount() + " max trees created " + astc.maxTreesCreatedDuringSolution);
            astc.endAll();

        }//print status of every partition
         
        
        
         
        //HERE is part 2 of the test, where we run individual leafs and compare results with CCA   
        
        //aloow SBF scheme more iters than LCA, but limit it so that we do not wait forever
        final int MAX_ITERS_ALLOWED_FOR_SBF = ADDITIONAL_SOLUTION_CYCLES_ALLOWED_FOR_SBF_COMAPRED_TO_LCA + iterationNumber;
        logger.debug ("max soln cycles for SBF = "+MAX_ITERS_ALLOWED_FOR_SBF) ;
           
        //this is the collection we will put on each partition
        List<ActiveSubtreeCollection> activeSubtreeCollectionList =null;
        
        for (int timeSliceMultiplicationFactor =ONE ; timeSliceMultiplicationFactor<=REPEAT_SBF_TEST_WITH_TIMESLICE_MULTIPLED_BY;
                timeSliceMultiplicationFactor++) {
            
            logger.debug ("\nUsing time slice multiplied by "+timeSliceMultiplicationFactor) ;

            //repeat test for all node selection strategies
            for(NodeSelectionStartegyEnum nodeSelectionStrategy  :NodeSelectionStartegyEnum.values()){

                //skip LSI and BEF
                if(NodeSelectionStartegyEnum.LOWEST_SUM_INFEASIBILITY_FIRST.equals(nodeSelectionStrategy ))  continue; 
                if(NodeSelectionStartegyEnum.BEST_ESTIMATE_FIRST.equals(nodeSelectionStrategy ))  continue; 

                if(NodeSelectionStartegyEnum.STRICT_BEST_FIRST.equals(nodeSelectionStrategy )){
                    activeSubtreeCollectionList= activeSubtreeCollectionListSBF;   
                    if (timeSliceMultiplicationFactor>ONE) {
                        activeSubtreeCollectionList= activeSubtreeCollectionListSBF_Duplicate;       
                    }
                } 

                logger.info(" \n\n\ntest started for Selection Strategy " + nodeSelectionStrategy + " with time slice " + 
                        TIME_SLICE_IN_MINUTES_PER_ACTIVE_SUBTREE*timeSliceMultiplicationFactor);

                //reset incumbent to value after ramp up
                incumbentGlobal= incumbentValueAfterRampup;

                //now run the iterations

                for (  iterationNumber=ZERO;  MAX_ITERATIONS_LIMIT >iterationNumber && MAX_ITERS_ALLOWED_FOR_SBF > iterationNumber; iterationNumber++){

                    if(isHaltFilePresent())  break;//halt
                    logger.debug("starting "+nodeSelectionStrategy+" iteration Number "+iterationNumber);

                    int numRemainingPartitions = simulateOneMapIteration ( activeSubtreeCollectionList, 
                                                       nodeSelectionStrategy,    incumbentGlobal, timeSliceMultiplicationFactor);

                    //update driver's copy of incumbent
                    for (ActiveSubtreeCollection astc : activeSubtreeCollectionListCCA){
                        if (IS_MAXIMIZATION) {
                            incumbentGlobal= Math.max(incumbentGlobal,  astc.getLocalIncumbentValue());
                        }else {
                            incumbentGlobal= Math.min(incumbentGlobal,  astc.getLocalIncumbentValue());
                        }
                    }

                    logger.debug ( "Number of reamining partitions is "+ numRemainingPartitions);  
                    logger.debug ( "Global incumbent is "+ incumbentGlobal);        

                    //do another iteration involving every partition, unless we are done
                    if (ZERO==numRemainingPartitions)  break;

                }//end     of iterations

                logger.debug(" Individual solve test with "+ nodeSelectionStrategy +" ended at iteration Number "+iterationNumber+ " with incumbent "+incumbentGlobal);
                //print status of every partition
                for (int partitionNumber = ZERO;partitionNumber < NUM_PARTITIONS; partitionNumber++ ){

                    ActiveSubtreeCollection astc= activeSubtreeCollectionList.get(partitionNumber);
                    logger.debug ("partition "+partitionNumber   +
                            " trees count " + astc.getNumTrees()+" raw nodes count "+ astc.getPendingRawNodeCount() + " max trees created " + astc.maxTreesCreatedDuringSolution);
                    astc.endAll();

                }//print status of every partition

                logger.info(" test completed Selection Strategy for " + nodeSelectionStrategy + " with time slice "+ TIME_SLICE_IN_MINUTES_PER_ACTIVE_SUBTREE*timeSliceMultiplicationFactor);

            }//for all node sequencing strategies


            
        }//for all time slice multiplication factors
        
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
    
    //simulate 1 map iteration on the cluster
    //there is one ActiveSubtreeCollection on each partition
    private static int simulateOneMapIteration (List<ActiveSubtreeCollection> activeSubtreeCollectionList, 
            NodeSelectionStartegyEnum nodeSelectionStrategy, final Double  incumbentGlobal, int time_slice_multiplication_factor) throws Exception{

        int numRemainingPartitions = activeSubtreeCollectionList.size(); // = NUM_PARTITIONS
        
         
        //solve every partition for 3 minutes at a time
        for (int partitionNumber = ZERO;partitionNumber < NUM_PARTITIONS; partitionNumber++ ){                     

            long rawnodeCount = activeSubtreeCollectionList.get(partitionNumber).getPendingRawNodeCount();
            long treeCount = activeSubtreeCollectionList.get(partitionNumber).getNumTrees();

            if (rawnodeCount+treeCount==ZERO)  continue;

            logger.debug("Solving partition for "+ SOLUTION_CYCLE_TIME_MINUTES +" minutes " + 
                         " having " +rawnodeCount + " rawnodes and " + treeCount + " trees " + 
                         " best reamining lp realx value "  + activeSubtreeCollectionList.get(partitionNumber).getBestRemainingLPRElaxValue() +
                         " ... Partition_" + partitionNumber );

            //inform the partition of the latest global incumbent
            activeSubtreeCollectionList.get(partitionNumber).setCutoff( incumbentGlobal);
            activeSubtreeCollectionList.get(partitionNumber).solve(  SOLUTION_CYCLE_TIME_MINUTES  ,     
                        TIME_SLICE_IN_MINUTES_PER_ACTIVE_SUBTREE*time_slice_multiplication_factor,  nodeSelectionStrategy  );               
        }

       

        //if every partition is done, we can stop the iterations
                    
        //check all the  partitions
        for (int partitionNumber = ZERO;partitionNumber < NUM_PARTITIONS; partitionNumber++ ){   
            if (activeSubtreeCollectionList.get(partitionNumber).getPendingRawNodeCount() + activeSubtreeCollectionList.get(partitionNumber).getNumTrees() ==ZERO) {
                //logger.info("This partition is complete: " + partitionNumber);
                numRemainingPartitions --;
            }  
        }
        
        return numRemainingPartitions;

    }
    
    
    private static List<CCANode> getadditionalCandidateCCANodes(ActiveSubtree activeSubtreeForRampUp, List<CCANode> candidateCCANodes) throws IloException {
        
        List<CCANode>  result = new ArrayList<CCANode> ();
        
        logger.debug("get rampedUpLeafList...");
        List<NodeAttachment> rampedUpLeafList=        activeSubtreeForRampUp.getActiveLeafList() ;
        List<String> rampedUpLeafListIDs=       new ArrayList<String>() ;        
        for (NodeAttachment na : rampedUpLeafList){
            rampedUpLeafListIDs.add(na.nodeID );
        }
        
        long rampedUpLeafListSize= rampedUpLeafListIDs.size();
        logger.debug("size rampedUpLeafList is "+rampedUpLeafListIDs.size());
        
        long pruneListSizeCumulative = ZERO;
        for (CCANode ccaNode: candidateCCANodes) {
            pruneListSizeCumulative+= ccaNode.pruneList.size();
        }
        
        if (pruneListSizeCumulative==rampedUpLeafListSize) {
             logger.debug("no leafs left behind." );
        }else {
            //must identify leafs left behind
            for (CCANode ccaNode: candidateCCANodes) {
                for (String pruneListEntry: ccaNode.pruneList){
                    rampedUpLeafListIDs.remove( pruneListEntry);
                }            
            }
            
            result = activeSubtreeForRampUp.getActiveLeafsAsCCANodes( rampedUpLeafListIDs);   
        }
        
        return   result;
    }
}



