package edu.tamu.aser.tide.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import org.eclipse.core.resources.IFile;

import edu.tamu.aser.tide.nodes.MemNode;
import edu.tamu.aser.tide.nodes.ReadNode;
import edu.tamu.aser.tide.nodes.WriteNode;

public class TIDERace implements ITIDEBug{

	public final MemNode node1;
	public final MemNode node2;
	public String sig;
	public String initsig;
	public int tid1;
	public int tid2;
	public String raceMsg, fixMsg;
	public ArrayList<LinkedList<String>> traceMsg;
	public HashMap<String, IFile> event_ifile_map = new HashMap<>();
	public HashMap<String, Integer> event_line_map = new HashMap<>();

	/**
	 * for recheck bugs
	 * @param node1
	 * @param node2
	 */
	public TIDERace(MemNode node1, MemNode node2){
		this.node1=node1;
		this.node2=node2;
		this.sig = "";
	}

	/**
	 * constructor
	 * @param sig
	 * @param xnode
	 * @param xtid
	 * @param wnode
	 * @param wtid
	 */
	public TIDERace(String sig, MemNode xnode, int xtid, WriteNode wnode, int wtid) {
		setUpSig(sig);
		this.node1 = xnode;
		this.node2 = wnode;
		this.tid1 = xtid;
		this.tid2 = wtid;
		this.initsig = sig;
	}

	public void setUpSig(String sig){
		int index1 = sig.indexOf(".");
		if(sig.substring(0,index1).contains("array"))
			this.sig = sig.substring(0,index1);
		else{
			int index2 = sig.substring(index1+1).indexOf(".");
			if(index2>0)//must be suffixed with .'hashcode'
				this.sig = sig.substring(0,index1+index2+1);
			else
				this.sig = sig;
		}
	}

	public HashMap<String, Integer> getEventLineMap(){
		return event_line_map;
	}

	public void addEventLineToMap(String event, int line){
		event_line_map.put(event, line);
	}

	public HashMap<String, IFile> getEventIFileMap(){
		return event_ifile_map;
	}

	public void addEventIFileToMap(String event, IFile ifile){
		event_ifile_map.put(event, ifile);//check later
	}

	public int hashCode()
	{
		return sig.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if(o instanceof TIDERace){
			if(this.sig.equals(((TIDERace) o).sig) && compareType((TIDERace) o) && compareSig((TIDERace) o))
				return true;
		}
		return false;
	}
	
	public boolean compareSig(TIDERace race) {
		return this.node1.getSig().equals(((TIDERace) race).node1.getSig()) 
				&& this.node2.getSig().equals(((TIDERace) race).node2.getSig()) 
				|| this.node1.getSig().equals(((TIDERace) race).node2.getSig()) 
				&& this.node2.getSig().equals(((TIDERace) race).node1.getSig());
	}
	
	// check if two races have the same race type
	// i.e. w/w or w/r
	// 0 stands for write
	// 1 stands for read
	// Type 0 is w/w
	// Type 1 is w/r
	public boolean compareType(TIDERace race) {
		int thisType = 0;
		int oType = 0;
		if (this.node1 instanceof ReadNode)
			thisType += 1;
		if (this.node2 instanceof ReadNode)
			thisType += 1;
		
		if (race.node1 instanceof ReadNode)
			oType += 1;
		if (race.node2 instanceof ReadNode)
			oType += 1;
		
		return (thisType == oType);
	}

	public void setBugInfo(String raceMsg, ArrayList<LinkedList<String>> traceMsg2, String fixMsg) {
		this.raceMsg = raceMsg;
		this.fixMsg = fixMsg;
		this.traceMsg = traceMsg2;
	}
}
