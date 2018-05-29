package edu.tamu.aser.tide.nodes;

import java.util.HashSet;

import org.eclipse.core.resources.IFile;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ssa.SSAInstruction;

public abstract class MemNode implements INode {
	final int TID;
	String addr, sig;
	int line;
	PointerKey pointerKey;
	private HashSet<String> objsigs = new HashSet<>();
	private String prefix;
	protected CGNode node;
	public SSAInstruction inst;
	public IFile file;
	public String localSig = "";


	public MemNode(int curTID, String instSig, int sourceLineNum, PointerKey key,
			String prefix, CGNode node, SSAInstruction inst, IFile file) {//current used
		this.TID = curTID;
		this.sig = instSig;
		this.line = sourceLineNum;
		this.pointerKey = key;
		this.prefix = prefix;
		this.node = node;
		this.inst = inst;
		this.file = file;
	}

	public void setLocalSig(String lsig){
		this.localSig = lsig;
	}

	public String getLocalSig() {
		return localSig;
	}

	public IFile getFile(){
		return file;
	}

//	public int hashCode(){
//		return objsigs.hashCode() + pointerKey.hashCode();
//	}


	@Override
	public boolean equals(Object that){
		if(that instanceof MemNode){
			MemNode thatnode = (MemNode) that;
			if((this instanceof ReadNode && that instanceof ReadNode)
					|| (this instanceof WriteNode && that instanceof WriteNode)){
				if(this.objsigs.equals(thatnode.objsigs)
						&& this.prefix.equals(thatnode.prefix)
						&& this.localSig.equals(((MemNode) that).localSig)
//						&& this.file.equals(thatnode.file)
//						&& this.line == ((MemNode) that).line//line??
						){
					return true;
				}
			}
		}
		return false;
	}

	public void replaceObjSig(HashSet<String> new_sigs){
		objsigs.clear();
		objsigs.addAll(new_sigs);
	}

	public CGNode getBelonging(){
		return node;
	}

	public void setBelonging(CGNode node){
		this.node = node;
	}

	public String getPrefix(){
		return prefix;
	}

	public void setPrefix(String prefix){
		this.prefix = prefix;
	}

	public void addObjSig(String sig){
		objsigs.add(sig);
	}

	public void setObjSigs(HashSet<String> sigs){
		objsigs.addAll(sigs);
	}

	public HashSet<String> getObjSig(){
		return objsigs;
	}

	public PointerKey getPointer(){
		return pointerKey;
	}

	public String getAddress()
	{
		return addr;
	}
	public int getTID() {
		return TID;
	}

	public String getSig(){
		return sig;
	}

	public void setSig(String sig){
		this.sig = sig;
	}

	public int getLine(){
		return line;
	}

	public void setLine(int line){
		this.line = line;
	}

	public void setInst(SSAInstruction inst){
		this.inst = inst;
	}


}
