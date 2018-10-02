package edu.tamu.aser.tide.akkasys;

import java.util.HashSet;

import edu.tamu.aser.tide.nodes.ReadNode;
import edu.tamu.aser.tide.nodes.WriteNode;


public class CheckDatarace{

	HashSet<WriteNode> writes;
	HashSet<ReadNode> reads;
	String sig;

	public CheckDatarace(String sig, HashSet<WriteNode> writes2, HashSet<ReadNode> reads2) {
		this.sig = sig;
		this.reads = reads2;
		this.writes = writes2;
	}

	public HashSet<WriteNode> getWrites(){
		return writes;
	}

	public HashSet<ReadNode> getReads(){
		return reads;
	}

	public String getSig(){
		return sig;
	}

}
