/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spCcaTest_v1_0.cplex;

import static ca.mcmaster.spCcaTest_v1_0.ConstantsAndParameters.IS_MAXIMIZATION;
import static ca.mcmaster.spCcaTest_v1_0.ConstantsAndParameters.LOG_FILE_EXTENSION;
import static ca.mcmaster.spCcaTest_v1_0.ConstantsAndParameters.LOG_FOLDER;
import static ca.mcmaster.spCcaTest_v1_0.ConstantsAndParameters.MINUS_INFINITY;
import static ca.mcmaster.spCcaTest_v1_0.ConstantsAndParameters.ONE;
import static ca.mcmaster.spCcaTest_v1_0.ConstantsAndParameters.PLUS_INFINITY;
import static ca.mcmaster.spCcaTest_v1_0.ConstantsAndParameters.*;
import ca.mcmaster.spCcaTest_v1_0.cb.CBInstructionTree;
import ca.mcmaster.spCcaTest_v1_0.cca.CCANode;
import static ca.mcmaster.spCcaTest_v1_0.cplex.NodeSelectionStartegyEnum.BEST_ESTIMATE_FIRST;
import static ca.mcmaster.spCcaTest_v1_0.cplex.NodeSelectionStartegyEnum.STRICT_BEST_FIRST;
import ca.mcmaster.spCcaTest_v1_0.cplex.datatypes.BranchingInstruction;
import ca.mcmaster.spCcaTest_v1_0.cplex.datatypes.SolutionVector;
import ilog.concert.IloException;
import ilog.cplex.IloCplex.Status;
import java.io.File;
import static java.lang.System.exit;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

/**
 *
 * @author tamvadss
 * 
 * rd-rplusc-21.mps was solved using round robin in             165395.2753
 * 
 * This class represents a partition, and the work on it
 * 
 */
public class ActiveSubtreeCollection {
    
    private static Logger logger=Logger.getLogger(ActiveSubtreeCollection.class);
        
    private List<ActiveSubtree> activeSubTreeList = new ArrayList<ActiveSubtree>();
    private List<CCANode> rawNodeList = new ArrayList<CCANode>();
    //record the branching instructions required to arrive at the subtree root node under which these CCA nodes lie
    //To promote a CCA node into an IloCplex, we need to apply these branching conditions and then the CCA branching conditions
    // the call is activeSubtree.mergeVarBounds(ccaNode,  instructionsFromOriginalMip, true);  
    private  List<BranchingInstruction> instructionsFromOriginalMIP ;
    
    private double incumbentLocal= IS_MAXIMIZATION ? MINUS_INFINITY : PLUS_INFINITY;
    private SolutionVector incumbentSolution = null;
    
    //astc id
    private int PARTITION_ID;
    
    //keep track of max trees created in this collection during solution
    public    int maxTreesCreatedDuringSolution = ONE;
    
    static {
        logger.setLevel(Level.OFF);
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            RollingFileAppender rfa =new  RollingFileAppender(layout,LOG_FOLDER+ActiveSubtreeCollection.class.getSimpleName()+ LOG_FILE_EXTENSION);
            rfa.setMaxBackupIndex(HUNDRED);
            logger.addAppender(rfa);
            logger.setAdditivity(false);
        } catch (Exception ex) {
            ///
            System.err.println("Exit: unable to initialize logging");       
            exit(1);
        }
          
    }
    
    public ActiveSubtreeCollection (List<CCANode> ccaNodeList, List<BranchingInstruction> instructionsFromOriginalMip, double cutoff, boolean useCutoff, int id) throws Exception {
        rawNodeList=ccaNodeList;
        this.instructionsFromOriginalMIP = instructionsFromOriginalMip;
        if (useCutoff) this.incumbentLocal= cutoff;
        //create 1 tree
        //this.promoteCCANodeIntoActiveSubtree( this.getRawNodeWithBestLPRelaxation(), false);
        
        PARTITION_ID=id;
        
        
        logger.debug("params are SAVE_TO_DISK_FLAG="+ (SAVE_TO_DISK_FLAG?ONE:ZERO) +  " REPEAT_SBF_TEST_WITH_TIMESLICE_MULTIPLED_BY="+
                     REPEAT_SBF_TEST_WITH_TIMESLICE_MULTIPLED_BY + " MAX_ITERATIONS_LIMIT="+MAX_ITERATIONS_LIMIT) ;
        
    }
    
    public void setCutoff (double cutoff) {
        this.incumbentLocal= cutoff;
    }
    
    public void setMIPStart(SolutionVector solutionVector) throws IloException {
        for (ActiveSubtree ast: activeSubTreeList){
            ast.setMIPStart(solutionVector);
        }        
    }
    
    public double getIncumbentValue (){
        return new Double (this.incumbentLocal);
    }
    

    //calculate MIP gap using global incumbent, which will be updated as this collection' s incumbent, and the best LP relax value
    //invoke this method only if computation has an incumbent
    public double getRelativeMIPGapPercent ()  {
        double result = -ONE;
        
        try {
            double bestInteger=this.incumbentLocal;
            double bestBound = this.getBestReaminingLPRElaxValue() ;

            double relativeMIPGap =  bestBound - bestInteger ;        
            if (! IS_MAXIMIZATION)  {
                relativeMIPGap = relativeMIPGap /(EPSILON + Math.abs(bestInteger  ));
            } else {
                relativeMIPGap = relativeMIPGap /(EPSILON + Math.abs(bestBound));
            }

            result = Math.abs(relativeMIPGap)*HUNDRED;
        }catch (Exception ex){
            logger.error("Error calculating mipgap "+ ex.getMessage() );
        }
        
        return  result;
    }
    
   
    
        

    
    public void endAll(){
        for (ActiveSubtree ast : this.activeSubTreeList){
            ast.end();
        }
    }
    
    public void solve (  double solutionCycleTimeMinutes,   double timeSlicePerTreeInMInutes ,         
            NodeSelectionStartegyEnum nodeSelectionStartegy) throws Exception {
     
        solve (    solutionCycleTimeMinutes,     timeSlicePerTreeInMInutes ,         
              nodeSelectionStartegy,            false, false   , null);
    }
     
    public void solve (  double solutionCycleTimeMinutes,   double timeSlicePerTreeInMInutes ,         
            NodeSelectionStartegyEnum nodeSelectionStartegy,
            boolean useTraditionalSearch, boolean reincarnateFlag , CBInstructionTree cbTree) throws Exception {
        logger.info(" \n solving ActiveSubtree Collection ... " + PARTITION_ID); 
        Instant startTime = Instant.now();
                
        while (activeSubTreeList.size()+ this.rawNodeList.size()>ZERO && Duration.between( startTime, Instant.now()).toMillis()< solutionCycleTimeMinutes*SIXTY*THOUSAND){
            
            double timeUsedUpMInutes = ( DOUBLE_ZERO+ Duration.between( startTime, Instant.now()).toMillis() ) / (SIXTY*THOUSAND) ;
                
            logger.info("time in seconds left = "+ (solutionCycleTimeMinutes -timeUsedUpMInutes)*SIXTY );            
                        
            //pick tree with best lp
            ActiveSubtree tree = this.getTreeWithBestRemainingMetric(nodeSelectionStartegy );
            //pick raw node with best LP
            CCANode rawNode = this.getRawNodeWithBestMetric( nodeSelectionStartegy);
            //check if promotion required
            if (null != rawNode && tree !=null){
                if ((IS_MAXIMIZATION  && rawNode.lpRelaxationValue> tree.getBestRemaining_LPValue() )  || 
                    (!IS_MAXIMIZATION && rawNode.lpRelaxationValue< tree.getBestRemaining_LPValue() ) ){
                    //promotion needed
                    tree = promoteCCANodeIntoActiveSubtree(rawNode, false);
                } 
            }else if (tree ==null){
                //promotion needed
                tree = promoteCCANodeIntoActiveSubtree(rawNode, false);
            }else if (null==rawNode){
                //just solve the best tree available
            }

            //keep track of max trees created on this partition during solution
            maxTreesCreatedDuringSolution = Math.max(maxTreesCreatedDuringSolution ,  activeSubTreeList.size());
                        
            //set best known LOCAL solution, if any, as MIP start
            logger.debug (tree.guid +" " + tree.isFeasible() + " Obj val = "+ (tree.isFeasible() ?tree.getObjectiveValue():"NONE")+ "  local incumbent "+ incumbentLocal);
            if (incumbentLocal != MINUS_INFINITY  && incumbentLocal != PLUS_INFINITY){
                if (tree.isFeasible()){
                    if (  (IS_MAXIMIZATION  && incumbentLocal> tree.getObjectiveValue())  || (!IS_MAXIMIZATION && incumbentLocal< tree.getObjectiveValue()) ) {                
                        //tree.setMIPStart(incumbentSolution);
                        logger.debug (tree.guid +" " + tree.isFeasible() + " setting cutoff to "+ incumbentLocal);
                        tree.setCutoffValue( incumbentLocal);
                    }
                } else{
                    //tree.setMIPStart(incumbentSolution);
                    logger.debug (tree.guid +" " + tree.isFeasible() + " setting cutoff to "+ incumbentLocal);
                    tree.setCutoffValue( incumbentLocal);
                }
            }

            //solve the tree for time slice reamining
            double timeSlice = timeSlicePerTreeInMInutes; //default
            if (  solutionCycleTimeMinutes -timeUsedUpMInutes < timeSlicePerTreeInMInutes ) {
                timeSlice= solutionCycleTimeMinutes -timeUsedUpMInutes;
                if (timeSlice < MINIMUM_TIME_SLICE_IN_MINUTES_PER_ACTIVE_SUBTREE) timeSlice = MINIMUM_TIME_SLICE_IN_MINUTES_PER_ACTIVE_SUBTREE; //15 second least count
            }
            if (timeSlice>ZERO) {
                logger.info("Solving tree seeded by cca node "+ tree.seedCCANodeID + " with " + 
                        tree.guid  + " for minutes " +  timeSlice + " having cutoff " + 
                        tree.getCurrentCutoff());  
                
                if (!useTraditionalSearch) {
                    tree. solveWithDynamicSearch(timeSlice  );
                } else if (reincarnateFlag){
                    //reincarnate 
                    tree.reincarnate( cbTree.asMap() ,   tree.seedCCANodeID ,  PLUS_INFINITY , false  );   
                } else {
                    //solve using traditional
                    tree. solveWithTraditionalSearch(timeSlice  );
                }
            }
            
            //update LOCAL incumbent if needed            
            if (tree.isFeasible()|| tree.isOptimal()){
                double objVal =tree.getObjectiveValue();
                if ((IS_MAXIMIZATION && incumbentLocal< objVal)  || (!IS_MAXIMIZATION && incumbentLocal> objVal) ){
                    incumbentLocal = objVal;
                    this.incumbentSolution=tree.getSolutionVector();
                    logger.info("Local Incumbent updated to  "+ this.incumbentLocal + " by tree " + tree.guid + " on this partition " + PARTITION_ID);
                }
            }
            
            //remove   tree from list of jobs, if tree is solved to completion
            if (tree.isUnFeasible()|| tree.isOptimal()) {
                logger.info("Tree completed "+ tree.seedCCANodeID + ", " + tree.guid + ", " +   tree.getStatus()) ;
                tree.end();
                this.activeSubTreeList.remove( tree);

            }           
            logger.info("Number of trees left is "+ this.activeSubTreeList.size());  
            printStatus();
            
            //do not continue solution cycle if this cycle is just for reincarnation
            if (reincarnateFlag) break; 
            
        } //while solution cycle not complete
        
        logger.info(" ActiveSubtree Collection solution cycle complete "+PARTITION_ID );
    }
        
    public double getLocalIncumbentValue (){
        return new Double (this.incumbentLocal);
    }
    
    
    
    public long getPendingRawNodeCount (){
        return this.rawNodeList.size();
    }
    
     
    
    public int getNumTrees() {
        return  activeSubTreeList.size();
    }
    
    public double getBestRemainingLPRElaxValue () throws Exception{
        return getBestReaminingLPRElaxValue();
    }
    
    private void printStatus() throws IloException {
        for (ActiveSubtree activeSubtree: this.activeSubTreeList){
            logger.debug( "Active tree " + activeSubtree.seedCCANodeID + ", " + activeSubtree.guid + ", " +   
                           activeSubtree.getStatus() +", BestRemaining_LPValue=" +activeSubtree.getBestRemaining_LPValue()           );
        }
        logger.debug("Number of pending raw nodes " + getPendingRawNodeCount());
    }
    
    private double getBestReaminingLPRElaxValue () throws Exception{
        double   bestReamining_LPValue = IS_MAXIMIZATION ? MINUS_INFINITY : PLUS_INFINITY;
        
        ActiveSubtree tree= getTreeWithBestRemainingMetric(NodeSelectionStartegyEnum.STRICT_BEST_FIRST);
        if (tree!=null) bestReamining_LPValue =     tree.getBestRemaining_LPValue();
        
        CCANode rawNode = this.getRawNodeWithBestMetric(STRICT_BEST_FIRST);
        if( rawNode!=null){
            if (IS_MAXIMIZATION){
                bestReamining_LPValue = Math.max(bestReamining_LPValue, rawNode.lpRelaxationValue) ;
            }else {
                bestReamining_LPValue = Math.min(bestReamining_LPValue, rawNode.lpRelaxationValue) ;
            }
        }
        
        return     bestReamining_LPValue;
    }
    
    private ActiveSubtree getTreeWithBestRemainingMetric (NodeSelectionStartegyEnum strategyEnum) throws Exception{
                                                   
        double   bestReamining_metric =  NodeSelectionStartegyEnum.LOWEST_SUM_INFEASIBILITY_FIRST.equals(strategyEnum ) ? PLUS_INFINITY: (IS_MAXIMIZATION ? MINUS_INFINITY : PLUS_INFINITY);
        ActiveSubtree result = null;
        
        for (ActiveSubtree activeSubtree: this.activeSubTreeList){
            if (IS_MAXIMIZATION) {
                if (STRICT_BEST_FIRST.equals(strategyEnum) && bestReamining_metric<  activeSubtree.getBestRemaining_LPValue()) {
                    result = activeSubtree;
                    bestReamining_metric=activeSubtree.getBestRemaining_LPValue();
                }  
            }else {
                if (STRICT_BEST_FIRST.equals(strategyEnum) && bestReamining_metric>  activeSubtree.getBestRemaining_LPValue()) {
                    result = activeSubtree;
                    bestReamining_metric=activeSubtree.getBestRemaining_LPValue();
                } 
            }
          
        }
        return result;
    }
    
    private CCANode getRawNodeWithBestMetric  (NodeSelectionStartegyEnum strategyEnum) {
        double   bestReamining_metric =  NodeSelectionStartegyEnum.LOWEST_SUM_INFEASIBILITY_FIRST.equals(strategyEnum ) ? PLUS_INFINITY: (IS_MAXIMIZATION ? MINUS_INFINITY : PLUS_INFINITY);
        CCANode result = null;
        
        for (CCANode ccaNode : this.rawNodeList){
            if (IS_MAXIMIZATION) {
                if (STRICT_BEST_FIRST.equals(strategyEnum) &&  bestReamining_metric<  ccaNode.lpRelaxationValue) {
                    result = ccaNode;
                    bestReamining_metric=ccaNode.lpRelaxationValue;
                } else if (NodeSelectionStartegyEnum.BEST_ESTIMATE_FIRST .equals(strategyEnum) &&  bestReamining_metric<  ccaNode.bestEstimateValue) {
                    result = ccaNode;
                    bestReamining_metric=ccaNode.bestEstimateValue;
                } else if (NodeSelectionStartegyEnum.LOWEST_SUM_INFEASIBILITY_FIRST.equals(strategyEnum ) && bestReamining_metric > ccaNode.sumOfIntegerInfeasibilities) {
                    result = ccaNode;
                    bestReamining_metric=ccaNode.sumOfIntegerInfeasibilities;
                }
            }else {
                if (STRICT_BEST_FIRST.equals(strategyEnum) &&  bestReamining_metric>  ccaNode.lpRelaxationValue) {
                    result = ccaNode;
                    bestReamining_metric=ccaNode.lpRelaxationValue;
                }else if ( NodeSelectionStartegyEnum.BEST_ESTIMATE_FIRST .equals(strategyEnum) &&  bestReamining_metric>  ccaNode.bestEstimateValue) {
                    result = ccaNode;
                    bestReamining_metric=ccaNode.bestEstimateValue;
                } else if (NodeSelectionStartegyEnum.LOWEST_SUM_INFEASIBILITY_FIRST.equals(strategyEnum ) && bestReamining_metric > ccaNode.sumOfIntegerInfeasibilities) {
                    result = ccaNode;
                    bestReamining_metric=ccaNode.sumOfIntegerInfeasibilities;
                }
            }
            
        }
        
        return result;
    }
    
    //remove cca node from raw node list and promote it into an active subtree.
    private ActiveSubtree promoteCCANodeIntoActiveSubtree (CCANode ccaNode, boolean useBranch) throws Exception{
        ActiveSubtree activeSubtree  = new ActiveSubtree () ;
        activeSubtree.mergeVarBounds(ccaNode,  this.instructionsFromOriginalMIP, useBranch);  
        activeSubTreeList.add(activeSubtree);      
        this.rawNodeList.remove( ccaNode);
        logger.debug ("promoted raw node "+ ccaNode.nodeID +" into tree"+ activeSubtree.guid) ;
        return activeSubtree;
    }
   
     
}
