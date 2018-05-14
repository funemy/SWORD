package edu.tamu.aser.tide.plugin.handlers;

import java.util.HashMap;
import java.util.HashSet;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.SourceType;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.HandlerUtil;
//import org.eclipse.swt.widgets.DirectoryDialog;
//import org.eclipse.swt.widgets.Shell;

import com.ibm.wala.ipa.callgraph.CGNode;

import edu.tamu.aser.tide.engine.ITIDEBug;
import edu.tamu.aser.tide.engine.TIDECGModel;
import edu.tamu.aser.tide.plugin.Activator;
import edu.tamu.aser.tide.views.EchoDLView;
import edu.tamu.aser.tide.views.EchoRaceView;
import edu.tamu.aser.tide.views.EchoReadWriteView;

public class ConvertHandler extends AbstractHandler {

	public static final String DESC_MAIN = "([Ljava/lang/String;)V";
//	public ExcludeView excludeView;
	public EchoRaceView echoRaceView;
	public EchoReadWriteView echoRWView;
	public EchoDLView echoDLView;
//	private static String EXCLUSIONS =
//			//wala
////			"java\\/awt\\/.*\n" +
////			"javax\\/swing\\/.*\n" +
////			"sun\\/awt\\/.*\n" +
////			"sun\\/swing\\/.*\n" +
////			"com\\/sun\\/.*\n" +
////			"sun\\/.*\n" +
////			"org\\/netbeans\\/.*\n" +
////			"org\\/openide\\/.*\n" +
////			"com\\/ibm\\/crypto\\/.*\n" +
////			"com\\/ibm\\/security\\/.*\n" +
////			"org\\/apache\\/xerces\\/.*\n" +
////			"java\\/security\\/.*\n" + "";


	public ConvertHandler() throws PartInitException{
		super();
		Activator.getDefault().setCHandler(this);
	}

	public EchoRaceView getEchoRaceView(){
		return echoRaceView;
	}

	public EchoDLView getEchoDLView(){
		return echoDLView;
	}

	public EchoReadWriteView getEchoReadWriteView(){
		return echoRWView;
	}

	private HashMap<IJavaProject,TIDECGModel> modelMap = new HashMap<IJavaProject,TIDECGModel>();
	private TIDECGModel currentModel;
	private IJavaProject currentProject;

	public HashSet<CGNode> changedNodes = new HashSet<>();
	public HashSet<CGNode> changedModifiers = new HashSet<>();//only for sync method
	public HashSet<CGNode> ignoreNodes = new HashSet<>();
	public HashSet<CGNode> considerNodes = new HashSet<>();

	long start_time = System.currentTimeMillis();

	public TIDECGModel getCurrentModel(){
		return currentModel;
	}

	public IJavaProject getCurrentProject(){
		return currentProject;
	}

	private int num_of_detection = 0;

	private void letUsRock(IJavaProject javaProject, final IFile file, final TIDECGModel model){
		num_of_detection = 1;
		new Thread(new Runnable(){
			@Override
			public void run() {
				HashSet<ITIDEBug> bugs = new HashSet<ITIDEBug>();
				//Detect Bugs
				if(num_of_detection == 1){
					//initial
					System.err.println("INITIAL DETECTION >>>");
					bugs =  model.detectBug();
				}else{
					System.out.println("wrong call");
				}
				//update UI
				model.updateGUI(javaProject, file, bugs, true);
				System.err.println("Total Bugs: "+ bugs.size());
				System.err.println("Total Time: "+(System.currentTimeMillis()-start_time));
			}
		}).start();
	}


	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection sel = HandlerUtil.getActiveMenuSelection(event);
		IStructuredSelection selection = (IStructuredSelection) sel;
		Object firstElement = selection.getFirstElement();
		if (firstElement instanceof ICompilationUnit) {

			ICompilationUnit cu = (ICompilationUnit) firstElement;
			if(hasMain(cu)){
				test(cu, selection);//initial
			}
		}
		return null;
	}


	public void test(ICompilationUnit cu, IStructuredSelection selection){
		try{
			IJavaProject javaProject = cu.getJavaProject();
			String mainSig = getSignature(cu);
			//excluded in text file
//			TIDECGModel model = new TIDECGModel(javaProject, "EclipseDefaultExclusions.txt", mainSig);
			//excluded in String
//			String deafaultDefined = excludeView.getDefaultText();
//			String userDefined = excludeView.getChangedText();
//			String new_exclusions = null;
//			if(userDefined.length() > 0){
//	            //append new added in excludeview
//				StringBuilder stringBuilder = new StringBuilder();
//				stringBuilder.append(deafaultDefined);
//				stringBuilder.append(userDefined);
//				new_exclusions = stringBuilder.toString();//combined
//				//write back to EclipseDefaultExclusions.txt
//				java.io.File file = new java.io.File("/Users/Bozhen/Documents/Eclipse2/Test_both_copy/edu.tamu.cse.aser.echo/data/EclipseDefaultExclusions.txt");
//				FileWriter fileWriter = new FileWriter(file, false);
//				BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
//				bufferedWriter.write(new_exclusions);
//				bufferedWriter.close();
//			}
			TIDECGModel model = new TIDECGModel(javaProject, "EclipseDefaultExclusions.txt", mainSig);
			model.buildGraph();
			System.err.println("Call Graph Construction Time: "+(System.currentTimeMillis()-start_time));
			modelMap.put(javaProject, model);
//			excludeView.setProgramInfo(cu, selection);
			IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(cu.getPath());
			//set echoview to menuhandler
			echoRaceView = model.getEchoRaceView();
			echoRWView = model.getEchoRWView();
			echoDLView = model.getEchoDLView();
			//set current model
			currentModel = model;
			currentProject = javaProject;
			//concurrent
			letUsRock(javaProject, file, model);
			Activator.getDefaultReporter().initialSubtree(cu, selection, javaProject);
		}catch(Exception e){
			e.printStackTrace();
		}
	}


	private boolean hasMain(ICompilationUnit cu){
		try{
			for(IJavaElement e: cu.getChildren()){
				if(e instanceof SourceType){
					SourceType st = (SourceType)e;
					for (IMethod m: st.getMethods())
						if((m.getFlags()&Flags.AccStatic)>0
								&&(m.getFlags()&Flags.AccPublic)>0
								&&m.getElementName().equals("main")
								&&m.getSignature().equals("([QString;)V")){
							return true;
						}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	private String getSignature(ICompilationUnit cu){
		try {
			String name = cu.getElementName();
			int index = name.indexOf(".java");
			name = name.substring(0,index);
			for(IType t :cu.getTypes()){
				String tName = t.getElementName();
				if(name.equals(tName))
					return t.getFullyQualifiedName()+".main"+DESC_MAIN;
			}
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
		return "";
	}


	protected String getPersistentProperty(IResource res, QualifiedName qn) {
		try {
			return res.getPersistentProperty(qn);
		} catch (CoreException e) {
			return "";
		}
	}

	protected void setPersistentProperty(IResource res, QualifiedName qn,
			String value) {
		try {
			res.setPersistentProperty(qn, value);
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}



}
