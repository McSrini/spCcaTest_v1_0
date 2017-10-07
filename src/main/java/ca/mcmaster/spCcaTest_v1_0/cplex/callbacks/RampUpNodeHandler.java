/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spCcaTest_v1_0.cplex.callbacks;

import static ca.mcmaster.spCcaTest_v1_0.Constants.*;
import ca.mcmaster.spCcaTest_v1_0.cplex.datatypes.NodeAttachment;
import ilog.concert.IloException;
import ilog.cplex.IloCplex;
import static java.lang.System.exit;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

/**
 *
 * @author tamvadss
 */
public class RampUpNodeHandler extends IloCplex.NodeCallback {

    public long activeLeafCount=ONE;
    public long activeLeafCount_LIMIT=PLUS_INFINITY;
         
    protected void main() throws IloException {
        if(getNremainingNodes64()> ZERO){
            
            activeLeafCount= getNremainingNodes64();    
            if (activeLeafCount >= activeLeafCount_LIMIT) {
                 
                abort();
            }
            
        }
    }
    
    public void setLeafCountLimit (long limit) {
        activeLeafCount_LIMIT= limit;
    }
    

}
