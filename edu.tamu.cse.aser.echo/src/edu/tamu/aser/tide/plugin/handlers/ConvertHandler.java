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
	public EchoRaceView echoRaceView;
	public EchoReadWriteView echoRWView;
	public EchoDLView echoDLView;

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

	public TIDECGModel getCurrentModel(){
		return currentModel;
	}

	public IJavaProject getCurrentProject(){
		return currentProject;
	}

	private int num_of_detection = 0;

	private void startDetection(IJavaProject javaProject, final IFile file, final TIDECGModel model){
		num_of_detection = 1;
		new Thread(new Runnable(){
			@Override
			public void run() {
				HashSet<ITIDEBug> bugs = new HashSet<ITIDEBug>();
				// Detect Bugs
				long start_time = System.currentTimeMillis();
				if(num_of_detection == 1){
					//initial
					System.err.println("INITIAL DETECTION >>>");
					bugs =  model.detectBug();
					System.out.println("DETECTION FINISHED");
				}else{
					System.out.println("wrong call");
				}
				//update UI
				model.updateGUI(javaProject, file, bugs, true);
				System.err.println("Total Bugs: "+ bugs.size());
				System.err.println("Total Detection Time: "+(System.currentTimeMillis()-start_time));
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
				// record total running time
				init(cu, selection);//initial
			}
		}
		return null;
	}


	public void init(ICompilationUnit cu, IStructuredSelection selection){
		try{
			IJavaProject javaProject = cu.getJavaProject();
			// the main function of target file
			// e.g. Test.main([Ljava/lang/String;)V
			String mainSig = getSignature(cu);
			// exclude some common library
			// add new exclusion file in data/EclipseDefaultExclusions.txt
			TIDECGModel model = new TIDECGModel(javaProject, "EclipseDefaultExclusions.txt", mainSig);
			// record call graph constructon time
			long start_time = System.currentTimeMillis();
			model.buildGraph();
			System.err.println("Call Graph Construction Time: "+(System.currentTimeMillis()-start_time));
			modelMap.put(javaProject, model);
			IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(cu.getPath());
			// set echoview to menuhandler
			echoRaceView = model.getEchoRaceView();
			echoRWView = model.getEchoRWView();
			echoDLView = model.getEchoDLView();
			// set current model
			currentModel = model;
			currentProject = javaProject;
			// start concurrent bug detection
			startDetection(javaProject, file, model);
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
