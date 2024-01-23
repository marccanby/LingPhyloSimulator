package Main;

import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by marccanby on 10/9/20.
 */
public abstract class AbstractCharacter {
    // This class contains stuff that are common to any kind of evolution model (regular, polymorphic, etc.)
    // Override it for a particular evolution model, and you have to implement the 3 abstract functions.

    public AbstractCharacterClass Class;
    public Network Network;
    // public HashMap<Integer, Double> EdgeLengthModifiers;
    public HashMap<Integer, Double> DlcModifiers;
    public HashMap<Integer, Double> HetModifiers;
    public HashMap<Integer, Double> EdgeLengthModifiers; // = height_factor * dlc_mod * het_mod
    public double transmissionSiteFactor; // pi_c
    public int id;

    public AbstractCharacter(AbstractCharacterClass clas, Network network, HashMap<Integer, Double> dlcModifiers, int id) {
        this.Class = clas;
        this.Network = network;
        this.DlcModifiers = dlcModifiers == null ? drawModifiers(Network.RootNode, Class.sigma_dlc) : dlcModifiers;
        this.HetModifiers = drawModifiers(Network.RootNode, Class.sigma_het);
        this.EdgeLengthModifiers = computeLengthModifiers(Network.RootNode);
        this.transmissionSiteFactor = drawTransmissionSiteFactor();
        this.id = id;
    }

    // For either dlc or het
    private double drawModifier(double sigma) {
        double draw_value;
        if (sigma == 0) {
            draw_value = 0.0;
        } else {
            draw_value = Class.randomProvider.nextGaussian(Class.randomProvider.indexMapping.get("rates"));
            draw_value = draw_value * sigma + 0.0; // transform into N(0, sigma_dlc^2)
        }
        draw_value = Math.exp(draw_value - (sigma * sigma)/2);
        return draw_value;
    }

    // For either dlc or het
    private HashMap<Integer, Double> drawModifiers(Vertex vertex, double sigma) {
        HashMap<Integer, Double> hm = new HashMap<Integer, Double>();
        for (Edge e : vertex.OutgoingEdges) {
            hm.put(e.Id, drawModifier(sigma));
            HashMap<Integer, Double> hm_recursive = drawModifiers(e.child, sigma);
            hm.putAll(hm_recursive);
        }
        return hm;
    }

    // Calculates height_factor * dlc_mod * het_mod for each edge
    private HashMap<Integer, Double> computeLengthModifiers(Vertex vertex) {
        HashMap<Integer, Double> hm = new HashMap<Integer, Double>();
        for (Edge e : vertex.OutgoingEdges) {
            hm.put(e.Id, Class.class_height_factor * DlcModifiers.get(e.Id) * HetModifiers.get(e.Id));
            HashMap<Integer, Double> hm_recursive = computeLengthModifiers(e.child);
            hm.putAll(hm_recursive);
        }
        return hm;
    }

    private double drawTransmissionSiteFactor() {
        double draw_value = -1;
        // draw_value = randomProvider.nextGamma(randomProvider.indexMapping.get("topology"));
        draw_value = Class.randomProvider.nextBeta(Class.randomProvider.indexMapping.get("topology"), "site");
        assert draw_value >= 0 && draw_value <= 1;
        return draw_value;
    }


    // This function is common for any type of evolution (original or polymorphic).
    public HashMap<Integer, HashSet<Integer>> evolveCharacter(boolean evolveWithReticulate, boolean noprint) {
        // The plan is to repeatedly evolve genetically until hit nodes with reticulate edge.
        // Then, we resolve the reticulate edge.
        // We repeat until all reticulate edges have been resolved and we have hit the leaves.
        // If evolveWithReticulate == false, then the while loop will exit immediately.

        // Make some global stuff
        final HashMap<Integer, Vertex> nodes = Network.getNodes();
        HashSet<Integer> alreadyHandledRetEdges = new HashSet<>(); // Has the ids of ret edges already handled
        HashMap<Integer, HashSet<Integer>> stateMap = new HashMap<>();

        // First, have to get root state
        Pair<HashSet<Integer>, Integer> rootState = getRootState();
        int next_state = rootState.snd;
        HashSet<Integer> stateSet = rootState.fst;
        stateMap.put(Network.RootNode.Id, stateSet);

        // Do the initial evolution
        Pair<HashMap<Integer, HashSet<Integer>>, Integer> res = evolveCharacterGenetically(Network.RootNode, stateSet, next_state, evolveWithReticulate, noprint);
        stateMap.putAll(res.fst);
        next_state = res.snd;

        // We always enter the loop with stateMap down to reticulate edges and next_state
        while (true) {

            // Find the reticulate edge(s) in the dataset
            PairSet<Integer, Integer> retEdges = new PairSet<>();
            for (int n : stateMap.keySet()) {
                Vertex node_n = nodes.get(n);
                if (evolveWithReticulate &&
                        node_n.reticulateEdge != null &&
                        !alreadyHandledRetEdges.contains(node_n.reticulateEdge.Id) &&
                        stateMap.containsKey(node_n.reticulateEdge.left.Id) && // If only one side is in stateMap, we're not ready to handle it
                        stateMap.containsKey(node_n.reticulateEdge.right.Id)) {
                    retEdges.add(new Pair<>(node_n.reticulateEdge.left.Id, node_n.reticulateEdge.right.Id));
                }
            }

            // If no reticulate edges, we have hit the leaves and we are done
            if (retEdges.size() == 0) {
                break;
            }

            // Then resolve these reticulate edges
            for (Pair<Integer, Integer> edge : retEdges.keySet()) {
                ReticulateEdge retEdge = nodes.get(edge.fst).reticulateEdge;
                handleReticulateEdge(retEdge, stateMap, noprint);
                alreadyHandledRetEdges.add(retEdge.Id);
            }

            // Now continue evolving as normal from the reticulate nodes
            for (Pair<Integer, Integer> edge : retEdges.keySet()) {
                ReticulateEdge retEdge = nodes.get(edge.fst).reticulateEdge;

                // Do left
                Pair<HashMap<Integer, HashSet<Integer>>, Integer> hashMapIntegerPair = evolveCharacterGenetically(retEdge.left, stateMap.get(retEdge.left.Id), next_state, evolveWithReticulate, noprint);
                next_state = hashMapIntegerPair.snd;
                stateMap.putAll(hashMapIntegerPair.fst);

                // Do right
                Pair<HashMap<Integer, HashSet<Integer>>, Integer> hashMapIntegerPair2 = evolveCharacterGenetically(retEdge.right, stateMap.get(retEdge.right.Id), next_state, evolveWithReticulate, noprint);
                next_state = hashMapIntegerPair2.snd;
                stateMap.putAll(hashMapIntegerPair2.fst);
            }
        }

        return stateMap;
    }



    //  Only thing is that the 3 functions below need to be overriden according to the specific evolution model.
    public abstract Pair<HashSet<Integer>, Integer> getRootState();
    public abstract Pair<HashMap<Integer, HashSet<Integer>>, Integer> evolveCharacterGenetically(Vertex node, HashSet<Integer> current_state, int next_state, boolean stopAtReticulate, boolean noprint);
    public abstract void handleReticulateEdge(ReticulateEdge retEdge, HashMap<Integer, HashSet<Integer>> stateMap, boolean noprint);

}
