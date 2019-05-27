package edu.tamu.aser.tide.cli;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class Main {

	public static void main(String[] args) {
		// -i, --input, input jar
		// -c, --config, config file
		// -o, --output, output dir
		// -s, --sensitive, sensitivity level
		Options options = new Options();
		options.addOption("i", "input", true, "path of the input jar file");
		options.addOption("c", "config", true, "path of the configuration file");
		options.addOption("o", "output", true, "output directory");
		options.addOption("s", "sensitive", true, "pointer analysis sensitivity");
		
		CommandLineParser parser = new DefaultParser();

		try {
			CommandLine cmd = parser.parse(options, args);
			
			if (cmd.hasOption("i")) {
				System.out.println(cmd.getOptionValue("i"));
			}

			if (cmd.hasOption("c")) {
				System.out.println(cmd.getOptionValue("c"));
			}

			if (cmd.hasOption("o")) {
				System.out.println(cmd.getOptionValue("o"));
			}

			if (cmd.hasOption("s")) {
				System.out.println(cmd.getOptionValue("s"));
			}

		} catch (ParseException e) {
			System.out.println(e.getMessage());
		}
		
		
	}

}
