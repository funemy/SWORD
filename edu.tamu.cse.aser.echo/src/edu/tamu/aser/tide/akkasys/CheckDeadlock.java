package edu.tamu.aser.tide.akkasys;

import java.util.ArrayList;
import java.util.Set;

import edu.tamu.aser.tide.nodes.DLPair;

public class CheckDeadlock{

	private ArrayList<DLPair> dLLockPairs;
	private int tid;
	private Set<Integer> tids;

	public CheckDeadlock(Integer tid1, Set<Integer> tids, ArrayList<DLPair> dLLockPairs2) {
		// TODO Auto-generated constructor stub
		this.tid = tid1;
		this.dLLockPairs = dLLockPairs2;
		this.tids = tids;
	}

	public ArrayList<DLPair> getPairs(){
		return dLLockPairs;
	}

	public int getTid(){
		return tid;
	}

	public Set<Integer> getTids(){
		return tids;
	}

}
