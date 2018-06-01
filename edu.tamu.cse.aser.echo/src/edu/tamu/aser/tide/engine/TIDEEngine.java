package edu.tamu.aser.tide.engine;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.eclipse.core.resources.IFile;

import com.ibm.wala.cast.java.loader.JavaSourceLoaderImpl.ConcreteJavaMethod;
import com.ibm.wala.cast.loader.AstMethod.DebuggingInformation;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IBytecodeMethod;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.IMethod.SourcePosition;
import com.ibm.wala.ide.util.JdtPosition;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.AllocationSiteInNode;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PropagationGraph;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAArrayLoadInstruction;
import com.ibm.wala.ssa.SSAArrayReferenceInstruction;
import com.ibm.wala.ssa.SSAArrayStoreInstruction;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSACFG.BasicBlock;
import com.ibm.wala.ssa.SSACheckCastInstruction;
import com.ibm.wala.ssa.SSAFieldAccessInstruction;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAMonitorInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.graph.dominators.NumberedDominators;
import com.ibm.wala.util.intset.IntSetUtil;
import com.ibm.wala.util.intset.MutableIntSet;
import com.ibm.wala.util.intset.OrdinalSet;

import akka.actor.ActorRef;
import edu.tamu.aser.tide.akkasys.BugHub;
import edu.tamu.aser.tide.akkasys.DistributeDatarace;
import edu.tamu.aser.tide.akkasys.DistributeDeadlock;
import edu.tamu.aser.tide.akkasys.FindSharedVariable;
import edu.tamu.aser.tide.akkasys.RemoveLocalVar;
import edu.tamu.aser.tide.nodes.DLPair;
import edu.tamu.aser.tide.nodes.DLockNode;
import edu.tamu.aser.tide.nodes.DUnlockNode;
import edu.tamu.aser.tide.nodes.INode;
import edu.tamu.aser.tide.nodes.JoinNode;
import edu.tamu.aser.tide.nodes.LockPair;
import edu.tamu.aser.tide.nodes.MemNode;
import edu.tamu.aser.tide.nodes.MethodNode;
import edu.tamu.aser.tide.nodes.ReadNode;
import edu.tamu.aser.tide.nodes.StartNode;
import edu.tamu.aser.tide.nodes.SyncNode;
import edu.tamu.aser.tide.nodes.WriteNode;
import edu.tamu.aser.tide.shb.SHBGraph;
import edu.tamu.aser.tide.shb.Trace;

public class TIDEEngine{

	//count the number of sigs from different traces: only for shared fields
	private HashMap<String, HashMap<Integer, Integer>> rsig_tid_num_map = new HashMap<>();
	private HashMap<String, HashMap<Integer, Integer>> wsig_tid_num_map = new HashMap<>();
	//record shared sigs and nodes
	public HashMap<String, HashSet<ReadNode>> sigReadNodes = new HashMap<String, HashSet<ReadNode>>();
	public HashMap<String, HashSet<WriteNode>> sigWriteNodes = new HashMap<String, HashSet<WriteNode>>();
	//record the ignored sigs by users
	public HashSet<String> excludedSigForRace = new HashSet<>();
	public HashMap<String, HashSet<ReadNode>> excludedReadSigMapping = new HashMap<>();
	public HashMap<String, HashSet<WriteNode>> excludedWriteSigMapping = new HashMap<>();
	//record the ignored function by users
	public HashSet<CGNode> excludedMethodForBugs = new HashSet<>();
	//to check isolates: only for testing
	public HashMap<CGNode, HashSet<CGNode>> excludedMethodIsolatedCGNodes = new HashMap<>();

	private LinkedList<CGNode> alreadyProcessedNodes = new LinkedList<CGNode>();
	private LinkedList<CGNode> twiceProcessedNodes = new LinkedList<CGNode>();
	private LinkedList<CGNode> thirdProcessedNodes = new LinkedList<CGNode>();
	private HashSet<CGNode> scheduledAstNodes = new HashSet<CGNode>();
	
	private LinkedList<CGNode> mainEntryNodes = new LinkedList<CGNode>();
	private LinkedList<CGNode> threadNodes = new LinkedList<CGNode>();

	private MutableIntSet stidpool = IntSetUtil.make();//start
	private HashMap<Integer, AstCGNode> dupStartJoinTidMap = new HashMap<>();//join with dup tid
	private HashMap<TypeName, CGNode> threadSigNodeMap = new HashMap<TypeName,CGNode>();

	private boolean hasSyncBetween = false;

	public HashMap<Integer, StartNode> mapOfStartNode = new HashMap<>();
	public HashMap<Integer, JoinNode> mapOfJoinNode = new HashMap<>();
	//lock pairs for deadlock
	public HashMap<Integer, ArrayList<DLPair>> threadDLLockPairs = new HashMap<Integer, ArrayList<DLPair>>();
	//currently locked objects
	public 	HashMap<Integer, HashSet<DLockNode>> threadLockNodes = new HashMap<Integer, HashSet<DLockNode>>();
	//node <-> since it's in a loop and we create an astnode
	public HashMap<CGNode, AstCGNode> n_loopn_map = new HashMap<>();

	private static HashMap<CGNode,Collection<Loop>> nodeLoops = new HashMap<CGNode,Collection<Loop>>();

	public CallGraph callGraph;
	public PointerAnalysis<InstanceKey> pointerAnalysis;
	protected PropagationGraph propagationGraph;
	private int maxGraphNodeID;

	public long timeForDetectingRaces = 0;
	public long timeForDetectingDL = 0;

	public HashSet<String> sharedFields = new HashSet<String>();
	public HashSet<ITIDEBug> bugs = new HashSet<ITIDEBug>();
	public HashSet<ITIDEBug> removedbugs = new HashSet<ITIDEBug>();
	public HashSet<ITIDEBug> addedbugs = new HashSet<ITIDEBug>();

	public HashSet<CGNode> syncMethods = new HashSet<>();
	private HashMap<Integer, SSAAbstractInvokeInstruction> threadInits = new HashMap<>();

	//akka system
	public ActorRef bughub;
	public SHBGraph shb;
	//to track changes from pta
	public HashMap<PointerKey, HashSet<MemNode>> pointer_rwmap = new HashMap<>();
	public HashMap<PointerKey, HashSet<SyncNode>> pointer_lmap = new HashMap<>();

	public int curTID;
	public HashMap<CGNode, Integer> astCGNode_ntid_map = new HashMap<>();

	public boolean useMayAlias = true;//false => lockObject.size == 1;

	//hard write
	private static Set<String> consideredJDKCollectionClass = HashSetFactory.make();
	//currently considered jdk class
	private static String ARRAYLIST = "<Primordial,Ljava/util/ArrayList>";
	private static String LINKEDLIST = "<Primordial,Ljava/util/LinkedList>";
	private static String HASHSET = "<Primordial,Ljava/util/HashSet>";
	private static String HASHMAP = "<Primordial,Ljava/util/HashMap>";
	private static String ARRAYS = "<Primordial,Ljava/util/Arrays>";
	private static String STRING = "<Primordial,Ljava/util/String>";


	public TIDEEngine(String entrySignature,CallGraph callGraph, PropagationGraph flowgraph, PointerAnalysis<InstanceKey> pointerAnalysis, ActorRef bughub){
		this.callGraph = callGraph;
		this.pointerAnalysis = pointerAnalysis;
		this.maxGraphNodeID = callGraph.getNumberOfNodes() + 1000;
		this.propagationGraph = flowgraph;
		this.bughub = bughub;

		consideredJDKCollectionClass.add(ARRAYLIST);
		consideredJDKCollectionClass.add(LINKEDLIST);
		consideredJDKCollectionClass.add(HASHSET);
		consideredJDKCollectionClass.add(HASHMAP);
		consideredJDKCollectionClass.add(ARRAYS);
		consideredJDKCollectionClass.add(STRING);

		Collection<CGNode> cgnodes = callGraph.getEntrypointNodes();
		for(CGNode n: cgnodes){
			String sig = n.getMethod().getSignature();
			//find the main node
			if(sig.contains(entrySignature)){
				mainEntryNodes.add(n);
			}else{
				TypeName name  = n.getMethod().getDeclaringClass().getName();
				threadSigNodeMap.put(name, n);
			}
		}

	}


	public HashSet<ITIDEBug> detectBothBugs(PrintStream ps) {
		long start = System.currentTimeMillis();

		for(CGNode main: mainEntryNodes){
			twiceProcessedNodes.clear();
			alreadyProcessedNodes.clear();//a new tid
			thirdProcessedNodes.clear();
			mapOfStartNode.clear();
			mapOfJoinNode.clear();
			stidpool.clear();
			threadDLLockPairs.clear();
			rsig_tid_num_map.clear();
			wsig_tid_num_map.clear();
			sharedFields.clear();
			sigReadNodes.clear();
			sigWriteNodes.clear();
			pointer_lmap.clear();
			pointer_rwmap.clear();
			excludedSigForRace.clear();
			excludedReadSigMapping.clear();
			excludedWriteSigMapping.clear();
			excludedMethodForBugs.clear();
			syncMethods.clear();
			bugs.clear();
			addedbugs.clear();
			removedbugs.clear();
			astCGNode_ntid_map.clear();

			shb = new SHBGraph();
			if(mainEntryNodes.size() >1 )
				System.err.println("MORE THAN 1 MAIN ENTRY!");

			//start from the main method
			threadNodes.add(main);
			int mainTID = main.getGraphNodeId();
			stidpool.add(mainTID);
			//find main node ifile
			SSAInstruction[] insts = main.getIR().getInstructions();
			SSAInstruction inst1st = null;
			for(int i=0; i<insts.length; i++){
				SSAInstruction inst = insts[i];
				if(inst!=null){
					inst1st = inst;
					break;
				}
			}

			IFile file = null;
			int sourceLineNum = 0;
			try{
				//get source code line number and ifile of this inst
				if(main.getIR().getMethod() instanceof IBytecodeMethod){
					int bytecodeindex = ((IBytecodeMethod) main.getIR().getMethod()).getBytecodeIndex(inst1st.iindex);
					sourceLineNum = (int)main.getIR().getMethod().getLineNumber(bytecodeindex);
				}else{
					SourcePosition position = main.getMethod().getSourcePosition(inst1st.iindex);
					sourceLineNum = position.getFirstLine();//.getLastLine();
					if(position instanceof JdtPosition){
						file = ((JdtPosition) position).getEclipseFile();
					}
				}
			}catch(Exception e){
				e.printStackTrace();
			}

			StartNode mainstart = new StartNode(-1, mainTID, null, main, sourceLineNum -1, file);
			mapOfStartNode.put(mainTID, mainstart);
			//add edge in shb
			shb.mainCGNode(main);
			shb.addEdge(mainstart, main);

			while(!threadNodes.isEmpty()){
				CGNode n = threadNodes.removeFirst();
				curTID = n.getGraphNodeId();

				if(n instanceof AstCGNode){
					CGNode real = ((AstCGNode)n).getCGNode();
					if(thirdProcessedNodes.contains(real))//already processed once
						continue;
					else
						thirdProcessedNodes.add(real);
				}else{
					//only twice at most for a node
					if(alreadyProcessedNodes.contains(n))
						if (twiceProcessedNodes.contains(n))
							continue;
						else
							twiceProcessedNodes.add(n);
				}

				hasSyncBetween = false;
				traverseNode(n);//path insensitive traversal
			}

			//extended happens-before relation
			organizeThreadsRelations();// grand -> parent -> kid threads
			if(mapOfStartNode.size() == 1){
				System.out.println("ONLY HAS MAIN THREAD, NO NEED TO PROCEED:   " + main.getMethod().toString());
				mapOfStartNode.clear();
				mapOfJoinNode.clear();
				continue;
			}else{
				System.out.println("mapOfStartNode =========================");
				for (Integer tid : mapOfStartNode.keySet()) {
					System.out.println(mapOfStartNode.get(tid).toString());
				}
				System.out.println("mapOfJoinNode =========================");
				for (Integer tid : mapOfJoinNode.keySet()) {
					System.out.println(mapOfJoinNode.get(tid).toString());
				}
				System.out.println();
			}

			//race detection
			//organize variable read/write map
			System.out.println("-----race detection start");
			organizeRWMaps();
			System.out.println("-----find shared variables");
			//1. find shared variables
			if(wsig_tid_num_map.size() >= 10){
				//use hub to speed up
				bughub.tell(new FindSharedVariable(rsig_tid_num_map, wsig_tid_num_map), bughub);
				awaitBugHubComplete();
			}else{
				//seq
				for(String sig: wsig_tid_num_map.keySet()){
					HashMap<Integer, Integer> writeTids = wsig_tid_num_map.get(sig);
					if(writeTids.size()>1){
						sharedFields.add(sig);
					}else{
						if(rsig_tid_num_map.containsKey(sig)){
							HashMap<Integer, Integer> readTids = rsig_tid_num_map.get(sig);
							if(readTids!=null){
								if(readTids.size() + writeTids.size() > 1){
									sharedFields.add(sig);
								}
							}
						}
					}
				}
			}

			//2. remove local nodes
			System.out.println("-----remove local nodes");
			bughub.tell(new RemoveLocalVar(), bughub);
			awaitBugHubComplete();

			//3. performance race detection with Fork-Join
			System.out.println("-----perform race detection with Fork-Join");
			bughub.tell(new DistributeDatarace(), bughub);
			awaitBugHubComplete();

			timeForDetectingRaces = timeForDetectingRaces + (System.currentTimeMillis() - start);
			start = System.currentTimeMillis();

			//detect deadlocks
			System.out.println("-----deadlocks detection start");
			bughub.tell(new DistributeDeadlock(), bughub);
			awaitBugHubComplete();

			timeForDetectingDL = timeForDetectingDL + (System.currentTimeMillis() -start);
		}

		System.err.println("Total Race Detection Time: " + timeForDetectingRaces);
		System.err.println("Total Deadlock Detection Time: " + timeForDetectingDL);

		bugs.removeAll(removedbugs);
		return bugs;
	}


	/**
	 * collect the rwnode sig from all trace, and count the number : //parallel?
	 */
	private void organizeRWMaps() {
		ArrayList<Trace> alltraces = shb.getAllTraces();
		for (Trace trace : alltraces) {
			singleOrganizeRWMaps(trace);
		}
	}

	/**
	 * sig-tid-num map
	 * @param trace
	 */
	private void singleOrganizeRWMaps(Trace trace) {
		HashMap<String, ArrayList<ReadNode>> rsigMapping = trace.getRsigMapping();
		HashMap<String, ArrayList<WriteNode>> wsigMapping = trace.getWsigMapping();
		ArrayList<Integer> tids = trace.getTraceTids();
		//read
		for (String rsig : rsigMapping.keySet()) {
			HashMap<Integer, Integer> tidnummap = rsig_tid_num_map.get(rsig);
			if(tidnummap == null){
				tidnummap = new HashMap<>();
				for (Integer tid : tids) {
					tidnummap.put(tid, 1);
				}
				rsig_tid_num_map.put(rsig, tidnummap);
			}else{
				for (Integer tid : tids) {
					if(tidnummap.keySet().contains(tid)){
						int num = tidnummap.get(tid);
						tidnummap.replace(tid, ++num);
					}else{
						tidnummap.put(tid, 1);
					}
				}
			}
		}
		//write
		for (String wsig : wsigMapping.keySet()) {
			HashMap<Integer, Integer> tidnummap = wsig_tid_num_map.get(wsig);
			if(tidnummap == null){
				tidnummap = new HashMap<>();
				for (Integer tid : tids) {
					tidnummap.put(tid, 1);
				}
				wsig_tid_num_map.put(wsig, tidnummap);
			}else{
				for (Integer tid : tids) {
					if(tidnummap.keySet().contains(tid)){
						int num = tidnummap.get(tid);
						tidnummap.replace(tid, ++num);
					}else{
						tidnummap.put(tid, 1);
					}
				}
			}
		}
	}

	private void awaitBugHubComplete() {
		boolean goon = true;
		while(goon){
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			goon = BugHub.askstatus();
		}
	}


	private void organizeThreadsRelations() {
		//start nodes --> add kids
		Iterator<Integer> tids = mapOfStartNode.keySet().iterator();
		LinkedList<Integer> reverse_tids = new LinkedList<>();
		while(tids.hasNext()){
			int cur_tid = tids.next();
			reverse_tids.addFirst(cur_tid);
		}
		//kid and grand kids
		Iterator<Integer> reverse_iter = reverse_tids.iterator();
		while(reverse_iter.hasNext()){
			int cur_tid = reverse_iter.next();
			StartNode cur_node = mapOfStartNode.get(cur_tid);
			int direct_kid = cur_node.getSelfTID();
			StartNode dkid_node = mapOfStartNode.get(direct_kid);
			if(dkid_node != null){
				MutableIntSet grand_kids = dkid_node.getTID_Child();
				if(!grand_kids.isEmpty()){
					cur_node.addChildren(grand_kids);
				}
			}
		}
		//join nodes --> add parents
		tids = mapOfJoinNode.keySet().iterator();
		reverse_tids = new LinkedList<>();
		while(tids.hasNext()){
			int cur_tid = tids.next();
			reverse_tids.addFirst(cur_tid);
		}
		reverse_iter = reverse_tids.iterator();
		while(reverse_iter.hasNext()){
			int cur_tid = reverse_iter.next();
			JoinNode cur_node = mapOfJoinNode.get(cur_tid);
			int direct_parent = cur_node.getParentTID();
			JoinNode dparent_node = mapOfJoinNode.get(direct_parent);
			if(dparent_node != null){
				MutableIntSet grand_parents = dparent_node.getTID_Parents();
				if(!grand_parents.isEmpty()){
					cur_node.addParents(grand_parents);
				}
			}
		}
	}


	private Trace traverseNode(CGNode n) {
		Trace curTrace = shb.getTrace(n);
		if(alreadyProcessedNodes.contains(n)){
			//allow multiple entries of a method if there exist sync in between
			if(!hasSyncBetween){
				if(curTrace == null){
					curTrace = new Trace(curTID);
					shb.addTrace(n, curTrace, curTID);
					return curTrace;
				}
			}else{
				hasSyncBetween = false;
			}
		}
		alreadyProcessedNodes.add(n);

		//create new trace if not in shbgraph
		if(curTrace != null){
			if(!curTrace.doesIncludeTid(curTID)){
				//exist edges include new tid>>
				traverseNode2nd(curTrace, n);
			}
			return curTrace;
		}else{
			if(n instanceof AstCGNode){
				n = ((AstCGNode)n).getCGNode();
			}
			curTrace = new Trace(curTID);
		}

		//add back to shb
		shb.addTrace(n, curTrace, curTID);

		if(n.getIR() == null)
			return null;

		SSACFG cfg = n.getIR().getControlFlowGraph();
		HashSet<SSAInstruction> catchinsts = InstInsideCatchBlock(cfg);//won't consider rw,lock related to catch blocks
		SSAInstruction[] insts = n.getIR().getInstructions();
		

		for(int i=0; i<insts.length; i++){
			SSAInstruction inst = insts[i];
			if(inst!=null){
				if(catchinsts.contains(inst)){
					continue;
				}
				IMethod method = n.getMethod();
				IFile file = null;
				int sourceLineNum = -1;
				if(!method.isSynthetic()){
					try{//get source code line number of this inst
						if(n.getIR().getMethod() instanceof IBytecodeMethod){
							int bytecodeindex = ((IBytecodeMethod) n.getIR().getMethod()).getBytecodeIndex(inst.iindex);
							sourceLineNum = (int)n.getIR().getMethod().getLineNumber(bytecodeindex);
						}else{
							SourcePosition position = n.getMethod().getSourcePosition(inst.iindex);
							sourceLineNum = position.getFirstLine();//.getLastLine();
							if(position instanceof JdtPosition){
								file = ((JdtPosition) position).getEclipseFile();
							}
						}
					}catch(Exception e){
						e.printStackTrace();
					}
				}

				if(inst instanceof SSAFieldAccessInstruction){
					processSSAFieldAccessInstruction(n, method, insts, i, inst, sourceLineNum, file, curTrace);
				}else if (inst instanceof SSAArrayReferenceInstruction){
					processSSAArrayReferenceInstruction(n, method, inst, sourceLineNum, file, curTrace);
				}else if (inst instanceof SSAAbstractInvokeInstruction){
					CallSiteReference csr = ((SSAAbstractInvokeInstruction)inst).getCallSite();
					MethodReference mr = csr.getDeclaredTarget();
					IMethod imethod = callGraph.getClassHierarchy().resolveMethod(mr);
					if(imethod != null){
						String sig = imethod.getSignature();
						if(sig.contains("java.util.concurrent") && sig.contains(".submit(Ljava/lang/Runnable;)Ljava/util/concurrent/Future")){
							//Future runnable
							PointerKey key = pointerAnalysis.getHeapModel().getPointerKeyForLocal(n, ((SSAAbstractInvokeInstruction) inst).getReceiver());
							OrdinalSet<InstanceKey> instances = pointerAnalysis.getPointsToSet(key);
							for(InstanceKey ins: instances){
								TypeName name = ins.getConcreteType().getName();
								CGNode node = threadSigNodeMap.get(name);
								if(node==null){
									//TODO: find out which runnable object -- need data flow analysis
									int param = ((SSAAbstractInvokeInstruction)inst).getUse(1);
									node = handleRunnable(ins, param, n);
									if(node==null){
										System.err.println("ERROR: starting new thread: "+ name);
										continue;
									}
								}
								System.out.println("Run : " + node.toString());

								processNewThreadInvoke(n, node, imethod, inst, ins, sourceLineNum, file, curTrace,false);
							}
							hasSyncBetween = true;
						}else if(sig.equals("java.lang.Thread.start()V")
								|| (sig.contains("java.util.concurrent") && sig.contains("execute"))){
							//Thread, Executors and ThreadPoolExecutor
							PointerKey key = pointerAnalysis.getHeapModel().getPointerKeyForLocal(n, ((SSAAbstractInvokeInstruction) inst).getReceiver());
							OrdinalSet<InstanceKey> instances = pointerAnalysis.getPointsToSet(key);
							for(InstanceKey ins: instances){
								TypeName name = ins.getConcreteType().getName();
								CGNode node = threadSigNodeMap.get(name);
								if(node==null){
									//TODO: find out which runnable object -- need data flow analysis
									int param = ((SSAAbstractInvokeInstruction)inst).getUse(0);
									if(sig.contains("java.util.concurrent") && sig.contains("execute")){
										param = ((SSAAbstractInvokeInstruction)inst).getUse(1);
									}
									node = handleRunnable(ins, param, n);
									if(node==null){
										System.err.println("ERROR: starting new thread: "+ name);
										continue;
									}
								}
								System.out.println("Run : " + node.toString());

								processNewThreadInvoke(n, node, imethod, inst, ins, sourceLineNum, file, curTrace, false);
							}
							hasSyncBetween = true;
						}else if(sig.contains("java.util.concurrent.Future.get()Ljava/lang/Object")){
							//Future join
							PointerKey key = pointerAnalysis.getHeapModel().getPointerKeyForLocal(n, ((SSAAbstractInvokeInstruction) inst).getReceiver());
							OrdinalSet<InstanceKey> instances = pointerAnalysis.getPointsToSet(key);
							for(InstanceKey ins: instances){
								TypeName name = ins.getConcreteType().getName();
								CGNode node = threadSigNodeMap.get(name);
								if(node==null){
									//TODO: find out which runnable object -- need data flow analysis
									int param = ((SSAAbstractInvokeInstruction)inst).getUse(0);
									SSAInstruction creation = n.getDU().getDef(param);
									if(creation instanceof SSAAbstractInvokeInstruction){
										param = ((SSAAbstractInvokeInstruction)creation).getUse(1);
										node = handleRunnable(ins, param, n);
										if(node==null){
											System.err.println("ERROR: joining parent thread: "+ name);
											continue;
										}
									}
								}
								System.out.println("Join : " + node.toString());

								processNewThreadJoin(n, node, imethod, inst, ins, sourceLineNum, file, curTrace, false, false);
							}
							hasSyncBetween = true;
						}
						else if(sig.equals("java.lang.Thread.join()V")
								|| (sig.contains("java.util.concurrent") && sig.contains("shutdown()V"))){
							//Executors and ThreadPoolExecutor
							PointerKey key = pointerAnalysis.getHeapModel().getPointerKeyForLocal(n, ((SSAAbstractInvokeInstruction) inst).getReceiver());
							OrdinalSet<InstanceKey> instances = pointerAnalysis.getPointsToSet(key);
							for(InstanceKey ins: instances) {
								TypeName name = ins.getConcreteType().getName();
								CGNode node = threadSigNodeMap.get(name);
								boolean isThreadPool = false;
								if(node==null){//could be a runnable class
									int param = ((SSAAbstractInvokeInstruction)inst).getUse(0);
									//Executors and ThreadPoolExecutor
									if(sig.contains("java.util.concurrent") &&sig.contains("shutdown()V")){
										Iterator<SSAInstruction> uses = n.getDU().getUses(param);
										while(uses.hasNext()){
											SSAInstruction use = uses.next();//java.util.concurrent.Executor.execute
											if(use instanceof SSAAbstractInvokeInstruction){
												SSAAbstractInvokeInstruction invoke = (SSAAbstractInvokeInstruction) use;
												CallSiteReference ucsr = ((SSAAbstractInvokeInstruction)invoke).getCallSite();
												MethodReference umr = ucsr.getDeclaredTarget();
												IMethod uimethod = callGraph.getClassHierarchy().resolveMethod(umr);
												String usig = uimethod.getSignature();
												if(usig.contains("java.util.concurrent") &&usig.contains("execute")){
													param = ((SSAAbstractInvokeInstruction)invoke).getUse(1);
													isThreadPool = true;
													break;
												}
											}
										}
									}
									node = handleRunnable(ins,param, n);
									if(node==null){
										System.err.println("ERROR: joining parent thread: "+ name);
										continue;
									}
								}
								System.out.println("Join : " + node.toString());

								processNewThreadJoin(n, node, imethod, inst, ins, sourceLineNum, file, curTrace, isThreadPool, false);
							}
							hasSyncBetween = true;
						}else if(sig.equals("java.lang.Thread.<init>(Ljava/lang/Runnable;)V")){
							//for new Thread(new Runnable) => record its initialization
							int use0 = inst.getUse(0);
							threadInits.put(use0, (SSAAbstractInvokeInstruction)inst);
						}else{
							//other method calls
							processNewMethodInvoke(n, csr, inst, sourceLineNum, file, curTrace);
						}
					}
				}else if(inst instanceof SSAMonitorInstruction){
					processSSAMonitorInstruction(n, method, inst, sourceLineNum, file, curTrace);
					hasSyncBetween = true;
				}
			}
		}
		return curTrace;
	}


	private void processNewMethodInvoke(CGNode n, CallSiteReference csr, SSAInstruction inst, int sourceLineNum, IFile file, Trace curTrace) {
		Set<CGNode> set = new HashSet<>();
		if(n instanceof AstCGNode){
			CGNode temp = n;
			while (temp instanceof AstCGNode) {
				temp = ((AstCGNode)temp).getCGNode();
			}
			set = callGraph.getPossibleTargets(temp, csr);
		}else{
			set = callGraph.getPossibleTargets(n, csr);
		}
		for(CGNode node: set){
			IClass declaringclass = node.getMethod().getDeclaringClass();
			if(include(declaringclass)){
				//static method call
				if(node.getMethod().isStatic()){
					//omit the pointer-lock map, use classname as lock obj
					String typeclassname =  n.getMethod().getDeclaringClass().getName().toString();
					String instSig =typeclassname.substring(1)+":"+sourceLineNum;
					String lock = node.getMethod().getDeclaringClass().getName().toString();
					//take out records
					HashSet<DLockNode> currentNodes = threadLockNodes.get(curTID);
					if(currentNodes==null){
						currentNodes = new HashSet<DLockNode>();
						threadLockNodes.put(curTID,currentNodes);
					}
					ArrayList<DLPair> dLLockPairs = threadDLLockPairs.get(curTID);
					if(dLLockPairs==null){
						dLLockPairs = new ArrayList<DLPair>();
						threadDLLockPairs.put(curTID, dLLockPairs);
					}
					DLockNode will = null;
					//if synchronized method, add lock/unlock
					if(node.getMethod().isSynchronized()){
						syncMethods.add(node);
						// for deadlock
						will = new DLockNode(curTID,instSig, sourceLineNum, null, null, n, inst, file);
						will.addLockSig(lock);
						for (DLockNode exist : currentNodes) {
							dLLockPairs.add(new DLPair(exist, will));
						}
						curTrace.add(will);
						threadLockNodes.get(curTID).add(will);
					}
					MethodNode m = new MethodNode(n, node, curTID, sourceLineNum, file, (SSAAbstractInvokeInstruction) inst);
					curTrace.add(m);
					Trace subTrace0 = traverseNode(node);
					shb.includeTidForKidTraces(node, curTID);
					shb.addEdge(m, node);
					if(node.getMethod().isSynchronized()){
						DUnlockNode unlock = new DUnlockNode(curTID, instSig, sourceLineNum, null, null, n, sourceLineNum);
						unlock.addLockSig(lock);
						curTrace.addLockPair(new LockPair(will, unlock));
						//remove
						curTrace.add(unlock);
						threadLockNodes.get(curTID).remove(will);
					}
				}else{
					//instance
					int objectValueNumber = inst.getUse(0);
					PointerKey objectPointer = pointerAnalysis.getHeapModel().getPointerKeyForLocal(n, objectValueNumber);
					OrdinalSet<InstanceKey> lockedObjects = pointerAnalysis.getPointsToSet(objectPointer);
					DLockNode will = null;
					if(lockedObjects.size()>0){//must be larger than 0
						//take out records
						HashSet<DLockNode> currentNodes = threadLockNodes.get(curTID);
						if(currentNodes==null){
							currentNodes = new HashSet<DLockNode>();
							threadLockNodes.put(curTID,currentNodes);
						}
						ArrayList<DLPair> dLLockPairs = threadDLLockPairs.get(curTID);
						if(dLLockPairs==null){
							dLLockPairs = new ArrayList<DLPair>();
							threadDLLockPairs.put(curTID, dLLockPairs);
						}
						//start to record new locks
						if(node.getMethod().isSynchronized()){
							String typeclassname = n.getMethod().getDeclaringClass().getName().toString();
							String instSig = typeclassname.substring(1)+":"+sourceLineNum;
							will = new DLockNode(curTID,instSig, sourceLineNum, objectPointer, lockedObjects, n, inst, file);
							for (InstanceKey key : lockedObjects) {
								String lock = key.getConcreteType().getName()+"."+key.hashCode();
								will.addLockSig(lock);
							}
							// for deadlock
							for (DLockNode exist : currentNodes) {
								dLLockPairs.add(new DLPair(exist, will));
							}
							curTrace.add(will);
							threadLockNodes.get(curTID).add(will);
							//for pointer-lock map
							HashSet<SyncNode> ls = pointer_lmap.get(objectPointer);
							if(ls == null){
								ls = new HashSet<>();
								ls.add(will);
								pointer_lmap.put(objectPointer, ls);
							}else{
								ls.add(will);
							}
						}
					}
					MethodNode m = new MethodNode(n, node, curTID, sourceLineNum, file, (SSAAbstractInvokeInstruction) inst);
					curTrace.add(m);
						Trace subTrace1 = traverseNode(node);
						shb.includeTidForKidTraces(node,curTID);
					shb.addEdge(m, node);
					if(lockedObjects.size() > 0){
						if(node.getMethod().isSynchronized()){
							String typeclassname =  n.getMethod().getDeclaringClass().getName().toString();
							String instSig =typeclassname.substring(1)+":"+sourceLineNum;
							DUnlockNode unlock = new DUnlockNode(curTID, instSig, sourceLineNum, objectPointer, lockedObjects, n, sourceLineNum);
							LockPair lockPair = new LockPair(will, unlock);
							curTrace.addLockPair(lockPair);
							for (InstanceKey instanceKey : lockedObjects) {
								String lock = instanceKey.getConcreteType().getName()+"."+instanceKey.hashCode();
								unlock.addLockSig(lock);
							}
							curTrace.add(unlock);
							threadLockNodes.get(curTID).remove(will);
						}
					}
				}
			}
		}
	}


	private void processSSAMonitorInstruction(CGNode n, IMethod method, SSAInstruction inst, int sourceLineNum,
			IFile file, Trace curTrace) {
		SSAMonitorInstruction monitorInstruction = ((SSAMonitorInstruction) inst);
		int lockValueNumber = monitorInstruction.getRef();

		PointerKey lockPointer = pointerAnalysis.getHeapModel().getPointerKeyForLocal(n, lockValueNumber);
		OrdinalSet<InstanceKey> lockObjects = pointerAnalysis.getPointsToSet(lockPointer);
		// for deadlock
		String typeclassname =  n.getMethod().getDeclaringClass().getName().toString();
		String instSig =typeclassname.substring(1)+":"+sourceLineNum;
		DLockNode will = null;
		DUnlockNode next = null;
		//take our record
		HashSet<DLockNode> currentNodes = threadLockNodes.get(curTID);
		if(currentNodes==null){
			currentNodes = new HashSet<DLockNode>();
			threadLockNodes.put(curTID,currentNodes);
		}
		ArrayList<DLPair> dlpairs = threadDLLockPairs.get(curTID);
		if(dlpairs==null){
			dlpairs = new ArrayList<DLPair>();
			threadDLLockPairs.put(curTID, dlpairs);
		}
		for (InstanceKey instanceKey : lockObjects) {
			String lock = instanceKey.getConcreteType().getName()+"."+instanceKey.hashCode();
			if(((SSAMonitorInstruction) inst).isMonitorEnter()){
				will = new DLockNode(curTID, instSig, sourceLineNum, lockPointer, lockObjects, n, inst, file);
				will.addLockSig(lock);
			}else{
				next = new DUnlockNode(curTID, instSig, sourceLineNum, lockPointer, lockObjects, n, sourceLineNum);
				next.addLockSig(lock);
				for (Iterator<DLockNode> iterator = currentNodes.iterator(); iterator.hasNext();) {
					DLockNode dLockNode = (DLockNode) iterator.next();
					if (dLockNode.getInstSig().equals(instSig)) {//maybe compare pointer?
						will = dLockNode;
						break;
					}
				}
			}
		}

		if(((SSAMonitorInstruction) inst).isMonitorEnter()){
			if(will != null){
				for (DLockNode exist : currentNodes) {
					dlpairs.add(new DLPair(exist, will));
				}
				curTrace.add(will);
				threadLockNodes.get(curTID).add(will);
				//for pointer-lock map
				HashSet<SyncNode> ls = pointer_lmap.get(lockPointer);
				if(ls == null){
					ls = new HashSet<>();
					ls.add(will);
					pointer_lmap.put(lockPointer, ls);
				}else{
					ls.add(will);
				}
			}
		}else {//monitor exit
			if(will != null){
				curTrace.add(next);
				curTrace.addLockPair(new LockPair(will, next));
				threadLockNodes.get(curTID).remove(will);
			}
		}
	}


	private void processNewThreadJoin(CGNode n, CGNode node, IMethod imethod, SSAInstruction inst, InstanceKey ins,
			int sourceLineNum, IFile file, Trace curTrace, boolean isThreadPool, boolean second) {
		//add node to trace
		int tid_child = node.getGraphNodeId();
		if(mapOfJoinNode.containsKey(tid_child)){
			CGNode threadNode = dupStartJoinTidMap.get(tid_child);
			tid_child = threadNode.getGraphNodeId();
			node = threadNode;
		}

		JoinNode jNode = new JoinNode(curTID, tid_child, n, node, sourceLineNum, file);
		if(second){
			curTrace.add2J(jNode, inst, tid_child);
		}else{
			curTrace.addJ(jNode, inst);
		}
		shb.addBackEdge(node, jNode);
		mapOfJoinNode.put(tid_child, jNode);

		boolean isInLoop = isInLoop(n,inst);
		if(isInLoop || isThreadPool){
			AstCGNode node2 = n_loopn_map.get(node);//should find created node2 during start
			if(node2 == null){
				node2 = dupStartJoinTidMap.get(tid_child);
				if(node2 == null){
					System.err.println("Null node obtain from n_loopn_map. ");
					return;
				}
			}
			int newID = node2.getGraphNodeId();
			JoinNode jNode2 = new JoinNode(curTID, newID, n, node2, sourceLineNum, file);
			curTrace.add2J(jNode2, inst, newID);//thread id +1
			shb.addBackEdge(node2, jNode2);
			mapOfJoinNode.put(newID, jNode2);
		}
	}


	private void processNewThreadInvoke(CGNode n, CGNode node, IMethod method, SSAInstruction inst, InstanceKey ins, int sourceLineNum, IFile file,
			Trace curTrace, boolean second) {
		boolean scheduled_this_thread = false;
		//duplicate graph node id
		if(stidpool.contains(node.getGraphNodeId())){
			if(threadNodes.contains(node) && scheduledAstNodes.contains(node)){
				//already scheduled to process twice, skip here.
				scheduled_this_thread = true;
			}else{
				scheduledAstNodes.add(node);
				AstCGNode threadNode = new AstCGNode(method, node.getContext());
				int threadID = ++maxGraphNodeID;
				threadNode.setGraphNodeId(threadID);
				threadNode.setCGNode(node);
				threadNode.setIR(node.getIR());
				dupStartJoinTidMap.put(node.getGraphNodeId(), threadNode);
				node = threadNode;
			}
		}

		if(!scheduled_this_thread){
			threadNodes.add(node);
			int tid_child = node.getGraphNodeId();
			stidpool.add(tid_child);
			//add node to trace
			StartNode startNode = new StartNode(curTID, tid_child, n, node, sourceLineNum, file);//n
			if(second){//2nd traveral, insert
				curTrace.add2S(startNode, inst, tid_child);
			}else{
				curTrace.addS(startNode, inst, tid_child);
			}
			shb.addEdge(startNode, node);
			mapOfStartNode.put(tid_child, startNode);
			StartNode pstartnode = mapOfStartNode.get(curTID);
			if(pstartnode == null){//?? should not be null, curtid is removed from map
				if(mainEntryNodes.contains(n)){
					pstartnode = new StartNode(-1, curTID, n, node, sourceLineNum, file);
					mapOfStartNode.put(curTID, pstartnode);
				}else{//thread/runnable
					pstartnode = new StartNode(curTID, tid_child, n, node,sourceLineNum, file);
					mapOfStartNode.put(tid_child, pstartnode);
				}
			}
			pstartnode.addChild(tid_child);

			boolean isInLoop = isInLoop(n,inst);
			if(isInLoop){
				AstCGNode node2 = new AstCGNode(node.getMethod(),node.getContext());
				threadNodes.add(node2);
				int newID = ++maxGraphNodeID;
				astCGNode_ntid_map.put(node, newID);
				StartNode duplicate = new StartNode(curTID,newID, n, node2,sourceLineNum, file);
				curTrace.add2S(duplicate, inst, newID);//thread id +1
				shb.addEdge(duplicate, node2);
				mapOfStartNode.put(newID, duplicate);
				mapOfStartNode.get(curTID).addChild(newID);

				node2.setGraphNodeId(newID);
				node2.setIR(node.getIR());
				node2.setCGNode(node);
				n_loopn_map.put(node, node2);
			}
		}
	}


	private void processSSAArrayReferenceInstruction(CGNode n, IMethod method, SSAInstruction inst, int sourceLineNum,
			IFile file, Trace curTrace) {
		SSAArrayReferenceInstruction arrayRefInst = (SSAArrayReferenceInstruction) inst;
		int	arrayRef = arrayRefInst.getArrayRef();
		String typeclassname =  method.getDeclaringClass().getName().toString();
		String instSig =typeclassname.substring(1)+":"+sourceLineNum;
		PointerKey key = pointerAnalysis.getHeapModel().getPointerKeyForLocal(n, arrayRef);
		OrdinalSet<InstanceKey> instances = pointerAnalysis.getPointsToSet(key);
		//		String field = handleArrayTypes(arrayRefInst, n, instances); //currently, won't consider
		String field = "";
		logArrayAccess(inst, sourceLineNum, instSig, curTrace, n, key, instances, file, field);
	}


	private void processSSAFieldAccessInstruction(CGNode n, IMethod method, SSAInstruction[] insts, int i,
			SSAInstruction inst, int sourceLineNum, IFile file, Trace curTrace) {
		if(n.getMethod().isClinit()||n.getMethod().isInit())
			return;
		//field access before monitorenter, check
		if(i+1 < insts.length){
			SSAInstruction next = insts[i+1];
			if(next instanceof SSAMonitorInstruction){
				SSAFieldAccessInstruction access = (SSAFieldAccessInstruction)inst;
				int result = access.getDef();//result
				int locked = ((SSAMonitorInstruction) next).getRef();
				if(result == locked){
					//pre-read of lock/monitor enter, do not record
					//check previous read
					if(i-1 >= 0){
						SSAInstruction pred = insts[i-1];
						int ref = access.getRef();
						if(pred instanceof SSAGetInstruction){
							int result2 = ((SSAGetInstruction) pred).getDef();//result
							if(result2 == ref && result2 != -1 && ref != -1){
								//another field access before monitorenter => we ignore
								//removed node in trace
								curTrace.removeLastNode();
							}
						}
					}
					return;
				}
			}
		}
		//TODO: handling field access of external objects
		String classname = ((SSAFieldAccessInstruction)inst).getDeclaredField().getDeclaringClass().getName().toString();
		String fieldname = ((SSAFieldAccessInstruction)inst).getDeclaredField().getName().toString();
		String sig = classname.substring(1)+"."+fieldname;
		String typeclassname =  method.getDeclaringClass().getName().toString();
		String instSig =typeclassname.substring(1)+":"+sourceLineNum;

		if(((SSAFieldAccessInstruction)inst).isStatic()){
			logFieldAccess(inst, sourceLineNum, instSig, curTrace, n, null, null, sig, file);
		}else{
			int baseValueNumber = ((SSAFieldAccessInstruction)inst).getUse(0);
			PointerKey basePointer = pointerAnalysis.getHeapModel().getPointerKeyForLocal(n, baseValueNumber);//+
			OrdinalSet<InstanceKey> baseObjects = pointerAnalysis.getPointsToSet(basePointer);//+
			logFieldAccess(inst, sourceLineNum, instSig, curTrace, n, basePointer, baseObjects, sig, file);
		}
	}

	/**
	 * the jdk classes and method we want to consider
	 * @param declaringclass
	 * @return
	 */
	private boolean include(IClass declaringclass) {
		if(AnalysisUtils.isApplicationClass(declaringclass)){
			return true;
		}else if(AnalysisUtils.isJDKClass(declaringclass)){
			String dcName = declaringclass.toString();
			if(consideredJDKCollectionClass.contains(dcName)){
				return true;
			}
		}
		return false;
	}



	private Trace traverseNode2nd(Trace curTrace, CGNode n) {
		if(n.getIR() == null)
			return curTrace;

		//let curtrace edges include new tids
		boolean includeCurtid = !shb.includeTidForKidTraces(n, curTID);
		//start traverse inst
		SSACFG cfg = n.getIR().getControlFlowGraph();
		HashSet<SSAInstruction> catchinsts = InstInsideCatchBlock(cfg);

		SSAInstruction[] insts = n.getIR().getInstructions();
		for(int i=0; i<insts.length; i++){
			SSAInstruction inst = insts[i];
			if(inst!=null){
				if(catchinsts.contains(inst)){
					continue;
				}
				IMethod method = n.getMethod() ;
				int sourceLineNum = 0;
				IFile file = null;
				try{//get source code line number of this inst
					if(n.getIR().getMethod() instanceof IBytecodeMethod){
						int bytecodeindex = ((IBytecodeMethod) n.getIR().getMethod()).getBytecodeIndex(inst.iindex);
						sourceLineNum = (int)n.getIR().getMethod().getLineNumber(bytecodeindex);
					}else{
						SourcePosition position = n.getMethod().getSourcePosition(inst.iindex);
						sourceLineNum = position.getFirstLine();//.getLastLine();
						if(position instanceof JdtPosition){
							file = ((JdtPosition) position).getEclipseFile();
						}
					}
				}catch(Exception e){
					e.printStackTrace();
				}

				if (inst instanceof SSAAbstractInvokeInstruction){
					CallSiteReference csr = ((SSAAbstractInvokeInstruction)inst).getCallSite();
					MethodReference mr = csr.getDeclaredTarget();
					com.ibm.wala.classLoader.IMethod imethod = callGraph.getClassHierarchy().resolveMethod(mr);
					if(imethod!=null){
						String sig = imethod.getSignature();
						if(sig.contains("java.util.concurrent") && sig.contains(".submit(Ljava/lang/Runnable;)Ljava/util/concurrent/Future")){
							//Future runnable
							PointerKey key = pointerAnalysis.getHeapModel().getPointerKeyForLocal(n, ((SSAAbstractInvokeInstruction) inst).getReceiver());
							OrdinalSet<InstanceKey> instances = pointerAnalysis.getPointsToSet(key);
							for(InstanceKey ins: instances){
								TypeName name = ins.getConcreteType().getName();
								CGNode node = threadSigNodeMap.get(name);
								if(node==null){
									//TODO: find out which runnable object -- need data flow analysis
									int param = ((SSAAbstractInvokeInstruction)inst).getUse(1);
									node = handleRunnable(ins, param, n);
									if(node==null){
										System.err.println("ERROR: starting new thread: "+ name);
										continue;
									}
								}
								System.out.println("Run : " + node.toString());

								processNewThreadInvoke(n, node, imethod, inst, ins, sourceLineNum, file, curTrace, true);
							}
							hasSyncBetween = true;
						}else if(sig.equals("java.lang.Thread.start()V")
								|| (sig.contains("java.util.concurrent") && sig.contains("execute"))){
							PointerKey key = pointerAnalysis.getHeapModel().getPointerKeyForLocal(n, ((SSAAbstractInvokeInstruction) inst).getReceiver());
							OrdinalSet<InstanceKey> instances = pointerAnalysis.getPointsToSet(key);
							for(InstanceKey ins: instances){
								TypeName name = ins.getConcreteType().getName();
								CGNode node = threadSigNodeMap.get(name);
								if(node==null){
									//TODO: find out which runnable object -- need data flow analysis
									int param = ((SSAAbstractInvokeInstruction)inst).getUse(0);
									if(sig.contains("java.util.concurrent") && sig.contains("execute")){
										param = ((SSAAbstractInvokeInstruction)inst).getUse(1);
									}
									node = handleRunnable(ins, param, n);
									if(node==null){
										System.err.println("ERROR: starting new thread: "+ name);
										continue;
									}
								}
								System.out.println("Run : " + node.toString());

								processNewThreadInvoke(n, node, imethod, inst, ins, sourceLineNum, file, curTrace, true);
							}
							hasSyncBetween = true;
						}
						else if(sig.contains("java.util.concurrent.Future.get()Ljava/lang/Object")){
							//Future join
							PointerKey key = pointerAnalysis.getHeapModel().getPointerKeyForLocal(n, ((SSAAbstractInvokeInstruction) inst).getReceiver());
							OrdinalSet<InstanceKey> instances = pointerAnalysis.getPointsToSet(key);
							for(InstanceKey ins: instances){
								TypeName name = ins.getConcreteType().getName();
								CGNode node = threadSigNodeMap.get(name);
								if(node==null){
									//TODO: find out which runnable object -- need data flow analysis
									int param = ((SSAAbstractInvokeInstruction)inst).getUse(0);
									SSAInstruction creation = n.getDU().getDef(param);
									if(creation instanceof SSAAbstractInvokeInstruction){
										param = ((SSAAbstractInvokeInstruction)creation).getUse(1);
										node = handleRunnable(ins, param, n);
										if(node==null){
											System.err.println("ERROR: joining parent thread: "+ name);
											continue;
										}
									}
								}
								System.out.println("Join : " + node.toString());

								processNewThreadJoin(n, node, imethod, inst, ins, sourceLineNum, file, curTrace, false, true);
							}
							hasSyncBetween = true;
						}
						else if(sig.equals("java.lang.Thread.join()V")
								|| (sig.contains("java.util.concurrent") && sig.contains("shutdown()V"))){
							PointerKey key = pointerAnalysis.getHeapModel().getPointerKeyForLocal(n, ((SSAAbstractInvokeInstruction) inst).getReceiver());
							OrdinalSet<InstanceKey> instances = pointerAnalysis.getPointsToSet(key);
							for(InstanceKey ins: instances){
								TypeName name = ins.getConcreteType().getName();
								CGNode node = threadSigNodeMap.get(name);
								boolean isThreadPool = false;
								if(node==null){//could be a runnable class
									int param = ((SSAAbstractInvokeInstruction)inst).getUse(0);
									//Executors and ThreadPoolExecutor
									if(sig.contains("java.util.concurrent") &&sig.contains("shutdown()V")){
										Iterator<SSAInstruction> uses = n.getDU().getUses(param);
										while(uses.hasNext()){
											SSAInstruction use = uses.next();//java.util.concurrent.Executor.execute
											if(use instanceof SSAAbstractInvokeInstruction){
												SSAAbstractInvokeInstruction invoke = (SSAAbstractInvokeInstruction) use;
												CallSiteReference ucsr = ((SSAAbstractInvokeInstruction)invoke).getCallSite();
												MethodReference umr = ucsr.getDeclaredTarget();
												IMethod uimethod = callGraph.getClassHierarchy().resolveMethod(umr);
												String usig = uimethod.getSignature();
												if(usig.contains("java.util.concurrent") &&usig.contains("execute")){
													param = ((SSAAbstractInvokeInstruction)invoke).getUse(1);
													isThreadPool = true;
													break;
												}
											}
										}
									}
									node = handleRunnable(ins,param, n);
									if(node==null){
										System.err.println("ERROR: joining parent thread: "+ name);
										continue;
									}
								}
								System.out.println("Join : " + node.toString());

								processNewThreadJoin(n, node, imethod, inst, ins, sourceLineNum, file, curTrace, isThreadPool, true);
							}
							hasSyncBetween = true;
						}else if(sig.equals("java.lang.Thread.<init>(Ljava/lang/Runnable;)V")){
							//for new Thread(new Runnable)
							int use0 = inst.getUse(0);
							threadInits.put(use0, (SSAAbstractInvokeInstruction)inst);
						}else{
							//other method calls
							Set<CGNode> set = new HashSet<>();
							if(n instanceof AstCGNode){
								CGNode temp = n;
								while (temp instanceof AstCGNode) {
									temp = ((AstCGNode)temp).getCGNode();
								}
								set = callGraph.getPossibleTargets(temp, csr);
							}else{
								set = callGraph.getPossibleTargets(n, csr);
							}
							for(CGNode node: set){
								IClass declaringclass = node.getMethod().getDeclaringClass();
								if(include(declaringclass)){
									if(!includeCurtid){
										shb.includeTidForKidTraces(node, curTID);
									}
								}
							}
						}
					}
				}
			}

		}
		return curTrace;
	}


	private boolean isInLoop(CGNode n, SSAInstruction inst) {
		Collection<Loop> loops = nodeLoops.get(n);
		if(loops==null){
			IR ir = n.getIR();
			if(ir!=null)
				loops = findLoops(ir);
			else
				return false;
		}

		for(Loop loop: loops){
			List insts = loop.getLoopInstructions();
			if(insts.contains(inst))
				return true;
		}
		return false;
	}

	@SuppressWarnings("rawtypes")
	private static Collection<Loop> findLoops(IR ir){
		SSACFG cfg =  ir.getControlFlowGraph();
		BasicBlock root = cfg.entry();
		NumberedDominators dominator = new NumberedDominators(cfg,root);

		Iterator<ISSABasicBlock> bbs = cfg.iterator();
		HashSet domSet = new HashSet();
		HashMap<BasicBlock, List<BasicBlock>> loops = new HashMap<BasicBlock, List<BasicBlock>>();

		while(bbs.hasNext()){
			ISSABasicBlock bb = bbs.next();
			Iterator<ISSABasicBlock> succs = cfg.getSuccNodes(bb);
			Iterator<ISSABasicBlock> dominators = dominator.dominators(bb);


			while(dominators.hasNext())
				domSet.add(dominators.next());

			ArrayList<ISSABasicBlock> headers=null;
			while(succs.hasNext()){
				ISSABasicBlock succ = succs.next();

				if (domSet.contains(succ)){
					//header succeeds and dominates s, we have a loop
					if(headers==null)
						headers = new ArrayList<ISSABasicBlock>();
					headers.add(succ);
				}
			}
			domSet.clear();
			if(headers!=null){
				Iterator<ISSABasicBlock> headersIt = headers.iterator();
				while (headersIt.hasNext()){
					BasicBlock header = (BasicBlock) headersIt.next();
					List<BasicBlock> loopBody = getLoopBodyFor(cfg, header, bb);
					if (loops.containsKey(header)){
						// merge bodies
						List<BasicBlock> lb1 = loops.get(header);
						loops.put(header, union(lb1, loopBody));
					}else {
						loops.put(header, loopBody);
					}
				}
			}
		}

		Collection<Loop> result = new HashSet<Loop>();
		for (Map.Entry<BasicBlock,List<BasicBlock>> entry : loops.entrySet()) {
			result.add(new Loop(entry.getKey(),entry.getValue(),cfg));
		}
		return result;
	}

	private static List<BasicBlock> getLoopBodyFor(SSACFG cfg, BasicBlock header, ISSABasicBlock node){
		ArrayList<BasicBlock> loopBody = new ArrayList<BasicBlock>();
		Stack<ISSABasicBlock> stack = new Stack<ISSABasicBlock>();

		loopBody.add(header);
		stack.push(node);

		while (!stack.isEmpty()){
			BasicBlock next = (BasicBlock)stack.pop();
			if (!loopBody.contains(next)){
				// add next to loop body
				loopBody.add(0, next);
				// put all preds of next on stack
				Iterator<ISSABasicBlock> it = cfg.getPredNodes(next);
				while (it.hasNext()){
					stack.push(it.next());
				}
			}
		}

		assert (node==header && loopBody.size()==1) || loopBody.get(loopBody.size()-2)==node;
		assert loopBody.get(loopBody.size()-1)==header;

		return loopBody;
	}

	private static List<BasicBlock> union(List<BasicBlock> l1, List<BasicBlock> l2){
		Iterator<BasicBlock> it = l2.iterator();
		while (it.hasNext()){
			BasicBlock next = it.next();
			if (!l1.contains(next)){
				l1.add(next);
			}
		}
		return l1;
	}

	/**
	 * not used => expensive
	 * @param inst
	 * @param anode
	 * @param instances
	 * @return
	 */
	private String handleArrayTypes(SSAArrayReferenceInstruction inst, CGNode anode, OrdinalSet<InstanceKey> instances) {
		int def = inst.getArrayRef();
		String returnValue = "";
		for (InstanceKey instKey : instances) {//size? mutiple => assignment between arrays?
			if(instKey instanceof AllocationSiteInNode){
				SSAInstruction creation = anode.getDU().getDef(def);
				CGNode who = anode;
				if(creation == null){
					CGNode n = ((AllocationSiteInNode) instKey).getNode();
					creation = n.getDU().getDef(def);
					who = n;
					//if creation still == null; this def represents a local variable or assignment between local and global variables;
					// =>> only use the instance hashcode to check race
					// the comment code below can get the name of the local variable.
					if(creation == null){
						IMethod method = anode.getIR().getControlFlowGraph().getMethod();
						if(method instanceof ConcreteJavaMethod){
							ConcreteJavaMethod jMethod = (ConcreteJavaMethod) method;
							DebuggingInformation info = jMethod.debugInfo();
							String[][] names = info.getSourceNamesForValues();
							String[] name = names[def];
							return "local:" + Arrays.toString(name).replace("[", "").replace("]", "");
						}
					}
				}
				returnValue = classifyStmtTypes(creation, who);
			}else{
				System.out.println("CANNOT HANDLE ARRAY: " + inst);
			}
		}
		return returnValue;
	}

	private String classifyStmtTypes(SSAInstruction creation, CGNode who){
		if(creation instanceof SSAFieldAccessInstruction){
			String classname = ((SSAFieldAccessInstruction) creation).getDeclaredField().getDeclaringClass().getName().toString();
			String fieldname = ((SSAFieldAccessInstruction) creation).getDeclaredField().getName().toString();
			return classname.substring(1)+"."+fieldname;
		}else if(creation instanceof SSANewInstruction){
			String classname = ((SSANewInstruction) creation).getNewSite().getDeclaredType().getName().getClassName().toString();
			return classname;
		}else if(creation instanceof SSAArrayReferenceInstruction ){
			SSAArrayReferenceInstruction arrayRefInst = (SSAArrayReferenceInstruction) creation;
			int def0 = arrayRefInst.getArrayRef();
			PointerKey key0 = pointerAnalysis.getHeapModel().getPointerKeyForLocal(who, def0);
			OrdinalSet<InstanceKey> instances0 = pointerAnalysis.getPointsToSet(key0);
			return handleArrayTypes(arrayRefInst, who, instances0);
		}else if(creation instanceof SSAAbstractInvokeInstruction){
			String classname = ((SSAAbstractInvokeInstruction) creation).getCallSite().getDeclaredTarget().getReturnType().getName().getClassName().toString();
			return classname;
		}else if(creation instanceof SSACheckCastInstruction){
			SSACheckCastInstruction cast = ((SSACheckCastInstruction) creation);
			int def0 = cast.getVal();
			SSAInstruction creation0 = who.getDU().getDef(def0);
			return classifyStmtTypes(creation0, who);
		}else if(creation instanceof SSAPhiInstruction){//infinit loop
			SSAPhiInstruction phi = (SSAPhiInstruction) creation;
			int def0 = phi.getUse(0);
			SSAInstruction creation0 = who.getDU().getDef(def0);
			return classifyStmtTypes(creation0, who);
		}
		else{
			System.out.println(creation);
			return "";
		}
	}

	/**
	 * only works for simple runnable constructor.
	 * @param instKey
	 * @param param
	 * @param invokeCGNode
	 * @return
	 */
	private CGNode handleRunnable(InstanceKey instKey, int param, CGNode invokeCGNode) {
		if(instKey instanceof AllocationSiteInNode){
			CGNode keyCGNode = ((AllocationSiteInNode) instKey).getNode();
			CGNode node = null;//return
			TypeName name = null;
			SSAInstruction creation = invokeCGNode.getDU().getDef(param);
			CGNode useNode = invokeCGNode;
			if(creation == null){
				creation = keyCGNode.getDU().getDef(param);
				useNode = keyCGNode;
			}

			if(creation instanceof SSAGetInstruction){
				name = ((SSAGetInstruction) creation).getDeclaredField().getDeclaringClass().getName();
				node = threadSigNodeMap.get(name);
				if(node!=null)
					return  node;
			}else if(creation instanceof SSAPutInstruction){
				name = ((SSAPutInstruction) creation).getDeclaredField().getDeclaringClass().getName();
				node = threadSigNodeMap.get(name);
				if(node!=null)
					return  node;
			}else if(creation instanceof SSANewInstruction){
				name = ((SSANewInstruction) creation).getConcreteType().getName();
				if(name.toString().contains("Ljava/lang/Thread")){
					name = useNode.getMethod().getDeclaringClass().getName();
				}
				node = threadSigNodeMap.get(name);
				if(node!=null)
					return  node;
			}else if(creation instanceof SSAAbstractInvokeInstruction){
				name = ((SSAAbstractInvokeInstruction) creation).getCallSite().getDeclaredTarget().getDeclaringClass().getName();
				node = threadSigNodeMap.get(name);
				if (node != null) {
					return node;
				}else {
					name = useNode.getMethod().getDeclaringClass().getName();
					node = threadSigNodeMap.get(name);
					if(node!=null)
						return  node;
					else{
						//special case
						Iterator<TypeName> iterator = threadSigNodeMap.keySet().iterator();
						while(iterator.hasNext()){
							TypeName key = iterator.next();
							if(key.toString().contains(name.toString())){
								return threadSigNodeMap.get(key);
							}
						}
					}
				}
			}
			//example: Critical: Thread t = new Thread(Class implements Runnable)
			//example: raytracer:
			//find out the initial of this Ljava/lang/Thread
			SSAAbstractInvokeInstruction initial = threadInits.get(param);
			if(initial != null){
				param = initial.getUse(1);
				return handleRunnable(instKey, param, useNode);
			}else{
				//because: assignments + ssa; array references
				int new_param = findDefsInDataFlowFor(useNode, param, creation.iindex);
				if(new_param != -1){
					node = handleRunnable(instKey, new_param, useNode);
					if(node != null)
						return node;
				}
				if(creation instanceof SSAArrayLoadInstruction){
					new_param = ((SSAArrayLoadInstruction)creation).getArrayRef();
				}
				while (node == null){
					new_param = findDefsInDataFlowFor(useNode, new_param, creation.iindex);
					node = handleRunnable(instKey, new_param, useNode);
				}
				return node;
			}
		}
		return null;
	}



	private int findDefsInDataFlowFor(CGNode node, int param, int idx) {
		if(param == -1)
			return -1;
		int def = -1;
		Iterator<SSAInstruction> defInsts = node.getDU().getUses(param);
		while(defInsts.hasNext()){
			SSAInstruction defInst = defInsts.next();
			int didx = defInst.iindex;
			if(didx < idx){
				int temp = -1;
				if(defInst instanceof SSANewInstruction){
					SSANewInstruction tnew = (SSANewInstruction) defInst;
					temp = tnew.getDef();
				}else if(defInst instanceof SSAArrayStoreInstruction){
					SSAArrayStoreInstruction astore = (SSAArrayStoreInstruction) defInst;
					temp = astore.getValue();
				}else if(defInst instanceof SSAPutInstruction){
					SSAPutInstruction fput = (SSAPutInstruction) defInst;
					temp = fput.getVal();
				}
				if(temp != param && temp != -1){
					def = temp;
				}
			}
		}
		return def;
	}



	private void logArrayAccess(SSAInstruction inst, int sourceLineNum, String instSig, Trace curTrace, CGNode n,
			PointerKey key, OrdinalSet<InstanceKey> instances, IFile file, String field) {
		String sig = "array.";
		if(inst instanceof SSAArrayLoadInstruction){//read
			ReadNode readNode = new ReadNode(curTID,instSig,sourceLineNum,key, sig, n, inst, file);
			for (InstanceKey instanceKey : instances) {
				String sig2 = sig + instanceKey.hashCode();
				readNode.addObjSig(sig2);
				curTrace.addRsigMapping(sig2, readNode);
			}
			readNode.setLocalSig(field);
			//add node to trace
			curTrace.add(readNode);
			//pointer rw map
			HashSet<MemNode> rwlist = pointer_rwmap.get(key);
			if(rwlist == null){
				rwlist = new HashSet<>();
				rwlist.add(readNode);
				pointer_rwmap.put(key, rwlist);
			}else{
				rwlist.add(readNode);
			}
		}else {//write
			WriteNode writeNode = new WriteNode(curTID,instSig,sourceLineNum, key, sig, n, inst, file);
			for (InstanceKey instanceKey : instances) {
				String sig2 = sig+ instanceKey.hashCode();
				writeNode.addObjSig(sig2);
				curTrace.addWsigMapping(sig2, writeNode);
			}
			writeNode.setLocalSig(field);
			//add node to trace
			curTrace.add(writeNode);
			//pointer rw map
			HashSet<MemNode> rwlist = pointer_rwmap.get(key);
			if(rwlist == null){
				rwlist = new HashSet<>();
				rwlist.add(writeNode);
				pointer_rwmap.put(key, rwlist);
			}else{
				rwlist.add(writeNode);
			}
		}
	}



	private void logFieldAccess(SSAInstruction inst, int sourceLineNum, String instSig, Trace curTrace, CGNode n,
			PointerKey key, OrdinalSet<InstanceKey> instances, String sig, IFile file) {
		boolean exclude = false;
		if(excludedSigForRace.contains(sig)){
			exclude = true;
		}
		HashSet<String> sigs = new HashSet<>();
		if(inst instanceof SSAGetInstruction){//read
			ReadNode readNode;
			if(key != null){
				for (InstanceKey instanceKey : instances) {
					String sig2 = sig+"."+String.valueOf(instanceKey.hashCode());
					sigs.add(sig2);
				}
				readNode = new ReadNode(curTID,instSig,sourceLineNum,key, sig, n, inst, file);
				readNode.setObjSigs(sigs);
				//excluded sigs
				if(exclude){
					HashSet<ReadNode> exReads = excludedReadSigMapping.get(sig);
					if(exReads == null){
						exReads = new HashSet<ReadNode>();
						excludedReadSigMapping.put(sig, exReads);
					}
					exReads.add(readNode);
					return;
				}
				for (String sig2 : sigs) {
					curTrace.addRsigMapping(sig2, readNode);
				}
				//add node to trace
				curTrace.add(readNode);
				//pointer rw map
				HashSet<MemNode> rwlist = pointer_rwmap.get(key);
				if(rwlist == null){
					rwlist = new HashSet<>();
					rwlist.add(readNode);
					pointer_rwmap.put(key, rwlist);
				}else{
					rwlist.add(readNode);
				}
			}else{//static
				readNode = new ReadNode(curTID,instSig,sourceLineNum,key, sig, n, inst,file);
				readNode.addObjSig(sig);
				//excluded sigs
				if(exclude){
					HashSet<ReadNode> exReads = excludedReadSigMapping.get(sig);
					if(exReads == null){
						exReads = new HashSet<ReadNode>();
						excludedReadSigMapping.put(sig, exReads);
					}
					exReads.add(readNode);
					return;
				}
				//add node to trace
				curTrace.add(readNode);
				curTrace.addRsigMapping(sig, readNode);
			}
		}else{//write
			WriteNode writeNode;
			if(key != null){
				for (InstanceKey instanceKey : instances) {
					String sig2 = sig+"."+String.valueOf(instanceKey.hashCode());
					sigs.add(sig2);
				}
				writeNode = new WriteNode(curTID,instSig,sourceLineNum,key, sig, n, inst, file);
				writeNode.setObjSigs(sigs);;
				//excluded sigs
				if(exclude){
					HashSet<WriteNode> exWrites = excludedWriteSigMapping.get(sig);
					if(exWrites == null){
						exWrites = new HashSet<WriteNode>();
						excludedWriteSigMapping.put(sig, exWrites);
					}
					exWrites.add(writeNode);
					return;
				}
				for (String sig2 : sigs) {
					curTrace.addWsigMapping(sig2, writeNode);
				}
				//add node to trace
				curTrace.add(writeNode);
				//pointer rw map
				HashSet<MemNode> rwlist = pointer_rwmap.get(key);
				if(rwlist == null){
					rwlist = new HashSet<>();
					rwlist.add(writeNode);
					pointer_rwmap.put(key, rwlist);
				}else{
					rwlist.add(writeNode);
				}
			}else{//static
				writeNode = new WriteNode(curTID,instSig,sourceLineNum,key, sig, n, inst, file);
				writeNode.addObjSig(sig);
				//excluded sigs
				if(exclude){
					HashSet<WriteNode> exWrites = excludedWriteSigMapping.get(sig);
					if(exWrites == null){
						exWrites = new HashSet<WriteNode>();
						excludedWriteSigMapping.put(sig, exWrites);
					}
					exWrites.add(writeNode);
					return;
				}
				//add node to trace
				curTrace.add(writeNode);
				curTrace.addWsigMapping(sig, writeNode);
			}
		}
	}


	public synchronized void addSharedVars(HashSet<String> sf) {
		sharedFields.addAll(sf);
	}

	public synchronized void addSigReadNodes(HashMap<String, HashSet<ReadNode>> sigReadNodes2) {
		for(String key : sigReadNodes2.keySet()){
			HashSet<ReadNode> readNodes = sigReadNodes2.get(key);
			if(sigReadNodes.containsKey(key)){
				sigReadNodes.get(key).addAll(readNodes);
			}else{
				sigReadNodes.put(key, readNodes);
			}
		}
	}

	public synchronized void addSigWriteNodes(HashMap<String, HashSet<WriteNode>> sigWriteNodes2) {
		for(String key : sigWriteNodes2.keySet()){
			HashSet<WriteNode> writeNodes = sigWriteNodes2.get(key);
			if(sigWriteNodes.containsKey(key)){
				sigWriteNodes.get(key).addAll(writeNodes);
			}else{
				sigWriteNodes.put(key, writeNodes);
			}
		}
	}

	public synchronized void removeBugs(HashSet<TIDERace> removes) {
		this.removedbugs.addAll(removes);
	}

	/**
	 * for processIncreRecheckCommonLocks
	 */
	public HashSet<TIDERace> recheckRaces = new HashSet<>();
	public synchronized void addRecheckBugs(MemNode wnode, MemNode xnode) {
		TIDERace recheck = new TIDERace(wnode, xnode);
		if(!recheckRaces.contains(recheck)){
			recheckRaces.add(recheck);
		}
	}


	public synchronized void addBugsBack(HashSet<ITIDEBug> bs) {
		Iterator<ITIDEBug> iterator = bs.iterator();
		while(iterator.hasNext()){
			ITIDEBug _bug = iterator.next();
			if (_bug instanceof TIDEDeadlock) {
				boolean iscontain = false;
				Iterator<ITIDEBug> iter = bugs.iterator();
				while(iter.hasNext()) {
					ITIDEBug exist = (ITIDEBug) iter.next();
					if(exist instanceof TIDEDeadlock){
						TIDEDeadlock bug = (TIDEDeadlock) exist;
						if(_bug.equals(bug)){
							iscontain = true;
						}
					}
				}
				if(!iscontain){
					bugs.add(_bug);
//					addedbugs.add(_bug);
				}
			}else if(_bug instanceof TIDERace){//race bug:
				boolean iscontain = false;
				Iterator<ITIDEBug> iter = bugs.iterator();
				while(iter.hasNext()) {
					ITIDEBug exist = (ITIDEBug) iter.next();
					if(exist instanceof TIDERace){
						TIDERace race = (TIDERace) exist;
						if(_bug.equals(race)){
							iscontain = true;
						}
					}
				}
				if(!iscontain){
					bugs.add(_bug);
//					addedbugs.add(_bug);
					System.err.println("all bugs: " + bugs);
				}
			}
		}
	}



	private HashSet<SSAInstruction> InstInsideCatchBlock(SSACFG cfg) {
		HashSet<SSAInstruction> catchinsts = new HashSet<>();
		for(int i=0; i<=cfg.getMaxNumber(); i++){
			BasicBlock block = cfg.getBasicBlock(i);
			if(block.isCatchBlock()){
				List<SSAInstruction> insts = block.getAllInstructions();
				catchinsts.addAll(insts);
				Iterator<ISSABasicBlock> succss = cfg.getSuccNodes(block);
				while(succss.hasNext()){
					BasicBlock succ = (BasicBlock) succss.next();
					List<SSAInstruction> insts2 = succ.getAllInstructions();
					for (SSAInstruction inst2 : insts2) {
						if(inst2.toString().contains("start()V")
								||inst2.toString().contains("join()V")){
							continue;
						}
						catchinsts.add(inst2);
					}
				}
			}
		}
		return catchinsts;
	}



}