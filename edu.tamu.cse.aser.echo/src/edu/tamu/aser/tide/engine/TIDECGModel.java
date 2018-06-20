/**
 * This file is licensed under the University of Illinois/NCSA Open Source License. See LICENSE.TXT for details.
 */
package edu.tamu.aser.tide.engine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

import com.ibm.wala.cast.ipa.callgraph.AstCallGraph;
import com.ibm.wala.cast.ipa.callgraph.AstCallGraph.AstCGNode;
import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.eclipse.cg.model.WalaProjectCGModel;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.DefaultEntrypoint;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PropagationGraph;
import com.ibm.wala.ipa.callgraph.propagation.PropagationSystem;
import com.ibm.wala.ipa.callgraph.propagation.SSAPropagationCallGraphBuilder;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAArrayReferenceInstruction;
import com.ibm.wala.ssa.SSAFieldAccessInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.graph.InferGraphRoots;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import edu.tamu.aser.tide.akkasys.BugHub;
import edu.tamu.aser.tide.marker.BugMarker;
import edu.tamu.aser.tide.nodes.DLockNode;
import edu.tamu.aser.tide.nodes.INode;
import edu.tamu.aser.tide.nodes.MemNode;
import edu.tamu.aser.tide.nodes.MethodNode;
import edu.tamu.aser.tide.nodes.ReadNode;
import edu.tamu.aser.tide.nodes.StartNode;
import edu.tamu.aser.tide.nodes.SyncNode;
import edu.tamu.aser.tide.nodes.WriteNode;
import edu.tamu.aser.tide.plugin.handlers.ConvertHandler;
import edu.tamu.aser.tide.shb.SHBEdge;
import edu.tamu.aser.tide.shb.SHBGraph;
import edu.tamu.aser.tide.tests.Test;
import edu.tamu.aser.tide.views.EchoDLView;
import edu.tamu.aser.tide.views.EchoRaceView;
import edu.tamu.aser.tide.views.EchoReadWriteView;

public class TIDECGModel extends WalaProjectCGModel {

	public AnalysisCache getCache() {
		return engine.getCache();
	}
	public AnalysisOptions getOptions() {
		return engine.getOptions();
	}
	private String entrySignature;
	private Iterable<Entrypoint> entryPoints;

	//start bug akka system
	int nrOfWorkers = 1;
	public ActorSystem akkasys;
	public ActorRef bughub;
	public static TIDEEngine bugEngine;
	private static ClassLoader akkaClassLoader = ActorSystem.class.getClassLoader();
	private final static boolean DEBUG = false;
	//Eclipse views
	public EchoRaceView echoRaceView;
	public EchoDLView echoDLView;
	public EchoReadWriteView echoRWView;
	private HashMap<String, HashSet<IMarker>> bug_marker_map = new HashMap<>();


	public TIDECGModel(IJavaProject project, String exclusionsFile, String mainMethodSignature) throws IOException, CoreException {
		super(project, exclusionsFile);
		this.entrySignature = mainMethodSignature;
		echoRaceView = (EchoRaceView) PlatformUI.getWorkbench().getActiveWorkbenchWindow()
				.getActivePage().showView("edu.tamu.aser.tide.views.echoraceview");
		echoDLView = (EchoDLView) PlatformUI.getWorkbench().getActiveWorkbenchWindow()
				.getActivePage().showView("edu.tamu.aser.tide.views.echodlview");
		echoRWView = (EchoReadWriteView) PlatformUI.getWorkbench().getActiveWorkbenchWindow()
				.getActivePage().showView("edu.tamu.aser.tide.views.echotableview");
	}

	public EchoRaceView getEchoRaceView(){
		return echoRaceView;
	}

	public EchoReadWriteView getEchoRWView(){
		return echoRWView;
	}

	public EchoDLView getEchoDLView(){
		return echoDLView;
	}

	public TIDEEngine getEngine(){
		return bugEngine;
	}

	public HashSet<ITIDEBug> detectBug() {
	    Thread.currentThread().setContextClassLoader(akkaClassLoader);
		akkasys = ActorSystem.create("bug");
		CallGraphBuilder builder = engine.builder_echo;
		PropagationGraph flowgraph = null;
		if(builder instanceof SSAPropagationCallGraphBuilder){
			flowgraph = ((SSAPropagationCallGraphBuilder) builder).getPropagationSystem().getPropagationGraph();
		}
		//initial bug engine
		bughub = akkasys.actorOf(Props.create(BugHub.class, nrOfWorkers), "bughub");
		bugEngine = new TIDEEngine(entrySignature, callGraph, flowgraph, engine.getPointerAnalysis(), bughub);
		//detect bug
		return bugEngine.detectBothBugs(null);
	}


	public void updateGUI(IJavaProject project, IFile file, Set<ITIDEBug> bugs, boolean initial) {
		try{
			if(initial){
				//remove all markers in previous checks
				IMarker[] markers0 = project.getResource().findMarkers(BugMarker.TYPE_SCARIEST, true, 3);
				IMarker[] markers1 = project.getResource().findMarkers(BugMarker.TYPE_SCARY, true, 3);
				for (IMarker marker : markers0) {
					marker.delete();
				}
				for (IMarker marker : markers1) {
					marker.delete();
				}
				//create new markers
				IPath fullPath = file.getProject().getFullPath();//full path of the project
				if(bugs.isEmpty())
					System.err.println(" _________________NO BUGS ________________");

				HashSet<String> alertAccesses = new HashSet<String>();
				for(ITIDEBug bug:bugs){
					if(bug instanceof TIDERace) {
						showRace(fullPath, (TIDERace) bug);
						if (!alertAccesses.contains(((TIDERace) bug).node1.getSig())) {
							System.out.println(((TIDERace) bug).node1.getSig());
							alertAccesses.add(((TIDERace) bug).node1.getSig());
						}
						if (!alertAccesses.contains(((TIDERace) bug).node2.getSig())) {
							System.out.println(((TIDERace) bug).node2.getSig());
							alertAccesses.add(((TIDERace) bug).node2.getSig());
						}
					} else
						showDeadlock(fullPath,(TIDEDeadlock) bug);
				}
				System.out.println("-----------ALL BUGGY MEMORY ACCESSES: " + alertAccesses.size());
				initialEchoView(bugs);
			}
		}catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void initialEchoView(Set<ITIDEBug> bugs) {
		new Thread(new Runnable() {
			public void run() {
				while (true) {
					try { Thread.sleep(10);} catch (Exception e) {System.err.println(e);}
					Display.getDefault().asyncExec(new Runnable() {
						public void run() {
							echoRaceView.initialGUI(bugs);
						}
					});
					break;
				}
			}
		}).start();
		new Thread(new Runnable() {
			public void run() {
				while (true) {
					try { Thread.sleep(10);} catch (Exception e) {System.err.println(e);}
					Display.getDefault().asyncExec(new Runnable() {
						public void run() {
							echoDLView.initialGUI(bugs);
						}
					});
					break;
				}
			}
		}).start();
		new Thread(new Runnable() {
			public void run() {
				while (true) {
					try { Thread.sleep(10);} catch (Exception e) {System.err.println(e);}
					Display.getDefault().asyncExec(new Runnable() {
						public void run() {
							echoRWView.initialGUI(bugs);
						}
					});
					break;
				}
			}
		}).start();
	}

	private void showDeadlock(IPath fullPath, TIDEDeadlock bug) throws CoreException {
		DLockNode l11 = bug.lp1.lock1;
		DLockNode l12 = bug.lp1.lock2;
		DLockNode l21 = bug.lp2.lock1;
		DLockNode l22 = bug.lp2.lock2;

		String sig11 = l11.getInstSig();
		String sig12 = l12.getInstSig();
		String sig21 = l21.getInstSig();
		String sig22 = l22.getInstSig();

		int line11 = l11.getLine();
		int line12 = l12.getLine();
		int line21 = l21.getLine();
		int line22 = l22.getLine();

		String deadlockMsg = "Deadlock: ("+sig11  +" => "+sig12+ ";  "+sig21+ " => "+sig22+ ")";
//		System.err.println(deadlockMsg);
		ArrayList<LinkedList<String>> traceMsg = obtainTraceOfDeadlock(bug);
		bug.setBugInfo(deadlockMsg, traceMsg, null);

		IMarker marker1 = null;
		IMarker marker2 = null;
		IMarker marker3 = null;
		IMarker marker4 = null;
		IFile file11 = l11.getFile();
		if(file11 == null){
			marker1 = getFileFromSigDL(fullPath,sig11,traceMsg.get(0), line11, deadlockMsg);
		}else{
			marker1 = createMarkerDL(file11,line11,deadlockMsg);
		}
		IFile file12 = l12.getFile();
		if(file12 == null){
			marker2 = getFileFromSigDL(fullPath,sig12,traceMsg.get(1), line12, deadlockMsg);
		}else{
			marker2 = createMarkerDL(file12,line12,deadlockMsg);
		}
		IFile file21 = l21.getFile();
		if(file21 == null){
			marker3 = getFileFromSigDL(fullPath,sig21,traceMsg.get(2), line21, deadlockMsg);
		}else{
			marker3 = createMarkerDL(file21,line21,deadlockMsg);
		}
		IFile file22 = l22.getFile();
		if(file22 == null){
			marker4 = getFileFromSigDL(fullPath,sig22,traceMsg.get(3), line22, deadlockMsg);
		}else{
			marker4 = createMarkerDL(file22,line22,deadlockMsg);
		}

		//store bug -> markers
		HashSet<IMarker> newMarkers = new HashSet<>();
		newMarkers.add(marker1);
		newMarkers.add(marker2);
		newMarkers.add(marker3);
		newMarkers.add(marker4);
		bug_marker_map.put(deadlockMsg, newMarkers);
	}

	private void showRace(IPath fullPath, TIDERace race) throws CoreException {
		String sig = race.sig;
		MemNode rnode = race.node1;
		MemNode wnode = race.node2;
		int findex = sig.indexOf('.');
		int lindex = sig.lastIndexOf('.');
		if(findex!=lindex)
			sig =sig.substring(0, lindex);//remove instance hashcode

		String raceMsg = "Race: "+sig+" ("+rnode.getSig()+", "+wnode.getSig()+")";
//		System.err.println(raceMsg + rnode.getObjSig().toString());
		ArrayList<LinkedList<String>> traceMsg = obtainTraceOfRace(race);
		race.setBugInfo(raceMsg, traceMsg, null);

		IMarker marker1 = null;
		IFile file1 = rnode.getFile();
		if(file1 == null){
			marker1 = getFileFromSigRace(fullPath,rnode.getSig(),traceMsg.get(0), rnode.getLine(), raceMsg);
		}else{
			marker1 = createMarkerRace(file1, rnode.getLine(), raceMsg);
		}

		IMarker marker2 = null;
		IFile file2 = wnode.getFile();
		if(file2 == null){
			marker2 = getFileFromSigRace(fullPath,wnode.getSig(),traceMsg.get(1), wnode.getLine(), raceMsg);
		}else{
			marker2 = createMarkerRace(file2, wnode.getLine(), raceMsg);
		}

		//store bug -> markers
		HashSet<IMarker> newMarkers = new HashSet<>();
		newMarkers.add(marker1);
		newMarkers.add(marker2);
		bug_marker_map.put(raceMsg, newMarkers);
	}

	private ArrayList<LinkedList<String>> obtainTraceOfRace(TIDERace race) {
		MemNode rw1 = race.node1;
		MemNode rw2 = race.node2;
		int tid1 = race.tid1;
		int tid2 = race.tid2;
		ArrayList<LinkedList<String>> traces = new ArrayList<>();
		LinkedList<String> trace1 = obtainTraceOfINode(tid1, rw1, race, 1);
		LinkedList<String> trace2 = obtainTraceOfINode(tid2, rw2, race, 2);
		traces.add(trace1);
		traces.add(trace2);
		return traces;
	}

	private ArrayList<LinkedList<String>> obtainTraceOfDeadlock(TIDEDeadlock bug) {
		DLockNode l11 = bug.lp1.lock1;//1
		DLockNode l12 = bug.lp1.lock2;
		DLockNode l21 = bug.lp2.lock1;//1
		DLockNode l22 = bug.lp2.lock2;
		int tid1 = bug.tid1;
		int tid2 = bug.tid2;
		ArrayList<LinkedList<String>> traces = new ArrayList<>();
		LinkedList<String> trace1 = obtainTraceOfINode(tid1, l11, bug, 1);
		trace1.add("   =>");
//		trace1.add(l12.toString());
		//writeDownMyInfo(trace1, l12, bug);
		String sub12 = l12.toString();
		IFile file12 = l12.getFile();
		int line12 = l12.getLine();
		trace1.addLast(sub12);
		bug.addEventIFileToMap(sub12, file12);
		bug.addEventLineToMap(sub12, line12);

		LinkedList<String> trace2 = obtainTraceOfINode(tid2, l21, bug, 2);
		trace2.add("   =>");
//		trace2.add(l22.toString());
		//writeDownMyInfo(trace2, l22, bug);
		String sub22 = l22.toString();
		IFile file22 = l22.getFile();
		int line22 = l22.getLine();
		trace2.addLast(sub22);
		bug.addEventIFileToMap(sub22, file22);
		bug.addEventLineToMap(sub22, line22);

		traces.add(trace1);
		traces.add(trace2);
		return traces;
	}


	private LinkedList<String> obtainTraceOfINode(int tid, INode rw1, ITIDEBug bug, int idx) {
		LinkedList<String> trace = new LinkedList<>();
		HashSet<CGNode> traversed = new HashSet<>();
		TIDEEngine engine;
		if(DEBUG){
			engine = Test.engine;
		}else{
			engine = TIDECGModel.bugEngine;
		}
		SHBGraph shb = engine.shb;
		writeDownMyInfo(trace, rw1, bug);
		CGNode node = rw1.getBelonging();
		SHBEdge edge = shb.getIncomingEdgeWithTidForShowTrace(node, tid);
		traversed.add(node);
		INode parent = null;
		if(edge == null){
			StartNode startNode = engine.mapOfStartNode.get(tid);
			if(startNode != null){
				parent = startNode;
				tid = startNode.getParentTID();
			}else{
				return trace;
			}
		}else{
			parent = edge.getSource();
		}
		while(parent != null){
			writeDownMyInfo(trace, parent, bug);
			CGNode node_temp = parent.getBelonging();
			if(node_temp != null){
				//this is a kid thread start node
				if(!traversed.contains(node_temp)){
					traversed.add(node_temp);
					node = node_temp;
					edge = shb.getIncomingEdgeWithTidForShowTrace(node, tid);
					if(edge == null){
						//run method => node
						if(node_temp.getMethod().getName().toString().contains("run")){
							StartNode startNode = engine.mapOfStartNode.get(tid);
							tid = startNode.getParentTID();
							edge = shb.getIncomingEdgeWithTidForShowTrace(node, tid);
							if(edge == null){
								break;
							}
						}else
							break;
					}
					parent = edge.getSource();
					if(parent instanceof StartNode){
						tid = ((StartNode) parent).getParentTID();
					}
				}else{//recursive calls
					boolean found = false;
					HashSet<SHBEdge> edges = shb.getAllIncomingEdgeWithTid(node, tid);
					for (SHBEdge edge0 : edges) {
						if(!edge.equals(edge0)){
							parent = edge0.getSource();
							node_temp = parent.getBelonging();
							if(!traversed.contains(node_temp)){
								if(parent instanceof StartNode){
									tid = ((StartNode) parent).getParentTID();
								}
								found = true;
								break;
							}
						}
					}
					if(!found){
						parent = null;
					}
				}
			}else
				break;
		}
		return trace;
	}


	private boolean writeDownMyInfo(LinkedList<String> trace, INode node, ITIDEBug bug){
		String sub = null;
		IFile file = null;
		int line = 0;
		if(node instanceof ReadNode){
			sub = ((ReadNode)node).toString();
			file = ((ReadNode)node).getFile();
			line = ((ReadNode)node).getLine();
		}else if(node instanceof WriteNode){
			sub = ((WriteNode)node).toString();
			file = ((WriteNode)node).getFile();
			line = ((WriteNode)node).getLine();
		}else if(node instanceof SyncNode){
			sub = ((SyncNode)node).toString();
			file = ((SyncNode)node).getFile();
			line = ((SyncNode)node).getLine();
		}else if(node instanceof MethodNode){
			sub = ((MethodNode) node).toString();
			file = ((MethodNode) node).getFile();
			line = ((MethodNode) node).getLine();
		}else if(node instanceof StartNode){
			sub = ((StartNode) node).toString();
			file = ((StartNode) node).getFile();
			line = ((StartNode) node).getLine();
		}else{
			sub = node.toString();
		}
		trace.addFirst(sub);
		bug.addEventIFileToMap(sub, file);
		bug.addEventLineToMap(sub, line);
		return true;
	}



	private IMarker createMarkerRace(IFile file, int line, String msg) throws CoreException {
		//for race markers
		Map<String, Object> attributes = new HashMap<String, Object>();
		attributes.put(IMarker.LINE_NUMBER, line);
		attributes.put(IMarker.MESSAGE, msg);
		IMarker newMarker = file.createMarker(BugMarker.TYPE_SCARIEST);
		newMarker.setAttributes(attributes);
		return newMarker;
	}

	private IMarker createMarkerDL(IFile file, int line, String msg) throws CoreException{
		//for deadlock markers
		Map<String, Object> attributes = new HashMap<String, Object>();
		attributes.put(IMarker.LINE_NUMBER, line);
		attributes.put(IMarker.MESSAGE,msg);
		IMarker newMarker = file.createMarker(BugMarker.TYPE_SCARY);
		newMarker.setAttributes(attributes);
		return newMarker;
	}

	private IMarker getFileFromSigRace(IPath fullPath, String sig, LinkedList<String> trace, int line, String msg) throws CoreException{//":"
		if(sig.contains("java/util/")){
			Object[] infos = trace.toArray();
			for (int i = infos.length -1; i >= 0; i--) {
				String info = (String) infos[i];
				if(!info.contains("java/util/")){
					String need = (String) infos[i+1];
					int idx_start = need.lastIndexOf("from ") + 5;
					int idx_end = need.lastIndexOf(".");
					sig = need.substring(idx_start, idx_end);
					int l_start = need.lastIndexOf("(line ") + 6;
					int l_end = need.lastIndexOf(")");
					String l_str = need.substring(l_start, l_end);
					line = Integer.parseInt(l_str);
					break;
				}
			}
		}
		String name = sig;
		if(sig.contains(":"))
			name = sig.substring(0,sig.indexOf(':'));
		if(name.contains("$"))
			name=name.substring(0, name.indexOf("$"));
		name=name+".java";

		IPath path = fullPath.append("src/").append(name);
		IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(path);

		return createMarkerRace(file, line, msg);
	}

	private IMarker getFileFromSigDL(IPath fullPath, String sig, LinkedList<String> trace, int line, String msg) throws CoreException{//":"
		if(sig.contains("java/util/")){
			Object[] infos = trace.toArray();
			for (int i = infos.length -1; i >= 0; i--) {
				String info = (String) infos[i];
				if(!info.contains("java/util/")){
					String need = (String) infos[i+1];
					int idx_start = need.lastIndexOf(" ") + 1;
					int idx_end = need.lastIndexOf(".");
					sig = need.substring(idx_start, idx_end);
					break;
				}
			}
		}
		String name = sig;
		if(sig.contains(":"))
			name = sig.substring(0,sig.indexOf(':'));
		if(name.contains("$"))
			name=name.substring(0, name.indexOf("$"));
		name=name+".java";

		IPath path = fullPath.append("src/").append(name);
		//L/ProducerConsumer/src/pc/Consumer.java
		IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(path);

		return createMarkerDL(file, line, msg);
	}

	@Override
	protected Iterable<Entrypoint> getEntrypoints(AnalysisScope analysisScope, IClassHierarchy classHierarchy) {
		if(entryPoints==null){
			entryPoints = findEntryPoints(classHierarchy,entrySignature);
		}
		return entryPoints;
	}

	public Iterable<Entrypoint> findEntryPoints(IClassHierarchy classHierarchy, String entrySignature) {
		final Set<Entrypoint> result = HashSetFactory.make();
		Iterator<IClass> classIterator = classHierarchy.iterator();
		while (classIterator.hasNext()) {
			IClass klass = classIterator.next();
			if (!AnalysisUtils.isJDKClass(klass)) {
				for (IMethod method : klass.getDeclaredMethods()) {
//					System.out.println(klass.toString()+ "   " + method.toString());
					try {
						if(method.isStatic()&&method.isPublic()
								&&method.getName().toString().equals("main")
								&&method.getDescriptor().toString().equals(ConvertHandler.DESC_MAIN)){
							result.add(new DefaultEntrypoint(method, classHierarchy));
						}else if(method.isPublic()&&!method.isStatic()
								&&method.getName().toString().equals("run")
								&&method.getDescriptor().toString().equals("()V")){
							if (AnalysisUtils.implementsRunnableInterface(klass) || AnalysisUtils.extendsThreadClass(klass))
							result.add(new DefaultEntrypoint(method, classHierarchy));
						}
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
			}
		}

		return new Iterable<Entrypoint>() {
			public Iterator<Entrypoint> iterator() {
				return result.iterator();
			}
		};

	}

	@Override
	protected Collection<CGNode> inferRoots(CallGraph cg) throws WalaException {
		return InferGraphRoots.inferRoots(cg);
	}

	@SuppressWarnings("rawtypes")
	public PointerAnalysis getPointerAnalysis() {
		return engine.getPointerAnalysis();
	}

	public IClassHierarchy getClassHierarchy() {
		return engine.getClassHierarchy();
	}

	public CGNode getOldCGNode(com.ibm.wala.classLoader.IMethod m_old){
		CGNode node = null;
		AstCallGraph cg = (AstCallGraph)callGraph;
		try {
			node = cg.findOrCreateNode(m_old, Everywhere.EVERYWHERE);
		} catch (CancelException e) {
		}
		return node;
	}

	public CGNode updateCallGraph(com.ibm.wala.classLoader.IMethod m_old, com.ibm.wala.classLoader.IMethod m, IR ir) {
		CGNode node = null;
		try{
			AstCallGraph cg = (AstCallGraph)callGraph;
			CGNode oldNode = cg.findOrCreateNode(m_old, Everywhere.EVERYWHERE);
			if(oldNode instanceof AstCGNode){
				AstCGNode astnode = (AstCGNode) oldNode;
				astnode.updateMethod(m, ir);
				//update call graph key
				cg.updateNode(m_old, m, Everywhere.EVERYWHERE, astnode);
				//update call site?
				astnode.clearAllTargets();//clear old targets
				if(engine.builder_echo!=null &&
						engine.builder_echo instanceof SSAPropagationCallGraphBuilder){
					SSAPropagationCallGraphBuilder builder = (SSAPropagationCallGraphBuilder) engine.builder_echo;
					builder.system.setUpdateChange(true);
					builder.addConstraintsFromChangedNode(astnode, null);
					PropagationSystem system = builder.system;
					do{
						system.solve(null);
					}while(!system.emptyWorkList());
					builder.system.setUpdateChange(false);
				}
			}
			node = oldNode;

		}catch(Exception e)
		{
			e.printStackTrace();
		}
		return node;
	}



	public void updatePointerAnalysis(CGNode node, IR ir_old, IR ir) {
    	//compute diff
    	SSAInstruction[] insts_old = ir_old.getInstructions();
    	SSAInstruction[] insts = ir.getInstructions();

    	HashMap<String,SSAInstruction> mapOld = new HashMap<String,SSAInstruction>();
    	HashMap<String,SSAInstruction> mapNew = new HashMap<String,SSAInstruction>();

        ControlFlowGraph<SSAInstruction, ISSABasicBlock> cfg_old = ir_old.getControlFlowGraph();
        ControlFlowGraph<SSAInstruction, ISSABasicBlock> cfg_new = ir.getControlFlowGraph();

    	for(int i=0;i<insts_old.length;i++){
    		SSAInstruction inst = insts_old[i];
    		if(inst!=null){
    			String str = inst.toString();
    			//TODO: JEFF  -- program counter may change, call graph
//    			if(str.indexOf('@')>0)
//    				str = str.substring(0,str.indexOf('@')-1);
    			mapOld.put(str, inst);
    		}
    	}
    	for(int i=0;i<insts.length;i++) {
    		SSAInstruction inst = insts[i];
    		if(inst!=null){
    			String str = inst.toString();
    			//TODO: JEFF
//    			if(str.indexOf('@')>0)
//    				str = str.substring(0,str.indexOf('@')-1);
    			mapNew.put(str, inst);
    		}
    	}

    	HashMap<SSAInstruction,ISSABasicBlock> deleted = new HashMap<SSAInstruction,ISSABasicBlock>();
    	HashMap<SSAInstruction,ISSABasicBlock> added = new HashMap<SSAInstruction,ISSABasicBlock>();

    	for(String s:mapOld.keySet()){
    		if(!mapNew.keySet().contains(s)){//removed from new
    			SSAInstruction inst = mapOld.get(s);
    			if(inst instanceof SSAFieldAccessInstruction
    					|| inst instanceof SSAAbstractInvokeInstruction
    					|| inst instanceof SSAArrayReferenceInstruction){
        			ISSABasicBlock bb = cfg_old.getBlockForInstruction(inst.iindex);
        			deleted.put(inst,bb);
    			}
    		}
    	}
    	for(String s:mapNew.keySet()){
    		if(!mapOld.keySet().contains(s)){//removed from new
    			SSAInstruction inst = mapNew.get(s);
    			ISSABasicBlock bb = cfg_new.getBlockForInstruction(inst.iindex);
    			added.put(inst,bb);
    		}
    	}

		engine.updatePointerAnalaysis(node, added,deleted,ir_old, ir);
	}

	public void clearChanges() {
		((SSAPropagationCallGraphBuilder) engine.builder_echo).system.clearChanges();
	}


}
