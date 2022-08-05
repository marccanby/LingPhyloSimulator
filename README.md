# LingPhyloSimulator

There are two steps to the simulator: <b>network generation</b> and <b>character simulation</b>. If you just want to simulate down a tree, you may skip the network generation step.

To generate a network, you must provide an underlying tree. These are the network generation parameters:
* <code>--generate-network</code>: Use this command if you want to generate a network.
* <code>--tree</code>: Provide a Newick string representation of the underlying tree. It must be anotated with edge lengths, and the age of the tree should be 1.0 (i.e. the length of the longest path from root to leaf should be 1.0). The tree does not need to be ultrametric (that is, all leaves do not need to be at the same time). See the example below for the correct formatting of the Newick string.
* <code>--num-edges</code>: The number of reticulate edges to add.
* <code>--mu-transmission</code>: The mean of the Gamma distribution from the edge transmission factor is drawn.
* <code>--sigma-transmission</code>: The standard deviation of the Gamma distribution from the edge transmission factor is drawn.
* <code>--save-network-file</code>: The file in which to save the network (can be a .txt file). Alternatively, you can leave this out and proceed straight to simulation.

To simulate character data,...
