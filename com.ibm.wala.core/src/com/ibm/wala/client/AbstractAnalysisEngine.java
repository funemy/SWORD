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
package com.ibm.wala.client;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.jar.JarFile;

import com.ibm.wala.analysis.pointers.BasicHeapGraph;
import com.ibm.wala.analysis.pointers.HeapGraph;
import com.ibm.wala.classLoader.ClassLoaderFactory;
import com.ibm.wala.classLoader.ClassLoaderFactoryImpl;
import com.ibm.wala.classLoader.JarFileModule;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.SSAPropagationCallGraphBuilder;
import com.ibm.wala.ipa.callgraph.propagation.SSAPropagationCallGraphBuilder.ConstraintVisitor;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.DefUse;
import com.ibm.wala.ssa.DefaultIRFactory;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.config.SetOfClasses;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.io.FileProvider;

/**
 * Abstract base class for analysis engine implementations
 *
 * Some clients choose to build on this, but many don't. I usually don't in new code; I usually don't find the re-use enabled by
 * this class compelling. I would probably nuke this except for some legacy code that uses it.
 */
public abstract class AbstractAnalysisEngine implements AnalysisEngine {

  public interface EntrypointBuilder {
    Iterable<Entrypoint> createEntrypoints(AnalysisScope scope, IClassHierarchy cha);
  }

  public final static String SYNTHETIC_J2SE_MODEL = "SyntheticJ2SEModel.txt";

  /**
   * DEBUG_LEVEL:
   * <ul>
   * <li>0 No output
   * <li>1 Print some simple stats and warning information
   * <li>2 Detailed debugging
   * </ul>
   */
  protected static final int DEBUG_LEVEL = 1;

  /**
   * Name of the file which holds the class hierarchy exclusions directives for this analysis.
   */
  private String exclusionsFile = "J2SEClassHierarchyExclusions.txt";

  /**
   * The modules to analyze
   */
  protected Collection<Module> moduleFiles;

  /**
   * A representation of the analysis scope
   */
  protected AnalysisScope scope;

  /**
   * A representation of the analysis options
   */
  private AnalysisOptions options;

  /**
   * A cache of IRs and stuff
   */
  private AnalysisCache cache = makeDefaultCache();

  /**
   * The standard J2SE libraries to analyze
   */
  protected Module[] j2seLibs;

  /**
   * Whether to perform closed-world analysis of an application
   */
  private boolean closedWorld = false;

  /**
   * Governing class hierarchy
   */
  private IClassHierarchy cha;

  /**
   * Governing call graph
   */
  protected CallGraph cg;

  /**
   * Results of pointer analysis
   */
  protected PointerAnalysis<InstanceKey> pointerAnalysis;

  /**
   * Graph view of flow of pointers between heap abstractions
   */
  private HeapGraph heapGraph;

  private EntrypointBuilder entrypointBuilder = new EntrypointBuilder() {
    @Override
    public Iterable<Entrypoint> createEntrypoints(AnalysisScope scope, IClassHierarchy cha) {
      return makeDefaultEntrypoints(scope, cha);
    }
  };

  protected abstract CallGraphBuilder getCallGraphBuilder(IClassHierarchy cha, AnalysisOptions options, AnalysisCache cache);

  protected CallGraphBuilder buildCallGraph(IClassHierarchy cha, AnalysisOptions options, boolean savePointerAnalysis,
      IProgressMonitor monitor) throws IllegalArgumentException, CancelException {

    CallGraphBuilder builder = getCallGraphBuilder(cha, options, cache);

    cg = builder.makeCallGraph(options, monitor);

    if (savePointerAnalysis) {
      pointerAnalysis = builder.getPointerAnalysis();
    }

    return builder;
  }

  @Override
  public void setModuleFiles(Collection moduleFiles) {
    this.moduleFiles = moduleFiles;
  }

  /**
   * Set up the AnalysisScope object
   *
   * @throws IOException
   */
  public void buildAnalysisScope() throws IOException {
    if (j2seLibs == null) {
      Assertions.UNREACHABLE("no j2selibs specified. You probably did not call AppAnalysisEngine.setJ2SELibrary.");
    }

    scope = AnalysisScopeReader.readJavaScope(SYNTHETIC_J2SE_MODEL, (new FileProvider()).getFile(getExclusionsFile()), getClass()
        .getClassLoader());

    // add standard libraries
    for (int i = 0; i < j2seLibs.length; i++) {
      scope.addToScope(scope.getPrimordialLoader(), j2seLibs[i]);
    }

    // add user stuff
    addApplicationModulesToScope();
  }


  /**
   * @return a IClassHierarchy object for this analysis scope
   */
  public IClassHierarchy buildClassHierarchy() {
    IClassHierarchy cha = null;
    ClassLoaderFactory factory = makeClassLoaderFactory(getScope().getExclusions());
    try {
      cha = ClassHierarchy.make(getScope(), factory);
    } catch (ClassHierarchyException e) {
      System.err.println("Class Hierarchy construction failed");
      System.err.println(e.toString());
      e.printStackTrace();
    }
    return cha;
  }

  protected ClassLoaderFactory makeClassLoaderFactory(SetOfClasses exclusions) {
    return new ClassLoaderFactoryImpl(exclusions);
   }

  public IClassHierarchy getClassHierarchy() {
    return cha;
  }

  protected void setClassHierarchy(IClassHierarchy cha) {
    this.cha = cha;
  }

  /**
   * @return Returns the call graph
   */
  public CallGraph getCallGraph() {
    return cg;
  }

  /**
   * Add the application modules to the analysis scope.
   */
  protected void addApplicationModulesToScope() {
    ClassLoaderReference app = scope.getApplicationLoader();
    for (Iterator it = moduleFiles.iterator(); it.hasNext();) {
      Object o = it.next();
      if (!(o instanceof Module)) {
        Assertions.UNREACHABLE("Unexpected type: " + o.getClass());
      }
      Module M = (Module) o;
      scope.addToScope(app, M);
    }
  }

  @Override
  public void setJ2SELibraries(JarFile[] libs) {
    if (libs == null) {
      throw new IllegalArgumentException("libs is null");
    }
    this.j2seLibs = new Module[libs.length];
    for (int i = 0; i < libs.length; i++) {
      j2seLibs[i] = new JarFileModule(libs[i]);
    }
  }

  @Override
  public void setJ2SELibraries(Module[] libs) {
    if (libs == null) {
      throw new IllegalArgumentException("libs is null");
    }
    this.j2seLibs = new Module[libs.length];
    for (int i = 0; i < libs.length; i++) {
      j2seLibs[i] = libs[i];
    }
  }

  @Override
  public void setClosedWorld(boolean b) {
    this.closedWorld = b;
  }

  public boolean isClosedWorld() {
    return closedWorld;
  }

  protected AnalysisScope getScope() {
    return scope;
  }

  public PointerAnalysis<InstanceKey> getPointerAnalysis() {
    return pointerAnalysis;
  }

  public HeapGraph getHeapGraph() {
    if (heapGraph == null) {
      heapGraph = new BasicHeapGraph(getPointerAnalysis(), cg);
    }
    return heapGraph;
  }

  public String getExclusionsFile() {
    return exclusionsFile;
  }

  public void setExclusionsFile(String exclusionsFile) {
    this.exclusionsFile = exclusionsFile;
  }

  @Override
  public AnalysisOptions getDefaultOptions(Iterable<Entrypoint> entrypoints) {
    return new AnalysisOptions(getScope(), entrypoints);
  }

  public AnalysisCache makeDefaultCache() {
    return new AnalysisCache(new DefaultIRFactory());
  }

  protected Iterable<Entrypoint> makeDefaultEntrypoints(AnalysisScope scope, IClassHierarchy cha) {
    return Util.makeMainEntrypoints(scope, cha);
  }

  public void setEntrypointBuilder(EntrypointBuilder builder) {
    entrypointBuilder = builder;
  }

  /**
   * Builds the call graph for the analysis scope in effect, using all of the given entry points.
   *
   * @throws CancelException
   * @throws IllegalArgumentException
   * @throws IOException
   */
  public CallGraphBuilder defaultCallGraphBuilder() throws IllegalArgumentException, CancelException, IOException {
    buildAnalysisScope();
    IClassHierarchy cha = buildClassHierarchy();
    setClassHierarchy(cha);
    Iterable<Entrypoint> eps = entrypointBuilder.createEntrypoints(scope, cha);
    options = getDefaultOptions(eps);
    cache = makeDefaultCache();
    return buildCallGraph(cha, options, true, null);
  }

  public CallGraphBuilder builder_echo;
  public CallGraph buildDefaultCallGraph() throws IllegalArgumentException, CancelException, IOException {
    builder_echo = defaultCallGraphBuilder();

    return builder_echo.getCallGraph();

  }

  public AnalysisCache getCache() {
    return cache;
  }

  public AnalysisOptions getOptions() {
    return options;
  }


  public void updatePointerAnalaysis(CGNode node, Map added, Map deleted, IR ir_old, IR ir) {
  //changed, only remove old
    if(builder_echo!=null && builder_echo instanceof SSAPropagationCallGraphBuilder){
      DefUse du_old = new DefUse(ir_old);
      ConstraintVisitor v_old = ((SSAPropagationCallGraphBuilder)builder_echo).makeVisitor(node);
      v_old.setIR(ir_old);
      v_old.setDefUse(du_old);
      DefUse du_new = new DefUse(ir);//should be ir new
      ConstraintVisitor v_new = ((SSAPropagationCallGraphBuilder)builder_echo).makeVisitor(node);
      v_new.setIR(ir);
      v_new.setDefUse(du_new);
      builder_echo.updatePointerAnalaysis(node, added, deleted, v_old, v_new);
    }
  }

}
