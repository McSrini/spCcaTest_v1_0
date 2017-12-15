/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spCcaTest_v1_0;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author tamvadss
 */
public class ConstantsAndParameters {
        
    public static final String EMPTY_STRING ="";
    public static final String MINUS_ONE_STRING = "-1";
    public static final int ZERO = 0;
    public static final double DOUBLE_ZERO = 0.0;
    public static final int ONE = 1;
    public static final int TWO = 2;    
    public static final int THREE = 3; 
    public static final int FOUR = 4;    
    public static final int FIVE = 5;  
    public static final int EIGHT = 8;  
    public static final int TEN = 10;  
    public static final int SIXTY = 60;  
    public static final int HUNDRED = 100 ;  
    public static final int THOUSAND = 1000 ;  
    public static final int MILLION = 1000000;  
    public static final long PLUS_INFINITY = Long.MAX_VALUE;
    public static final long MINUS_INFINITY = Long.MIN_VALUE;
    public static final double EPSILON = 0.0000000001;
    public static final String DELIMITER = "______";
    
    //public static  String MPS_FILE_ON_DISK =  "F:\\temporary files here\\rd-rplusc-21.mps";
    //public static  String MPS_FILE_ON_DISK =  "F:\\temporary files here\\atlanta-ip.mps"; //windows
    public static  String MPS_FILE_ON_DISK =  "";  //linux
    

    //public static final String LOG_FOLDER="F:\\temporary files here\\logs\\testing\\ccav1_3\\"; //windows
    public static final String LOG_FOLDER="logs/"; //linux
    public static final String LOG_FILE_EXTENSION = ".log";
    
     
    public static final boolean IS_MAXIMIZATION = false;
     
   
    //used to reject inferior LCA candidates
    public static final double LCA_CANDIDATE_PACKING_FACTOR_LARGEST_ACCEPTABLE=1.20;
    
    
    //CCA subtree allowed to have slightly less good leafs than asked for in NUM_LEAFS_FOR_MIGRATION_IN_CCA_SUBTREE 
    public static   double CCA_TOLERANCE_FRACTION =  0.00;
    public static  double CCA_PACKING_FACTOR_MAXIMUM_ALLOWED =  0.0;
    
     
    
    public static   int TIME_SLICE_IN_MINUTES_PER_ACTIVE_SUBTREE = 2;    
    public static   double MINIMUM_TIME_SLICE_IN_MINUTES_PER_ACTIVE_SUBTREE = 0.5 ;//30 seconds
    public static final int SOLUTION_CYCLE_TIME_MINUTES = 8;
    
     /*
     public static   String MIP_NAME_UNDER_TEST ="p6b";
    public static   int RAMP_UP_TO_THIS_MANY_LEAFS =200000;    
    public static  int NUM_PARTITIONS =5;
    */
     
    /*
    public static   String MIP_NAME_UNDER_TEST ="wnq";
    public static   int RAMP_UP_TO_THIS_MANY_LEAFS =50000;    
    public static  int NUM_PARTITIONS =5;
    */
    
    
    /*
    public static   String MIP_NAME_UNDER_TEST ="dg012142";
    public static   int RAMP_UP_TO_THIS_MANY_LEAFS =3000 ;    
    public static  int NUM_PARTITIONS =75;
     */
    
    /*
    public static   String MIP_NAME_UNDER_TEST ="d10200";
    public static   int RAMP_UP_TO_THIS_MANY_LEAFS =5000;    
    public static  int NUM_PARTITIONS =5;
    */
    
    
    /*
    public static   String MIP_NAME_UNDER_TEST ="neos-847302";
    public static   int RAMP_UP_TO_THIS_MANY_LEAFS = 8000;    
    public static  int NUM_PARTITIONS =800;
    */
    
    /*
    public static   String MIP_NAME_UNDER_TEST ="lrsa120";
    public static   int RAMP_UP_TO_THIS_MANY_LEAFS = 9000;    
    public static  int NUM_PARTITIONS =300;
    */
   
   
   
    /*
    public static   String MIP_NAME_UNDER_TEST ="set3-20";
    public static   int RAMP_UP_TO_THIS_MANY_LEAFS = 100000; 
    public static  int NUM_PARTITIONS =5;
    */
     
        
   /*
    public static   String MIP_NAME_UNDER_TEST ="probportfolio";
    public static   int RAMP_UP_TO_THIS_MANY_LEAFS = 500000 ;    
    public static  int NUM_PARTITIONS =5;
    */
   
     
    
     
    
    
    public static   String MIP_NAME_UNDER_TEST ="seymour-disj-10";
    public static   int RAMP_UP_TO_THIS_MANY_LEAFS = 300;    
    public static  int NUM_PARTITIONS =50;
    
 
    /*
    public static   String MIP_NAME_UNDER_TEST ="swath";
    public static   int RAMP_UP_TO_THIS_MANY_LEAFS = 100000; //1000000;    
    public static  int NUM_PARTITIONS =5;
     */
    
    /*
    public static   String MIP_NAME_UNDER_TEST ="r80x800";
    public static   int RAMP_UP_TO_THIS_MANY_LEAFS = 100000;    
    public static  int NUM_PARTITIONS =5;
    */
    
    /*
    public static   String MIP_NAME_UNDER_TEST ="neos-807456";
    public static   int RAMP_UP_TO_THIS_MANY_LEAFS = 2400;    
    public static  int NUM_PARTITIONS =600;
    */
    
  /*
    public static   String MIP_NAME_UNDER_TEST ="protfold";
    public static   int RAMP_UP_TO_THIS_MANY_LEAFS = 1500;    
    public static  int NUM_PARTITIONS =75;
*/
 
    
    public static double EXPECTED_LEAFS_PER_PARTITION = (RAMP_UP_TO_THIS_MANY_LEAFS +DOUBLE_ZERO)/NUM_PARTITIONS;
    
    //public static  double WORK_MEM =  1024;
    public static boolean  SAVE_TO_DISK_FLAG = true;
    
    public static int REPEAT_SBF_TEST_WITH_TIMESLICE_MULTIPLED_BY = 2 ; //leave at 1 to not repeat the test
    public static int MAX_ITERATIONS_LIMIT = 25000 ; //make this smaller say 25 when solving very complex MIPS like sct32
    public static int    ADDITIONAL_SOLUTION_CYCLES_ALLOWED_FOR_SBF_COMAPRED_TO_LCA =50;  //this value is used in the driver to limit iters taken by SBF
    
}
