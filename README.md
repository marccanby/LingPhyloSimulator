# LingPhyloSimulator

There are two steps to the simulator: <b>network generation</b> and <b>character simulation</b>. If you just want to simulate down a tree, you may skip the network generation step.

<b>Network Generation</b><br>
To generate a network, you must provide an underlying tree. These are the network generation command line parameters:

* <code>--generate-network</code>: Use this command if you want to generate a network.
* <code>--tree</code>: Provide a Newick string representation of the underlying tree. It must be anotated with edge lengths, and the age of the tree should be 1.0 (i.e. the length of the longest path from root to leaf should be 1.0). The tree does not need to be ultrametric (that is, all leaves do not need to be at the same time). See the example below for the correct formatting of the Newick string.
* <code>--num-edges</code>: The number of reticulate edges to add.
* <code>--epsilon</code>: The minimum amount of time between the least common ancestor and a pair of edges for which we consider drawing a reticulate edge between them.
* <code>--mu-transmission</code>: The mean of the Gamma distribution from the edge transmission factor is drawn.
* <code>--sigma-transmission</code>: The standard deviation of the Gamma distribution from the edge transmission factor is drawn.
* <code>--save-network-file</code>: The file in which to save the network (can be a .txt file). Alternatively, you can leave this out and proceed straight to simulation.

For example, if you want to create a network with 4 reticulate edges and save the result in <code>network.txt</code>, you could use the parameters:

<code>
--generate-network --num-edges 4 --epsilon 0.00001 --mu-transmission 0.5 --sigma-transmission 0.2 --tree "(((((t2:0.25,t3:0.25):0.25,t1:0.5):0.25,(((t4:0.125,t5:0.125):0.125,t6:0.25):0.25,t17:0.125):0.25):0.125,(t7:0.5,t8:0.5):0.375):0.125,(t16:0.125,((t14:0.125,t15:0.125):0.125,(t9:0.5,(t11:0.25,t12:0.25):0.25):0.125):0.25):0.125)" --save-network-file network.txt
</code>
<br><br>
<b>Simulation</b><br>
To simulate character data, you must provide either a tree or network. You must also provide a CSV file that contains your character class settings, which correspond to parameters in the model of evolution you wish to use. These are the simulation command line parameters:


* <code>--simulate</code>: Use this command if you want to simulate character data.
* <code>--tree</code> or <code>--network-input-file</code>: You must provide one of these two parameters. If you wish to simulate down a tree, provide <code>--tree</code> in the same format as above. If you wish to simulate down a network, provide <code>--network-input-file</code> with the file containing the network saved from the previous step.
* <code>--sim-params-file</code>: A CSV containing parameters for the Character Class of the evolution model you are using to generate data.
* <code>--sim-output-file</code>: A CSV file in which you want to save the simulated sequences.
