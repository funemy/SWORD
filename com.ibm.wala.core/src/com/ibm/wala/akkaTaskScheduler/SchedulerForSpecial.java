/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.akkaTaskScheduler;

import java.util.ArrayList;

import com.ibm.wala.ipa.callgraph.propagation.PointsToSetVariable;
import com.ibm.wala.ipa.callgraph.propagation.PropagationSystem;
import com.ibm.wala.util.intset.MutableIntSet;

public class SchedulerForSpecial{
  private final ArrayList<PointsToSetVariable> lhss;
  private final boolean isAddition;
  private final MutableIntSet targets;
  private PropagationSystem system;

  public SchedulerForSpecial(final ArrayList<PointsToSetVariable> lhss,
      final MutableIntSet targets, final boolean isAddition, PropagationSystem propagationSystem){
    this.lhss = lhss;
    this.targets = targets;
    this.isAddition = isAddition;
    this.system = propagationSystem;
  }

  public PropagationSystem getPropagationSystem (){
    return system;
  }

  public ArrayList<PointsToSetVariable> getLhss(){
    return lhss;
  }

  public MutableIntSet getTargets(){
    return targets;
  }

  public boolean getIsAddition(){
    return isAddition;
  }


}
