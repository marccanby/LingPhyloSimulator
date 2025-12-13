import Main.*;
import org.apache.commons.cli.*;

import java.io.*;
import java.lang.String;
import java.util.*;

public class Simulator {

	private static Options buildOptions() {
		Options options = new Options();
		Option gen_network = new Option("gen", "generate-network", false, "Whether or not to generate network");
		options.addOption(gen_network);

		Option simul = new Option("sim", "simulate", false, "Whether or not to simulate down network");
		options.addOption(simul);

		Option noprint = new Option("np", "no-print", false, "Whether or not to print things");
		options.addOption(noprint);

		Option num_edges = new Option("ne", "num-edges", true, "Number of reticulate edges");
		//num_edges.setRequired(true);
		num_edges.setType(Integer.TYPE);
		options.addOption(num_edges);

		Option epsilon = new Option("eps", "epsilon", true, "Epsilon");
		epsilon.setType(Float.TYPE);
		options.addOption(epsilon);

		Option mu_trm = new Option("alphatrm", "alpha-trm-edge", true, "Alpha transmission edge");
		mu_trm.setType(Float.TYPE);
		options.addOption(mu_trm);

		Option sigma_trm = new Option("betatrm", "beta-trm-edge", true, "Beta transmission edge");
		sigma_trm.setType(Float.TYPE);
		options.addOption(sigma_trm);

		Option tree = new Option("tree", "tree", true, "Tree in Newick format");
		//tree.setType(String.TYPE);
		options.addOption(tree);

		Option save_network = new Option("snetfile", "save-network-file", true, "File in which to save generated network");
		//tree.setType(String.TYPE);
		options.addOption(save_network);

		Option net_input_file = new Option("netinputfile", "network-input-file", true, "File from which to read network for simulation");
		//tree.setType(String.TYPE);
		options.addOption(net_input_file);

		Option simparfile = new Option("simparfile", "sim-params-file", true, "CSV file with character class parameters.");
		//tree.setType(String.TYPE);
		options.addOption(simparfile);

		Option simoutfile = new Option("simoutfile", "sim-output-file", true, "CSV file in which to save simulation results.");
		//tree.setType(String.TYPE);
		options.addOption(simoutfile);

		Option simcharclass = new Option("simcharclass", "sim-char-class", true, "Name of characer class to use.");
		//tree.setType(String.TYPE);
		options.addOption(simcharclass);

		Option seed = new Option("seed", "seed", true, "Random seed to use");
		//num_edges.setRequired(true);
		seed.setType(Integer.TYPE);
		options.addOption(seed);

		return options;
	}

	public static void checkOptions(CommandLine cmd) {
		if (Arrays.stream(cmd.getOptions()).anyMatch(x -> ((Option) x).getLongOpt() == "generate-network")) {
			assert Arrays.stream(cmd.getOptions()).anyMatch(x -> ((Option) x).getLongOpt() == "num-edges") : "num-edges must be present if generate-network is";
			assert Arrays.stream(cmd.getOptions()).anyMatch(x -> ((Option) x).getLongOpt() == "epsilon") : "epsilon must be present if generate-network is";
			assert Arrays.stream(cmd.getOptions()).anyMatch(x -> ((Option) x).getLongOpt() == "alpha-trm-edge") : "alpha-trm-edge must be present if generate-network is";
			assert Arrays.stream(cmd.getOptions()).anyMatch(x -> ((Option) x).getLongOpt() == "beta-trm-edge") : "beta-trm-edge must be present if generate-network is";
			assert Arrays.stream(cmd.getOptions()).anyMatch(x -> ((Option) x).getLongOpt() == "tree") : "tree must be present if generate-network is";
			if (!Arrays.stream(cmd.getOptions()).anyMatch(x -> ((Option) x).getLongOpt() == "save-network-file")) {
				assert Arrays.stream(cmd.getOptions()).anyMatch(x -> ((Option) x).getLongOpt() == "simulate") : "simulate must be enabled if save-network-file is not present";
			}
		}
		else {
			assert !Arrays.stream(cmd.getOptions()).anyMatch(x -> ((Option) x).getLongOpt() == "num-edges") : "num-edges must not be present if generate-network is not";
			assert !Arrays.stream(cmd.getOptions()).anyMatch(x -> ((Option) x).getLongOpt() == "epsilon") : "epsilon must not be present if generate-network is not";
			assert !Arrays.stream(cmd.getOptions()).anyMatch(x -> ((Option) x).getLongOpt() == "alpha-trm-edge") : "alpha-trm-edge must not be present if generate-network is not";
			assert !Arrays.stream(cmd.getOptions()).anyMatch(x -> ((Option) x).getLongOpt() == "beta-trm-edge") : "beta-trm-edge must not be present if generate-network is not";
			// assert !Arrays.stream(cmd.getOptions()).anyMatch(x -> ((Option) x).getLongOpt() == "tree") : "tree must not be present if generate-network is not";
			assert !Arrays.stream(cmd.getOptions()).anyMatch(x -> ((Option) x).getLongOpt() == "save-network-file"): "snetfile must not be present if generate-network is not";

		}

		if (Arrays.stream(cmd.getOptions()).anyMatch(x -> ((Option) x).getLongOpt() == "simulate")) {
			if (!Arrays.stream(cmd.getOptions()).anyMatch(x -> ((Option) x).getLongOpt() == "generate-network")) {
				boolean ninpfileprovided = Arrays.stream(cmd.getOptions()).anyMatch(x -> ((Option) x).getLongOpt() == "network-input-file");
				boolean treeprovided = Arrays.stream(cmd.getOptions()).anyMatch(x -> ((Option) x).getLongOpt() == "tree");
				assert !(ninpfileprovided && treeprovided) : "You cannot provide both tree and network-input-file when simulate is selected.";
				assert ninpfileprovided || treeprovided : "tree or network-input-file (but not both) must be provided if network was not generated";
			}
			assert Arrays.stream(cmd.getOptions()).anyMatch(x -> ((Option) x).getLongOpt() == "sim-output-file") : "Must specify CSV file into which to save simulations if simulate enabled";
			assert Arrays.stream(cmd.getOptions()).anyMatch(x -> ((Option) x).getLongOpt() == "sim-params-file") : "Must specify CSV file with character class parameters if simulate enabled";
			assert Arrays.stream(cmd.getOptions()).anyMatch(x -> ((Option) x).getLongOpt() == "sim-char-class") : "Must specify character class if simulate enabled";

		} else {
			assert !Arrays.stream(cmd.getOptions()).anyMatch(x -> ((Option) x).getLongOpt() == "network-input-file") : "network-input-file must not be present if simulate is not";
			assert !Arrays.stream(cmd.getOptions()).anyMatch(x -> ((Option) x).getLongOpt() == "sim-output-file") : "sim-output-file must not be present if simulate is not";
			assert !Arrays.stream(cmd.getOptions()).anyMatch(x -> ((Option) x).getLongOpt() == "sim-params-file") : "sim-params-file must not be present if simulate is not";
			assert !Arrays.stream(cmd.getOptions()).anyMatch(x -> ((Option) x).getLongOpt() == "sim-char-class") : "sim-char-class must not be present if simulate is not";
		}

		assert Arrays.stream(cmd.getOptions()).anyMatch(x -> ((Option) x).getLongOpt() == "seed");


	}

	public static void main (String args[]) {

		// READ COMMAND LINE PARAMS
		Options opts = buildOptions();
		HelpFormatter formatter = new HelpFormatter();
		CommandLineParser parser = new DefaultParser();
		CommandLine cmd;
		try {
			cmd = parser.parse(opts, args);
		} catch (ParseException e) {
			System.out.println(e.getMessage());
			formatter.printHelp("User Profile Info", opts);
			System.exit(1);
			return;
		}

		checkOptions(cmd);

		boolean noprint = Arrays.stream(cmd.getOptions()).anyMatch(x -> ((Option) x).getLongOpt() == "no-print");

		int seedVal = cmd.hasOption("seed")
		    ? Integer.parseInt(cmd.getOptionValue("seed"))
		    : (int) System.currentTimeMillis();

		RandomProvider random_provider = new RandomProvider(seedVal);



		// BUILD NETWORK
		Network net = null;
		if (Arrays.stream(cmd.getOptions()).anyMatch(x -> ((Option) x).getLongOpt() == "generate-network")) {
			double alpha = Double.parseDouble(cmd.getOptionValue("alpha-trm-edge"));
			double beta = Double.parseDouble(cmd.getOptionValue("beta-trm-edge"));
			random_provider.addBetaDistribution("edge", alpha, beta);

			net = new Network(random_provider);
			// Original tree; all the leaves are really far away from junctions though....makes it hard to see how well it works....
			//net.createFromNewick("((t10:0.737253,((t9:0.530645,((t3:0.049233,t8:0.049233):0.339981,t5:0.389213):0.141431):0.027915,t2:0.558560):0.178693):0.262747,((t4:0.849867,(t7:0.000025,t6:0.685825):0.000041):0.000628,t1:0.915495):0.084505)"); // Note: at least this string has height 1 (think it has to be)
			// New tree: More even
			//net.createFromNewick("(((((t2:0.25,t3:0.25):0.25,t1:0.5):0.25,(((t4:0.125,t5:0.125):0.125,t6:0.25):0.25,t17:0.125):0.25):0.125,(t7:0.5,t8:0.5):0.375):0.125,(t16:0.125,((t14:0.125,t15:0.125):0.125,(t9:0.5,(t11:0.25,t12:0.25):0.25):0.125):0.25):0.125)");
			String newick = cmd.getOptionValue("tree");
			net.createFromNewick(newick);
			// Make sure there are no spaces when defining a newick string!
			net.printNetwork();
			if (!noprint) System.out.println("");

			net.addReticulateEdges(Integer.parseInt(cmd.getOptionValue("num-edges")),
					Float.parseFloat(cmd.getOptionValue("epsilon")),
					false, noprint);
			// Note: If want to set all transmission strengths to 0.5, set mu_trm = 0.5 and sigma_trm = 0.0
			net.printNetwork();
			if (!noprint) System.out.println("");
			net.writeNetwork(cmd.getOptionValue("save-network-file", null), noprint);
		}

		if (Arrays.stream(cmd.getOptions()).anyMatch(x -> ((Option) x).getLongOpt() == "simulate")) {
			if (net == null) {
				net = new Network(random_provider);
				if (Arrays.stream(cmd.getOptions()).anyMatch(x -> ((Option) x).getLongOpt() == "network-input-file")) {
					net.readFromFile(cmd.getOptionValue("network-input-file"), false, noprint);
				}
				else {
					String newick = cmd.getOptionValue("tree");
					net.createFromNewick(newick);
				}
				if (!noprint) net.printNetwork();
				if (!noprint) System.out.println("");
				net.writeNetwork(null, noprint);
			}

			// READ SIMUL PARAMS
			LinkedHashMap<String, HashMap<String, String>> params = new LinkedHashMap<>();
			try (BufferedReader br = new BufferedReader(new FileReader(cmd.getOptionValue("sim-params-file")))) {
				String line;
				int line_num = 0;
				String[] colNames = null;
				while ((line = br.readLine()) != null) {
					String[] values = line.split(",");
					if (line_num == 0) {
						colNames = values;
						for (int i = 1; i < values.length; i++) params.put(values[i], new HashMap<>());
					}
					else {
						for (int i = 1; i < values.length; i++) {
							params.get(colNames[i]).put(values[0], values[i]);
						}
					}
					line_num += 1;
					//records.add(Arrays.asList(values));
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				return;
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}

			String charClassToUse = cmd.getOptionValue("sim-char-class");


			// CREATE CHARACTER CLASSES AND SIMULATE
			HashMap<Integer, LinkedHashMap<String, HashSet<Integer>>> finalSequences = new HashMap<>();
			ArrayList<AbstractCharacterClass> charList = new ArrayList<>();
			for (String ctype : params.keySet()) {
				for (int a = 0; a <= 9; a++) assert ctype.indexOf(String.valueOf(a)) == -1; // Assert no numbers in the class name so that can number them with no issue (and look up their weights in the make csv function by shaving off the number)
				AbstractCharacterClass charClass = null;
				if (charClassToUse.equals("CharacterClass")) charClass = new CharacterClass(ctype, params.get(ctype), random_provider, net);
				else if (charClassToUse.equals("PolymorphicCharacterClass")) charClass = new PolymorphicCharacterClass(ctype, params.get(ctype), random_provider, net);
				else assert false;
				charClass.createCharacters();
				charList.add(charClass);

				HashMap<Integer, ArrayList<HashSet<Integer>>> sequences = charClass.evolveCharacters(true, noprint);
				for (int i : sequences.keySet()) {
					if (!finalSequences.containsKey(i)) {
						finalSequences.put(i, new LinkedHashMap<>());
					}
					for (int j = 0; j < sequences.get(i).size(); j++) {
						finalSequences.get(i).put(charClass.PML + String.valueOf(j), sequences.get(i).get(j));
					}
				}
			}

			HashMap<String, Double> weightsByClass = new HashMap<>();
			for (AbstractCharacterClass acc : charList) weightsByClass.put(acc.PML, acc.weight);

			if (!noprint) System.out.println();
			String csv_str = charList.get(0).printSequences(finalSequences, net,weightsByClass, false, noprint);

			BufferedWriter writer = null;
			try {
				writer = new BufferedWriter(new FileWriter(cmd.getOptionValue("sim-output-file")));
				writer.write(csv_str);
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}








	}
}











