package com.ibm.wala.cast.java.client.impl;

import com.ibm.wala.cast.java.ipa.callgraph.AstJavaZeroXCFABuilder;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.impl.DefaultContextSelector;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.cfa.OneLevelSiteContextSelector;
import com.ibm.wala.ipa.callgraph.propagation.cfa.ZeroXInstanceKeys;
import com.ibm.wala.ipa.cha.IClassHierarchy;

/**
 *
 * A factory to create call graph builders using 0-CFA
 */
public class ZeroCFABuilderFactory  {

  public CallGraphBuilder make(AnalysisOptions options, AnalysisCache cache, IClassHierarchy cha, AnalysisScope scope, boolean keepPointsTo) {
    Util.addDefaultSelectors(options, cha);
    Util.addDefaultBypassLogic(options, scope, Util.class.getClassLoader(), cha);
//    ContextSelector defaultContext = new DefaultContextSelector(options, cha);
//    ContextSelector oneCallSite = new OneLevelSiteContextSelector(defaultContext);
    return new AstJavaZeroXCFABuilder(cha, options, cache, null, null, ZeroXInstanceKeys.ALLOCATIONS  | ZeroXInstanceKeys.SMUSH_MANY | ZeroXInstanceKeys.SMUSH_THROWABLES);//JEFF ZeroXInstanceKeys.ALLOCATIONS ZeroXInstanceKeys.NONE
  }
}
