package edu.tamu.aser.tide.tests;


import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.Set;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.DefaultEntrypoint;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysisImpl;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PropagationGraph;
import com.ibm.wala.ipa.callgraph.propagation.SSAPropagationCallGraphBuilder;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.io.FileProvider;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import edu.tamu.aser.tide.akkasys.BugHub;
import edu.tamu.aser.tide.engine.AnalysisUtils;
import edu.tamu.aser.tide.engine.ITIDEBug;
import edu.tamu.aser.tide.engine.TIDEDeadlock;
import edu.tamu.aser.tide.engine.TIDEEngine;
import edu.tamu.aser.tide.engine.TIDERace;
import edu.tamu.aser.tide.nodes.DLockNode;
import edu.tamu.aser.tide.nodes.MemNode;
import edu.tamu.aser.tide.plugin.handlers.ConvertHandler;

public class Test {
	static PrintStream ps;
	private static long totaltime;
	public static TIDEEngine engine;
	static void print(String msg, boolean printErr)
	{
		try{
			if(ps==null)
				ps = new PrintStream(new FileOutputStream("log_d4_tradesoap"));

			ps.println(msg);

			if(printErr)
				System.err.println(msg);
		}catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	public static void main(String[] args) {
		try{
			boolean includeAllMainEntryPoints = true;
			String mainClassName ="rasterizer/Main";
			// avrora: Main
			// batik: rasterizer/Main
			// eclipse: EclipseStarter
			// fop: cli/Main
			// jython: PySystemState
			// jython: jython
			// luindex: Index -> fail
			// lusearch: Search
			// pmd: PMD
			// sunflow :Benchmark; mainSignature: kernelMain
			// xalan: XSLTBench

					//"IndexHTML";
			//"avrora/Main";
//			 "rasterizer/Main";
			//IndexHTML
//					h2:
//					"Console";
//			"Server";
//			"Recover";
//			'Restore';
//			'RunScript';
//			'Backup';
//			'Script';

			//for experiments
			AnalysisScope scope = AnalysisScopeReader.readJavaScope("data/test2.txt", (new FileProvider()).getFile("data/EclipseDefaultExclusions.txt"), Test.class.getClassLoader());
			ClassHierarchy cha = ClassHierarchy.make(scope);
			String mainSignature = ".main"+ConvertHandler.DESC_MAIN;;
			String mainMethodSig =
					mainClassName + mainSignature;
//			"rasterizer.Main" + mainSignature;
//					 "avrora.Main" + mainSignature;
//			         "SunflowGUI$";
//				     "driver.TestMulti";

			Iterable<Entrypoint> entrypoints = findEntryPoints(cha,mainClassName,includeAllMainEntryPoints);
			AnalysisOptions options = new AnalysisOptions(scope, entrypoints);

		    //parallel incremental experiment ->
			SSAPropagationCallGraphBuilder builder = Util.makeZeroOneContainerCFABuilder(options, new AnalysisCache(), cha, scope);
			long start_time = System.currentTimeMillis();
			CallGraph cg  = builder.makeCallGraph(options, null);
			PointerAnalysis<InstanceKey> pta = builder.getPointerAnalysis();
			System.out.println("Call Graph Construction Time: "+(System.currentTimeMillis()-start_time));

			print("Call Graph Construction Time: "+(System.currentTimeMillis()-start_time),true);
			int numofCGNodes = cg.getNumberOfNodes();

			int totalInstanceKey = pta.getInstanceKeys().size();
			int totalPointerKey =((PointerAnalysisImpl)pta).getNumOfPointerKeys();
			int totalPointerEdge = 0;
			int totalClass=cha.getNumberOfClasses();
			Iterator<PointerKey> iter = pta.getPointerKeys().iterator();
			while(iter.hasNext()){
				PointerKey key = iter.next();
				int size = pta.getPointsToSet(key).size();
				totalPointerEdge+=size;
			}

			print("Total Pointer Keys: "+totalPointerKey,true);
			print("Total Instance Keys: "+totalInstanceKey,true);
			print("Total Pointer Edges: "+totalPointerEdge,true);
			print("Total Classes: "+totalClass,true);
			print("Total Methods: "+numofCGNodes,true);
			ps.println();

			System.out.println();
			System.out.println();

//			//initial start bug akka system
			int nrOfWorkers = 8;//8;
			ActorSystem akkasys = ActorSystem.create();
		    ActorRef bughub = akkasys.actorOf(Props.create(BugHub.class, nrOfWorkers), "bughub");
			start_time = System.currentTimeMillis();
			PropagationGraph flowgraph = builder.getPropagationSystem().getPropagationGraph();
		    engine = new TIDEEngine((includeAllMainEntryPoints?mainSignature:mainMethodSig), cg, flowgraph, pta, bughub);
			Set<ITIDEBug> bugs = engine.detectBothBugs(ps);
			System.out.println("Total Trace in SHB graph: " + engine.shb.getAllTraces().size());
			System.out.println("Total Edge in SHB graph: " + engine.shb.getNumOfEdges());
			System.out.println("Total Nodes in SHB graph: " + engine.shb.getNumOfNodes());

			System.err.println("INITIAL DETECTION >>>");
			int race = 0;
			int dl = 0;
			for(ITIDEBug bug : bugs){
				if(bug instanceof TIDERace){
					race++;
					showUpRaces((TIDERace) bug);
				}else if (bug instanceof TIDEDeadlock){
					showUpDeadlocks((TIDEDeadlock) bug);
					dl++;
				}
			}
			System.out.println();
			System.out.println("detection time: " + (System.currentTimeMillis() - start_time));
			System.out.println("num of race: " + race + "  dl: " + dl);

		}catch(Exception e){
			e.printStackTrace();
		}
		ps.close();

	}

	private static void showUpDeadlocks(TIDEDeadlock deadlock) {
		DLockNode l11 = deadlock.lp1.lock1;
		DLockNode l12 = deadlock.lp1.lock2;
		DLockNode l21 = deadlock.lp2.lock1;
		DLockNode l22 = deadlock.lp2.lock2;

		String s11 = l11.instSig;
		String s12 = l12.instSig;
		String s21 = l21.instSig;
		String s22 = l22.instSig;

		String deadlockMsg = "Deadlock: ("+s11+","+s12+";"+s21+","+s22+")";
		print(deadlockMsg,true);
	}

	private static void showUpRaces(TIDERace race) {
		String sig = race.sig;
		MemNode rnode = race.node1;
		MemNode wnode = race.node2;
		int findex = sig.indexOf('.');
		int lindex = sig.lastIndexOf('.');
		if(findex!=lindex)
			sig =sig.substring(0, lindex);//remove instance hashcode

		String raceMsg = "Race: "+sig+" ("+rnode.getSig()+", "+wnode.getSig()+")";
		print(raceMsg,true);
	}


	public static Iterable<Entrypoint> findEntryPoints(IClassHierarchy classHierarchy, String mainClassName, boolean includeAll) {
		final Set<Entrypoint> result = HashSetFactory.make();
		Iterator<IClass> classIterator = classHierarchy.iterator();
		while (classIterator.hasNext()) {
			IClass klass = classIterator.next();
			if (!AnalysisUtils.isJDKClass(klass)) {
				for (IMethod method : klass.getDeclaredMethods()) {
					try {
						if(method.isStatic()&&method.isPublic()
								&&method.getName().toString().equals("main")
								&&method.getDescriptor().toString().equals(ConvertHandler.DESC_MAIN))
						{
							//Test: accept only one main entryPoint
							if(includeAll
									||klass.getName().toString().contains(mainClassName))
								result.add(new DefaultEntrypoint(method, classHierarchy));
						}
						else if(method.isPublic()&&!method.isStatic()
								&&method.getName().toString().equals("run")
								&&method.getDescriptor().toString().equals("()V"))
						{
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



}
