package edu.tamu.aser.tide.engine;

import java.lang.ref.WeakReference;
import java.util.Iterator;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.DefUse;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.util.collections.SparseVector;
import com.ibm.wala.util.graph.impl.NodeWithNumber;

public class AstCGNodeEcho extends NodeWithNumber implements CGNode {

	/**
	 * A Mapping from call site program counter (int) -> Object, where Object is a CGNode if we've discovered exactly one target for
	 * the site, or an IntSet of node numbers if we've discovered more than one target for the site.
	 */
	protected final SparseVector<Object> targets = new SparseVector<Object>();

	private WeakReference<IR> ir = new WeakReference<IR>(null);
	private WeakReference<DefUse> du = new WeakReference<DefUse>(null);
	/**
	 * The method this node represents.
	 */
	protected IMethod method;

	/**
	 * The context this node represents.
	 */
	private final Context context;

	public void setIR(IR ir)
	{
		this.ir = new WeakReference<IR>(ir);
	}
	public AstCGNodeEcho(IMethod method, Context C) {
		this.method = method;
		this.context = C;
		if (method != null && !method.isSynthetic() && method.isAbstract()) {
			assert !method.isAbstract() : "Abstract method " + method;
		}
		assert C != null;
	}

	@Override
	public IClassHierarchy getClassHierarchy() {
		return method.getClassHierarchy();
	}

	@Override
	public IMethod getMethod() {
		return method;
	}

	@Override
	public Context getContext() {
		return context;
	}

	@Override
	public boolean addTarget(CallSiteReference site, CGNode target) {
		return false;
	}

	@Override
	public IR getIR() {
		return ir.get();
	}

	CGNode n;
	public void setCGNode(CGNode n)
	{
		this.n = n;
	}
	public CGNode getCGNode()
	{
		return n;
	}
	@Override
	public DefUse getDU() {
		DefUse du = this.du.get();
		if (du == null) {

			this.du = new WeakReference<DefUse>(du);
		}
		return du;
	}

	public AstCGNodeEcho getCallGraph() {
		return AstCGNodeEcho.this;
	}



	@Override
	public boolean delTarget(CallSiteReference callSite, CGNode target) {
		return false;
	}

	@Override
	public Iterator<NewSiteReference> iterateNewSites() {
		return null;
	}

	@Override
	public Iterator<CallSiteReference> iterateCallSites() {
		return null;
	}

}
