package Main;

//import com.sun.tools.hat.internal.model.Root;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;


/**
 * Created by marccanby on 7/16/20.
 */
public class Network {

    boolean is_rooted = true;
    Vertex RootNode;
    RandomProvider randomProvider;
    String UnderlyingNewick;

    public Network(RandomProvider randomProvider) {
        this.randomProvider = randomProvider;
    }

    // Primary fxns

    public void createFromNewick(String newick_string) {
        RootNode = new Vertex(1,0);
        UnderlyingNewick = newick_string;
        UnderlyingNewick = UnderlyingNewick.replaceAll("\\s", ""); // Remove white spaces from the string
        if (UnderlyingNewick.charAt(UnderlyingNewick.length() - 1) == ';') UnderlyingNewick = UnderlyingNewick.substring(0, UnderlyingNewick.length() - 1);
        createFromNewick(RootNode, UnderlyingNewick,2);
    }

    public void readFromFile(String network_file, boolean renumber_network, boolean noprint) {
        StringBuilder resultStringBuilder = new StringBuilder();
        try (BufferedReader br
                     = new BufferedReader(new FileReader(network_file))) {
            String line;
            while ((line = br.readLine()) != null) {
                resultStringBuilder.append(line).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        String netRep = resultStringBuilder.toString();
        String[] split = netRep.split(("\n"));
        String newickRep = split[0];
        createFromNewick(newickRep);
        HashMap<Pair<Integer, Integer>, Pair<Double, Double>> pairsToCreate = new HashMap<>();
        for (int i = 1; i < split.length; i++) {
            String curr = split[i];
            String[] curr_split = curr.split(";");
            assert curr_split.length == 4;
            double time = Double.parseDouble(curr_split[2]);
            double strength = Double.parseDouble(curr_split[3]);
            Edge e1 = findEdge(curr_split[0], RootNode);
            Edge e2 = findEdge(curr_split[1], RootNode);
            pairsToCreate.put(new Pair<>(e1.Id, e2.Id), new Pair(time, strength));
        }

        makeReticulateEdges(pairsToCreate, renumber_network, noprint);
    }

    public void addReticulateEdges(int number, double epsilon, boolean renumber_network, boolean noprint) {
        // Note: Fairly well tested under normal circumstances, but not under many edge cases (multiple splits per edge, high number of contact edges, etc)

        // First get edges, nodes, and lcas
        HashMap<Integer, Edge> edgeMap = getEdges();
        HashMap<Integer, Vertex> nodeMap = getNodes();
        ArrayList<Edge> edges = new ArrayList<>(edgeMap.values());
        HashMap<Integer, HashMap<Integer, Integer>> lcas = findLeastCommonAncestors();

        // Now find edge pairs, along with lca and overlap: (edge1, edge2) -> (lca, (t_1, t_2))
        PairMap<Integer, Integer, Pair<Integer, Pair<Double, Double>>> edgeEdgeOverlapMap = new PairMap<>();

        for (int i = 0; i < edges.size(); i++) {
            Edge edge_i = edges.get(i);
            double start_i = edge_i.parent.Time;
            double end_i = edge_i.child.Time;
            for (int j = i+1; j < edges.size(); j++) {
                Edge edge_j = edges.get(j);
                double start_j = edge_j.parent.Time;
                double end_j = edge_j.child.Time;
                Pair<Double, Double> distance_pair;
                if (end_i <= start_j || end_j <= start_i) { // not contemporary; case 1 and 2
                    continue;
                } else if (start_i >= start_j && end_i <= end_j) { // case 3
                    distance_pair = new Pair<>(start_i, end_i);
                } else if (start_j >= start_i && end_j <= end_i) { // case 4
                    distance_pair = new Pair<>(start_j, end_j);
                } else if (start_i >= start_j) { // case 5
                    distance_pair = new Pair<>(start_i, end_j);
                } else { // start_j >= start_i, case 6
                    distance_pair = new Pair<>(start_j, end_i);
                }
                if (edge_i.Id == 0) {
                    int x = 3;
                }
                Integer lca = lcas.get(edge_i.parent.Id).get(edge_j.parent.Id);

                // Question - should amount of overlap also have to pass epsilon threshold? document says so but i don't see it in their code.
                // Probably doesn't matter too much because score is tiny

                if (distance_pair.fst - nodeMap.get(lca).Time > epsilon) { // needs to pass some threshold to appear, otherwise get infinite scores
                    Pair<Integer, Integer> edge_pair = edge_i.Id < edge_j.Id ? new Pair<>(edge_i.Id, edge_j.Id) : new Pair<>(edge_j.Id, edge_i.Id) ;
                    Pair<Integer, Pair<Double, Double>> inner_pair = new Pair<>(lca, distance_pair);
                    edgeEdgeOverlapMap.put(edge_pair, inner_pair);
                }

            }
        }

        // Now score each pair
        PairMap<Integer, Integer, Double> scores = new PairMap<Integer, Integer, Double>();
        for (Pair<Integer, Integer> pair : edgeEdgeOverlapMap.keySet()) {
            double t_0 = nodeMap.get(edgeEdgeOverlapMap.get(pair).fst).Time;
            double t_1 = edgeEdgeOverlapMap.get(pair).snd.fst;
            double t_2 = edgeEdgeOverlapMap.get(pair).snd.snd;
            assert t_2 > t_1 && t_0 <= t_1 && t_0 < t_2;

            // Motivation: when edge length is small (t1 ~= t2), score should be small
            // when close to t0, score should be large
            double score = -Math.log((t_1 - t_0) / (t_2 - t_0));
            scores.put(pair, score);
        }

        // Now repeatedly draw pairs where to add a contact edge; edge -> (contact_time, strenght)
        HashMap<Pair<Integer, Integer>, Pair<Double, Double>> pairsToCreate = new HashMap<>();
        for (int i = 0; i < number; i++) {

            // Get sum
            double score_sum = 0;
            ArrayList<Pair<Integer, Integer>> scoreKeys = new ArrayList<>(scores.keySet());
            for (int j = 0; j < scoreKeys.size(); j++) {
                score_sum += scores.get(scoreKeys.get(j));
            }

            // Draw number
            double draw = randomProvider.nextDouble(randomProvider.indexMapping.get("topology"));
            draw = draw * score_sum;

            // Figure out interval
            Pair<Integer, Integer> selected_pair = null;
            for (int j = 0; j < scoreKeys.size(); j++) {
                double score = scores.get(scoreKeys.get(j));
                draw -= score;
                if (draw <= 0) {
                    selected_pair = scoreKeys.get(j);
                    break;
                }
            }

            // Figure out contact time
            double t_0 = nodeMap.get(edgeEdgeOverlapMap.get(selected_pair).fst).Time;
            double t_1 = edgeEdgeOverlapMap.get(selected_pair).snd.fst;
            double t_2 = edgeEdgeOverlapMap.get(selected_pair).snd.snd;
            assert t_2 > t_1 && t_0 <= t_1 && t_0 < t_2;

            // Note: This fxn feels way too extreme on biasing the contact time to be close to t_1. even draw = 0.5 makes it so close.
            double draw2 = randomProvider.nextDouble(randomProvider.indexMapping.get("topology"));
            double contact_time = t_0 + (t_1 - t_0) * Math.exp(draw2 * Math.log((t_2-t_0)/(t_1-t_0)));

            double strength = ReticulateEdge.drawTransmissionStrength(randomProvider);

            // Store edge and remove from scores.
            pairsToCreate.put(selected_pair, new Pair<>(contact_time, strength));
            scores.remove(selected_pair);
        }

        makeReticulateEdges(pairsToCreate, renumber_network, noprint);


    }

    private void makeReticulateEdges(HashMap<Pair<Integer, Integer>, Pair<Double, Double>> pairsToCreate,
                                     boolean renumber_network, boolean noprint) {

        HashMap<Integer, Edge> edgeMap = getEdges();
        HashMap<Integer, Vertex> nodeMap = getNodes();

        // Print
        if (!noprint) System.out.println("Contact edges to be created:");
        int cnt = 1;
        for (Pair<Integer, Integer> pair : pairsToCreate.keySet()) {
            if (!noprint) System.out.println("Contact Edge " + String.valueOf(cnt) + ": " + String.valueOf(pair.fst) + " <-----> " +
                    String.valueOf(pair.snd) + " at time " + String.valueOf(pairsToCreate.get(pair).fst));
            cnt++;
        }
        if (!noprint) System.out.println("");

        // Now create the edges. Do it very carefully, because when creating an edge, new edge ids are made,
        // and the original mapping has original edge numbers. Do it in steps - basically, first splinter the edges,
        // keeping track of how the new edge ids relate to the old ones, then draw the contact edges.

        // First, make a mapping of edge -> list (ordered) of contact times along it
        HashMap<Integer, HashSet<Double>> splitPointsSet = new HashMap<>(); // start with set, then order it
        for (Pair<Integer, Integer> pair : pairsToCreate.keySet()) {
            int edge1 = pair.fst;
            int edge2 = pair.snd;
            double time = pairsToCreate.get(pair).fst;

            if (!splitPointsSet.containsKey(edge1)) splitPointsSet.put(edge1, new HashSet<>());
            splitPointsSet.get(edge1).add(time);

            if (!splitPointsSet.containsKey(edge2)) splitPointsSet.put(edge2, new HashSet<>());
            splitPointsSet.get(edge2).add(time);
        }

        HashMap<Integer, ArrayList<Double>> splitPoints = new HashMap<>();
        for (int e : splitPointsSet.keySet()) {
            ArrayList<Double> times = new ArrayList<>(splitPointsSet.get(e));
            Collections.sort(times);
            splitPoints.put(e, times);
        }

        // Now, splinter each edge at the times, recording (old edge id, time) -> new edge whose terminal node is at time
        // New edge ids are edge ids not in the original graph (i.e. higher than max id)
        PairMap<Integer, Double, Edge> oldNewEdgeMapping = new PairMap<Integer, Double, Edge>();
        int nextVertexId = Collections.max(nodeMap.keySet()) + 1;
        int nextEdgeId = Collections.max(edgeMap.keySet()) + 1;
        for (int e : splitPoints.keySet()) {
            PairMap<Integer, Double, Edge> oldNew_e = splinterEdge(edgeMap.get(e), splitPoints.get(e), nextVertexId, nextEdgeId);
            nextVertexId += splitPoints.get(e).size();
            nextEdgeId += splitPoints.get(e).size() + 1;
            oldNewEdgeMapping.putAll(oldNew_e);
        }

        // Now add the reticulate edges into the graph
        cnt = 1;
        for (Pair<Integer, Integer> pair : pairsToCreate.keySet()) {
            double time = pairsToCreate.get(pair).fst;
            double strength = pairsToCreate.get(pair).snd;
            Edge edge1 = oldNewEdgeMapping.get(new Pair<>(pair.fst, time));
            Edge edge2 = oldNewEdgeMapping.get(new Pair<>(pair.snd, time));

            // Add reticulate edge between edge1 and edge2
            ReticulateEdge re = new ReticulateEdge(cnt, edge1.child, edge2.child);
            re.transmission_strength = strength;
            edge1.child.reticulateEdge = re;
            edge2.child.reticulateEdge = re;
            cnt += 1;
        }

        // Finally, renumber entire network
        if (renumber_network) reNumberNetwork(RootNode, 1, 1, 1);
    }

    // Helper fxns for the primary fxns here

    public void writeNetwork(String save_file, boolean noprint) {
        String ret = UnderlyingNewick;
        HashSet<ReticulateEdge> retEdges = findRetEdges(RootNode);
        for (int cnt = 1; cnt <= retEdges.size(); cnt++) { // Iterate over them in order they were created
            ReticulateEdge edge = (ReticulateEdge) retEdges.toArray()[0];
            for (ReticulateEdge e : retEdges) if (e.Id == cnt) edge = e;
            ArrayList<Edge> lout = edge.left.OutgoingEdges;
            ArrayList<Edge> rout = edge.right.OutgoingEdges;
            assert lout.size() == 1 && rout.size() == 1;
            assert edge.left.Time == edge.right.Time;
            String str = lout.get(0).NewickRepresentation + ";" + rout.get(0).NewickRepresentation + ";" + edge.left.Time + ";" + edge.transmission_strength;
            ret += ("\n" + str);
        }
        if (!noprint) System.out.println(ret);
        if (save_file != null) {
            BufferedWriter writer = null;
            try {
                writer = new BufferedWriter(new FileWriter(save_file));
                writer.write(ret);
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private int createFromNewick(Vertex node, String newick_string, int id) {

        // First partition the string based on commas at the outermost level
        String splits[]= newick_string.substring(1, newick_string.length() - 1 ).split(",");
        ArrayList<String> partitions = new ArrayList<>();

        int open_counter = 0;
        String partial_string = "";
        for (int i = 0; i < splits.length; i++) {
            if (open_counter == 0  && splits[i].charAt(0) != '(') {
                partitions.add(splits[i]);
            } else {
                for (int j = 0; j < splits[i].length(); j++) {
                    if (splits[i].charAt(j) == '(') open_counter++;
                    if (splits[i].charAt(j) == ')') open_counter--;
                }
                if (partial_string.length() > 0) partial_string += ",";
                partial_string += splits[i];
                if (open_counter == 0) {
                    partitions.add(partial_string);
                    partial_string = "";
                }
            }
            int q=2;
        }

        assert open_counter == 0;

        // Handle each partition - either recursively call, or add as a leaf
        for (int i = 0; i < partitions.size(); i++) {
            String lens[] = partitions.get(i).split(":");
            double length = Double.parseDouble(lens[lens.length - 1]);
            String new_string = partitions.get(i).substring(0, partitions.get(i).length() - lens[lens.length - 1].length() - 1);
            if (partitions.get(i).charAt(0) != '(') {
                Edge edge = new Edge(id - 1, node, new Vertex(id, node.Time + length, new_string), length); // id - 2 so starts at 1
                edge.NewickRepresentation = new_string;
                id = id + 1;
                edge.child.IncomingEdge = edge;
                node.OutgoingEdges.add(edge);
            }
            else {
                Edge edge = new Edge(id - 1, node, new Vertex(id, node.Time + length), length); // id - 2 so starts at 1
                id = id + 1;
                edge.NewickRepresentation = new_string;
                edge.child.IncomingEdge = edge;
                id = createFromNewick(edge.child, new_string, id);
                node.OutgoingEdges.add(edge);
            }


        }

        // Return next id
        return id;


    }

    private PairMap<Integer, Double, Edge> splinterEdge(Edge e, ArrayList<Double> times, Integer nextVertexId, Integer nextEdgeId) {

        // Global variables
        Vertex parent = e.parent;
        Vertex child = e.child;
        Integer id = e.Id;
        String newickRep = e.NewickRepresentation; // All splintered edges will get this Newick string, since it corresponds to the same clade
        PairMap<Integer, Double, Edge> ret = new PairMap<Integer, Double, Edge>();

        // Delete outgoing edge in parent
        parent.OutgoingEdges.remove(e);

        // Init loop variables
        Vertex prevParent = parent;

        for (int i = 0; i < times.size(); i++) {
            Vertex newChild = new Vertex(nextVertexId, times.get(i));
            Edge newEdge = new Edge(nextEdgeId, prevParent, newChild, times.get(i) - prevParent.Time);
            newEdge.NewickRepresentation = newickRep;
            newChild.IncomingEdge = newEdge;
            prevParent.OutgoingEdges.add(newEdge);
            ret.put(new Pair<>(id, times.get(i)), newEdge);
            nextVertexId++;
            nextEdgeId++;
            prevParent = newChild;
        }

        // Make final edge
        Edge newEdge = new Edge(nextEdgeId, prevParent, child, child.Time - prevParent.Time);
        newEdge.NewickRepresentation = newickRep;
        child.IncomingEdge = newEdge;
        prevParent.OutgoingEdges.add(newEdge);

        return ret;


    }

    private ArrayList<Integer> reNumberNetwork(Vertex node, int nodeId, int edgeId, int retEdgeId) {
        node.Id = nodeId;
        nodeId = nodeId +1;
        if (node.reticulateEdge != null && node.Id == Math.min(node.reticulateEdge.left.Id, node.reticulateEdge.right.Id)) {
            node.reticulateEdge.Id = retEdgeId;
            retEdgeId += 1;
        }
        for (Edge e : node.OutgoingEdges) {
            e.Id = edgeId;
            ArrayList<Integer> rec = reNumberNetwork(e.child, nodeId, edgeId+1, retEdgeId);
            nodeId = rec.get(0);
            edgeId = rec.get(1);
            retEdgeId = rec.get(2);
        }

        ArrayList<Integer> ret = new ArrayList<Integer>();
        ret.add(nodeId);
        ret.add(edgeId);
        ret.add(retEdgeId);
        return ret;
    }

    // Bunch of utility functions down here

    public HashMap<Integer, Vertex> getNodes() {
        return getNodes(RootNode);
    }

    public HashMap<Integer, Edge> getEdges() {
        return getEdges(RootNode);
    }

    public HashSet<Integer> getLeaves() {
        return getLeaves(RootNode);
    }

    public HashMap<Integer, HashMap<Integer, Integer>> findLeastCommonAncestors() {
        return findLeastCommonAncestors(RootNode);
    }


    private HashMap<Integer, Vertex> getNodes(Vertex node) {
        HashMap<Integer, Vertex> hs = new HashMap<>();
        hs.put(node.Id, node);
        for (Edge e: node.OutgoingEdges) {
            HashMap<Integer, Vertex> recursive_hs = getNodes(e.child);
            hs.putAll(recursive_hs);
        }
        return hs;
    }

    private HashMap<Integer, Edge> getEdges(Vertex node) {
        HashMap<Integer, Edge> hs = new HashMap<>();
        for (Edge e: node.OutgoingEdges) {
            hs.put(e.Id, e);
            HashMap<Integer, Edge> recursive_hs = getEdges(e.child);
            hs.putAll(recursive_hs);
        }
        return hs;
    }

    private HashSet<Integer> getLeaves(Vertex node) {
        HashSet<Integer> leaves = new HashSet<>();

        if (node.OutgoingEdges.size() == 0) leaves.add(node.Id);

        for (Edge e : node.OutgoingEdges) {
            leaves.addAll(getLeaves(e.child));
        }

        return leaves;
    }

    private HashMap<Integer, HashMap<Integer, Integer>> findLeastCommonAncestors(Vertex node) {
        HashMap<Integer, HashMap<Integer, Integer>> hm = new HashMap<>();

        // First map node to itself
        HashMap<Integer, Integer> inner_hm = new HashMap<>();
        inner_hm.put(node.Id, node.Id);
        hm.put(node.Id, inner_hm);

        // Now compute on each child, storing results in list
        ArrayList<HashMap<Integer, HashMap<Integer, Integer>>> list = new ArrayList<>();
        for (Edge e : node.OutgoingEdges) {
            HashMap<Integer, HashMap<Integer, Integer>> hm_recursive = findLeastCommonAncestors(e.child);
            list.add(hm_recursive);
            hm.putAll(hm_recursive);

            // Map current node to each element of child
            for (int i : hm_recursive.keySet()) {
                HashMap<Integer, Integer> inner_hm2 = new HashMap<>();
                hm.get(node.Id).put(i, node.Id);
                hm.get(i).put(node.Id, node.Id);
            }
        }

        // Now map nodes within each child to nodes within each other child
        for (int i = 0; i < list.size(); i++) {
            HashMap<Integer, HashMap<Integer, Integer>> hm_i = list.get(i);
            for (int j = i+1; j < list.size(); j++) {
                HashMap<Integer, HashMap<Integer, Integer>> hm_j = list.get(j);
                for (int k : hm_i.keySet()) {
                    for (int l : hm_j.keySet()) {
                        hm.get(k).put(l, node.Id);
                        hm.get(l).put(k, node.Id);
                    }
                }
            }
        }

        return hm;
    }

    private Edge findEdge(String newick_rep, Vertex node) {
        for (Edge e : node.OutgoingEdges) {
            if (e.NewickRepresentation.equals(newick_rep)) return (e);
            else {
                Edge e_sol = findEdge(newick_rep, e.child);
                if (e_sol != null) return (e_sol);
            }
        }
        return null;
    }



    // Printing fxns

    public void printNetwork() {
        System.out.println("----------------------------------");
        printVertexes();
        System.out.println();
        printEdges();
        System.out.println("----------------------------------");
    }

    public void printVertexes() {
        System.out.println("Vertex List:");
        printVertexes(RootNode);
    }

    public void printEdges() {
        System.out.println("Edge List:");
        printEdges(RootNode);
    }


    private void printVertexes(Vertex node) {
        node.printVertex();
        for (Edge e : node.OutgoingEdges) {
            printVertexes(e.child);
        }
    }

    private void printEdges(Vertex node) {
        for (Edge e: node.OutgoingEdges) {
            e.printEdge();
            printEdges(e.child);
        }
        if (node.reticulateEdge != null && node.Id == Math.min(node.reticulateEdge.left.Id, node.reticulateEdge.right.Id))
            node.reticulateEdge.printEdge(); // 2nd condition just to prevent it from printing twice
    }

    private HashSet<ReticulateEdge> findRetEdges(Vertex node) {
        HashSet<ReticulateEdge> ret = new HashSet<>();
        for (Edge e : node.OutgoingEdges) {
            ret.addAll(findRetEdges(e.child));
        }
        if (node.reticulateEdge != null && node.Id == Math.min(node.reticulateEdge.left.Id, node.reticulateEdge.right.Id)) {
            ret.add(node.reticulateEdge);
        }
        return ret;

    }


}
