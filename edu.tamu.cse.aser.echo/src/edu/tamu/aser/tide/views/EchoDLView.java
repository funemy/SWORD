package edu.tamu.aser.tide.views;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.texteditor.AbstractTextEditor;

import edu.tamu.aser.tide.engine.ITIDEBug;
import edu.tamu.aser.tide.engine.TIDEDeadlock;
import edu.tamu.aser.tide.engine.TIDEEngine;



public class EchoDLView extends ViewPart{

	protected TreeViewer treeViewer;
	protected Text text;
	protected BugLabelProvider labelProvider;
	protected DeadlockDetail deadlockDetail;
	protected Action jumpToLineInEditor;

	protected TIDEEngine bugEngine;
	protected HashSet<ITIDEBug> existingbugs = new HashSet<>();

	public EchoDLView() {
		super();
	}

	public void setEngine(TIDEEngine bugEngine) {
		this.bugEngine = bugEngine;
	}

	@Override
	public void createPartControl(Composite parent) {
		/* Create a grid layout object so the text and treeviewer
		 * are layed out the way I want. */
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		layout.verticalSpacing = 2;
		layout.marginWidth = 0;
		layout.marginHeight = 2;
		parent.setLayout(layout);

		/* Create a "label" to display information in. I'm
		 * using a text field instead of a lable so you can
		 * copy-paste out of it. */
		text = new Text(parent, SWT.READ_ONLY | SWT.SINGLE | SWT.BORDER);
		// layout the text field above the treeviewer
		GridData layoutData = new GridData();
		layoutData.grabExcessHorizontalSpace = true;
		layoutData.horizontalAlignment = GridData.FILL;
		text.setLayoutData(layoutData);

		// Create the tree viewer as a child of the composite parent
		treeViewer = new TreeViewer(parent);
		treeViewer.setContentProvider(new BugContentProvider());
		labelProvider = new BugLabelProvider();
		treeViewer.setLabelProvider(labelProvider);
		treeViewer.setUseHashlookup(true);

		// layout the tree viewer below the text field
		layoutData = new GridData();
		layoutData.grabExcessHorizontalSpace = true;
		layoutData.grabExcessVerticalSpace = true;
		layoutData.horizontalAlignment = GridData.FILL;
		layoutData.verticalAlignment = GridData.FILL;
		treeViewer.getControl().setLayoutData(layoutData);

		createActions();
		hookListeners();
		initializeTree();

		//create a context menu in the view
		MenuManager manager = new MenuManager();
		Control control = treeViewer.getControl();
		Menu menu = manager.createContextMenu(control);
		control.setMenu(menu);
		getSite().registerContextMenu("edu.tamu.aser.tide.views.echomenu", manager, treeViewer);
	}


	public void initializeTree(BugDetail bugDetail) {
		//initialize the tree
		deadlockDetail = new DeadlockDetail(bugDetail);
		bugDetail.setDeadlockDetail(deadlockDetail);
	}

	private void initializeTree() {
		//initialize the tree
		deadlockDetail = new DeadlockDetail(null);
	}

	protected void createActions() {
		jumpToLineInEditor = new Action() {
			@Override
			public void run(){
				ISelection selection = treeViewer.getSelection();
				Object obj = ((IStructuredSelection) selection).getFirstElement();
				if (obj instanceof EventNode) {
					ITreeNode parent = ((EventNode) obj).getParent().getParent();
					if(parent instanceof RaceNode){
						RaceNode race = (RaceNode) parent;
						HashMap<String, IFile> map = race.race.event_ifile_map;
						IFile file = map.get(((EventNode) obj).getName());

						IEditorRegistry editorRegistry = PlatformUI.getWorkbench().getEditorRegistry();
						String editorId = editorRegistry.getDefaultEditor(file.getFullPath().toString()).getId();
						IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
						try {
							AbstractTextEditor ePart = (AbstractTextEditor) page.openEditor(new FileEditorInput(file),editorId);
							IDocument document = ePart.getDocumentProvider().getDocument(ePart.getEditorInput());
							if (document != null) {
								IRegion lineInfo = null;
								try {
									HashMap<String, Integer> map2 = race.race.event_line_map;
									int line = map2.get(((EventNode) obj).getName());
									lineInfo = document.getLineInformation(line - 1);
								} catch (BadLocationException e) {
									e.printStackTrace();
								}
								if (lineInfo != null) {
									ePart.selectAndReveal(lineInfo.getOffset(),
											lineInfo.getLength());
								}
							}
						} catch (PartInitException e) {
							e.printStackTrace();
						}
					}else if(parent instanceof DeadlockNode){
						DeadlockNode dl = (DeadlockNode) parent;
						HashMap<String, IFile> map = dl.deadlock.event_ifile_map;
						IFile file = map.get(((EventNode) obj).getName());
						if(file == null)
							return;

						IEditorRegistry editorRegistry = PlatformUI.getWorkbench().getEditorRegistry();
						String editorId = editorRegistry.getDefaultEditor(file.getFullPath().toString()).getId();
						IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
						try {
							AbstractTextEditor ePart = (AbstractTextEditor) page.openEditor(new FileEditorInput(file),editorId);
							IDocument document = ePart.getDocumentProvider().getDocument(ePart.getEditorInput());
							if (document != null) {
								IRegion lineInfo = null;
								try {
									HashMap<String, Integer> map2 = dl.deadlock.event_line_map;
									int line = map2.get(((EventNode) obj).getName());
									lineInfo = document.getLineInformation(line - 1);
								} catch (BadLocationException e) {
									e.printStackTrace();
								}
								if (lineInfo != null) {
									ePart.selectAndReveal(lineInfo.getOffset(),
											lineInfo.getLength());
								}
							}
						} catch (PartInitException e) {
							e.printStackTrace();
						}
					}
				}else if(obj instanceof FixNode){
					//TODO: show the fix view?
				}else{
//					true if the node is expanded, and false if collapsed
					 if(treeViewer.getExpandedState(obj)){
						 treeViewer.collapseToLevel(obj, 1);
					 }else{
						 treeViewer.expandToLevel(obj, 1);
					 }
				}
			}
		};
	}


	public void initialGUI(Set<ITIDEBug> bugs) {
		//clear all
		deadlockDetail.clear();
		existingbugs.clear();
		//refresh
		treeViewer.refresh();
		translateToInput((HashSet<ITIDEBug>) bugs);
		treeViewer.setInput(deadlockDetail);
		existingbugs.addAll(bugs);
		treeViewer.expandToLevel(deadlockDetail, 1);
	}


	public void updateGUI(HashSet<ITIDEBug> addedbugs, HashSet<ITIDEBug> removedbugs) {
		//only update changed bugs
		for (ITIDEBug removed : removedbugs) {
			if(existingbugs.contains(removed)){
				if(removed instanceof TIDEDeadlock){
					TIDEDeadlock deadlock = (TIDEDeadlock) removed;
					deadlockDetail.removeChild(deadlock);
				}
				existingbugs.remove(removed);
			}else{
				System.err.println("Existing bugs should contain this removed bug.");
			}
		}
		//add new
		addToInput(addedbugs);
		existingbugs.addAll(addedbugs);
		treeViewer.refresh();
		treeViewer.expandToLevel(deadlockDetail, 1);
	}


	public void considerBugs(HashSet<ITIDEBug> addedbugs) {
		addToInput(addedbugs);
		existingbugs.addAll(addedbugs);
		treeViewer.refresh();
		treeViewer.expandToLevel(deadlockDetail, 1);
	}


	public void ignoreBugs(HashSet<ITIDEBug> ignores){
		for (ITIDEBug ignore : ignores) {
			if(existingbugs.contains(ignore)){
				if(ignore instanceof TIDEDeadlock){
					TIDEDeadlock deadlock = (TIDEDeadlock) ignore;
					deadlockDetail.removeChild(deadlock);
				}
				existingbugs.remove(ignore);
			}else{
				System.err.println("Existing bugs should contain this removed bug.");
			}
		}

		treeViewer.refresh();
		treeViewer.expandToLevel(deadlockDetail, 1);
	}


	public boolean updateGUI(Set<ITIDEBug> bugs) {//update all, not efficient
		//remove old
		treeViewer.remove(deadlockDetail);
		treeViewer.refresh();
		//also remove things in bugdetail
		translateToInput((HashSet<ITIDEBug>) bugs);
		treeViewer.setInput(deadlockDetail);
		return true;
	}

	private void translateToInput(HashSet<ITIDEBug> bugs) {
		deadlockDetail.clear();
		for (ITIDEBug bug : bugs) {
			if(bug instanceof TIDEDeadlock){
				deadlockDetail.createChild((TIDEDeadlock) bug);
			}
		}
	}

	private void addToInput(HashSet<ITIDEBug> bugs) {
		for (ITIDEBug bug : bugs) {
			if(bug instanceof TIDEDeadlock){
				deadlockDetail.createChild((TIDEDeadlock) bug, true);
			}
		}
	}

	protected void hookListeners() {
		treeViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				jumpToLineInEditor.run();
			}
		});
	}



	@Override
	public void setFocus() {
	}



}