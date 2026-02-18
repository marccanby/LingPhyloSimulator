# LingPhyloSimulator

This is the code belonging to [Addressing Polymorphism in Linguistic Phylogenetics](https://onlinelibrary.wiley.com/doi/10.1111/1467-968X.12289) (Canby et al., 2024). The paper presents a model for linguistic polymorphism (primarily focused on lexical polymorphism); this repo contains the code to simulate character evolution down trees and phylogenetic networks under this model (and can be easily extended to simulate under other models). It is based loosely on the simulator used in Barbançon et al., 2013. Furthermore, the repo includes commands that can be used to infer the underlying evolutionary trees from the data, as well as simulated trees, networks, and datasets. Finally, the repo contains a new Indo-European dataset, extended and corrected from the dataset presented in Ringe et al., 2002.

There are two steps to the simulator: <b>network generation</b> and <b>character simulation</b>. If you just want to simulate down a tree, you may skip the network generation step. For an algorithmic description of the simulator, see <code>algorithmic_description.pdf</code>.

<b>Tree Generation</b><br>
To generate trees, we use an R script `tree_generation.R`. You can set the number of taxa and other parameters at the top. This outputs a Nexus file with simulated trees of height 1.0.

<b>Network Generation</b><br>
To generate a network, you must provide an underlying tree. These are the network generation command line parameters:

* <code>--generate-network</code>: Use this command if you want to generate a network.
* <code>--tree</code>: Provide a Newick string representation of the underlying tree. It must be anotated with edge lengths, and the age of the tree should be 1.0 (i.e. the length of the longest path from root to leaf should be 1.0). The tree does not need to be ultrametric (that is, all leaves do not need to be at the same time). See the example below for the correct formatting of the Newick string.
* <code>--num-edges</code>: The number of reticulate edges to add.
* <code>--epsilon</code>: The minimum amount of time between the least common ancestor and a pair of edges for which we consider drawing a reticulate edge between them.
* <code>--alpha-trm-edge</code>: The first shape parameter of the Beta distribution from which the edge transmission factor is drawn.
* <code>--beta-trm-edge</code>: The second shape parameter of the Beta distribution from which the edge transmission factor is drawn.
* <code>--save-network-file</code>: The file in which to save the network (can be a .txt file). Alternatively, you can omit this and proceed straight to simulation.

For example, if you want to create a network with 4 reticulate edges and save the result in <code>network.txt</code>, you could use the parameters:

<code>--generate-network --num-edges 4 --epsilon 0.00001 --alpha-trm-edge 10 --beta-trm-edge 20 --tree "(((((t2:0.25,t3:0.25):0.25,t1:0.5):0.25,(((t4:0.125,t5:0.125):0.125,t6:0.25):0.25,t17:0.125):0.25):0.125,(t7:0.5,t8:0.5):0.375):0.125,(t16:0.125,((t14:0.125,t15:0.125):0.125,(t9:0.5,(t11:0.25,t12:0.25):0.25):0.125):0.25):0.125)" --save-network-file network.txt</code>
<br><br>
<b>Simulation</b><br>
To simulate character data, you must provide either a tree or network. You must also provide a CSV file that contains your character class settings, which correspond to parameters in the model of evolution you wish to use. These are the simulation command line parameters:

* <code>--simulate</code>: Use this command if you want to simulate character data.
* <code>--tree</code> or <code>--network-input-file</code>: You must provide one of these two parameters. If you wish to simulate down a tree, provide <code>--tree</code> in the same format as above. If you wish to simulate down a network, provide <code>--network-input-file</code> with the file containing the network saved from the previous step.
* <code>--sim-params-file</code>: A CSV containing parameters for the Character Class of the evolution model you are using to generate data.
* <code>--sim-output-file</code>: A CSV file in which to save the simulated sequences.
* <code>--sim-char-class</code>: The name of the Character Class corresponding to the evolution model you want to use. Currently must be <code>CharacterClass</code> (in the case of the Warnow et al. 2006 model) or <code>PolymorphicCharacterClass</code> (in the case of the polymorphism model of Canby et al., 2024). See below for adding a new type of Character Class.

For example, if you want to simulate down a tree under the polymorphic model with parameters specified in <code>config.csv</code> and save the result in <code>sequences.csv</code>, you could use the parameters:

<code>--simulate --tree "(((((t2:0.25,t3:0.25):0.25,t1:0.5):0.25,(((t4:0.125,t5:0.125):0.125,t6:0.25):0.25,t17:0.125):0.25):0.125,(t7:0.5,t8:0.5):0.375):0.125,(t16:0.125,((t14:0.125,t15:0.125):0.125,(t9:0.5,(t11:0.25,t12:0.25):0.25):0.125):0.25):0.125)" --sim-output-file sequences.csv --sim-params-file config.csv --sim-char-class PolymorphicCharacterClass</code>

Example configuration files are provided in the <code>example/configs/</code>. The resulting simulated data can then be used as input to a variety of tree (or network) estimation methods. We provide commands for software we used in <code>software_commands.pdf</code>.
<br><br>
<b>How to Add a New Model of Evolution</b><br>
So far, only two evolution models (i.e. Character Class) are implemented: <code>CharacterClass</code> (for the Warnow et al. 2006 model) and <code>PolymorphicCharacterClass</code> (for the polymorphism model of Canby et al., 2024). To add a new model of evolution, a similar format must be followed. First, you must add files <code>NewCharacter.java</code> (inheriting <code>AbstractCharacter</code>) and <code>NewCharacterClass.java</code> (inheriting <code>AbstractCharacterClass</code>).

The <code>NewCharacterClass</code> provides a template for parameters specific to the model (e.g. relevant substitution probabilities or birth rates) and implements a function <code>generateCharacter(...)</code> that instantiates a character of this class. The <code>NewCharacter</code> class is used to represent individual instances of the character. In this case, one must implement 3 functions:
* <code>getRootState()</code>: Returns the state (or set of states) that begin the evolution at the root of the tree.
* <code>evolveCharacterGenetically(...)</code>: This returns the state (or set of states) of the character at each child of a particular node.
* <code>handleReticulateEdge(...)</code>: This resolves a reticulate edge in the case of borrowing on networks.

Finally, the <code>main(...)</code> method in <code>Simulator.java</code> must be updated to reflect the addition of a new character class, that way it may be referenced on the command line via <code>--sim-char-class</code> (see above).
<br><br>
<b>Example files</b><br>
Example configurations, trees, networks, and simulated data under these trees and networks are provided in the <code>example</code> folder. This is organized as follows:
* <code>trees.txt</code>: 32 trees (with 30 leaves each) simulated under a Yule model with exponential speciation and extinction waiting times.
* <code>networks/</code>: Networks corresponding to each of the 32 trees, for 1, 2, and 3 borrowing edges.
* <code>configs/</code>: Contains CSV configuration files provided to the Simulator for 5 different levels of polymorphism.
* <code>simulated_data/</code>: Simulated data on each of the trees and networks under 5 different levels of polymorphism.
* <code>ie_dataset.csv</code>: An extended and corrected version of the dataset of Ringe et al. 2002 with all polymorphisms included (previous versions had either removed polymorphic characters or reduced some of them to groups of monomorphic characters via the "split coding" technique). Descriptions of the original dataset can be found at https://tandy.cs.illinois.edu/histling.html.

<br>
<b>References</b>

F. Barbançon, S. N. Evans, L. Nakhleh, D. Ringe, and T. Warnow (2013), An experimental study comparing linguistic phylogenetic reconstruction methods. Diachronica, 30(2):143–170.

Canby, M.E., Evans, S.N., Ringe, D. and Warnow, T. (2024), [Addressing Polymorphism in Linguistic Phylogenetics](https://onlinelibrary.wiley.com/doi/10.1111/1467-968X.12289). Trans Philologic Soc.

D. Ringe, T. Warnow, and A. Taylor (2002), Indo-European and computational cladistics. Transactions of the Philological Society, 100(1):59–129.

T. Warnow, S. N. Evans, D. , and L. Nakhleh (2006) A stochastic model of language evolution that incorporates homoplasy and borrowing. In P. Forster and C. Renfrew, editors, Phylogenetic Methods and the Prehistory of Languages, pages 75–90. Cambridge: McDonald Institute for Archaeological Research.
