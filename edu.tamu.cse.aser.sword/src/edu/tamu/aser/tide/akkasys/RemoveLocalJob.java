package edu.tamu.aser.tide.akkasys;

import java.util.ArrayList;

import edu.tamu.aser.tide.shb.Trace;

public class RemoveLocalJob {

	ArrayList<Trace> node;

	public RemoveLocalJob(ArrayList<Trace> team1) {
		this.node = team1;
	}

	public ArrayList<Trace> getTeam(){
		return node;
	}

}
