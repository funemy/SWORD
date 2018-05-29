/*******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.ipa.callgraph.propagation;


import java.util.Map;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;

/**
 * standard fixed-point iterative solver for pointer analysis
 */
public class StandardSolver extends AbstractPointsToSolver {

  private static final boolean DEBUG_PHASES = DEBUG || false;

  public StandardSolver(PropagationSystem system, PropagationCallGraphBuilder builder) {
    super(system, builder);
  }

  @Override
  public void solve(CGNode node, Map added, Map deleted)
  {
    final PropagationSystem system = getSystem();
    final PropagationCallGraphBuilder builder = getBuilder();
    try {

      for(Object key: deleted.keySet()){
        SSAInstruction diff = (SSAInstruction)key;
        ISSABasicBlock bb = (ISSABasicBlock)deleted.get(key);

        system.setFirstDel(true);
        builder.setDelete(true);
        builder.processDiff(node,bb,diff);//only for those affecting data flow facts
        system.setFirstDel(false);

        system.solveDel(null);

      }
      //added instructions
      builder.setDelete(false);//add
      builder.addConstraintsFromChangedNode(node, null);
    do{
        system.solve(null);
      builder.addConstraintsFromNewNodes(null);
    } while (!system.emptyWorkList());

    } catch (CancelException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  /*
   * @see com.ibm.wala.ipa.callgraph.propagation.IPointsToSolver#solve()
   */
  @Override
  public void solve(IProgressMonitor monitor) throws IllegalArgumentException, CancelException {
    int i = 0;
    final PropagationSystem system = getSystem();
    final PropagationCallGraphBuilder builder = getBuilder();


    do {
      i++;

      if (DEBUG_PHASES) {
        System.err.println("Iteration " + i);
      }

      system.solve(monitor);

      if (DEBUG_PHASES) {
        System.err.println("Solved " + i);
      }

      if (builder.getOptions().getMaxNumberOfNodes() > -1) {
        if (builder.getCallGraph().getNumberOfNodes() >= builder.getOptions().getMaxNumberOfNodes()) {
          if (DEBUG) {
            System.err.println("Bail out from call graph limit" + i);
          }
          throw CancelException.make("reached call graph size limit");
        }
      }

      // Add constraints until there are no new discovered nodes
      if (DEBUG_PHASES) {
        System.err.println("adding constraints");
      }

      builder.addConstraintsFromNewNodes(monitor);

      // getBuilder().callGraph.summarizeByPackage();

      if (DEBUG_PHASES) {
        System.err.println("handling reflection");
      }

      if (i <= builder.getOptions().getReflectionOptions().getNumFlowToCastIterations()) {
        getReflectionHandler().updateForReflection(monitor);
      }

      // Handling reflection may have discovered new nodes!
      // Therefore we need to add constraints twice
      if (DEBUG_PHASES) {
        System.err.println("adding constraints again");
      }

      builder.addConstraintsFromNewNodes(monitor);


      if (monitor != null) { monitor.worked(i); }

      // Note that we may have added stuff to the
      // worklist; so,
    } while (!system.emptyWorkList());

  }

  // return points to map, make it accessable for users
  public PointsToMap getPointsToMap(){
    return getSystem().pointsToMap;
  }

}
