package edu.tamu.aser.tide.tests;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class Data_init
{
	public static void main(String[] args) throws FileNotFoundException, IOException
	{

		File folder = new File(".");

		File[] listOfFiles = folder.listFiles();

		for (File file : listOfFiles) {
		    if (file.isFile()) {
		    	String filename = file.getName();
		    	if(filename.startsWith("log_base_xalan")) //&&!filename.endsWith("-opt")
//		    	if(filename.startsWith("data-"))
		    	{

		//String filename = "data-example";

		long totaldeletetime =0;
		long totaladdtime =0;
		long worstdeletetime =0;
		long worstaddtime =0;
		int totalinstruction=0;
		int totaladdinstructionless100ms=0;
		int totaldeleteinstructionless100ms=0;

		try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
		    String line;
		    while ((line = br.readLine()) != null) {

		       // process the line.
		    	String[] strs = line.split(" ");
		    	//make sure it starts with a number
		    	if(strs.length>0&&!strs[0].isEmpty()
		    			&&strs[0].charAt(0)>='0'&&strs[0].charAt(0)<='9')
		    	{
		    	totalinstruction += strs.length/2;
		    	for(int i=0;i<strs.length-2;i=i+2)
		    	{
		    		int deletetime = Integer.parseInt(strs[i]);
		    		int addtime = Integer.parseInt(strs[i+1]);
		    		totaldeletetime+=deletetime;
		    		totaladdtime+=addtime;
		    		if(deletetime>worstdeletetime)
		    			worstdeletetime = deletetime;

		    		if(deletetime<=100)
		    			totaldeleteinstructionless100ms++;

		    		if(addtime>worstaddtime)
		    			worstaddtime = addtime;
		    		if(addtime<=100)
		    			totaladdinstructionless100ms++;
		    	}
		    	}
		    }
		}

		double averagedeletetime = (double)totaldeletetime/totalinstruction;
		double percentdelete = (double)totaldeleteinstructionless100ms/totalinstruction;
		double percentadd = (double)totaladdinstructionless100ms/totalinstruction;

		double averageaddtime = (double)totaladdtime/totalinstruction;
		double averagetime = (averagedeletetime+averageaddtime)/2;

		System.out.println(filename+":"
				+"\n == Average: "+averagetime+ "(Delete: "+averagedeletetime+ " Add: "+averageaddtime+")"
				+"\n == Worst (Delete: "+worstdeletetime+ " Add: "+worstaddtime+")"
				+"\n == Percent <=100ms (Delete: "+percentdelete+ " Add: "+percentadd+")"
				+"\n == TotalInstruction: " + totalinstruction

				);

		    	}
		    }
		}
	}
}