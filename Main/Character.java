package Main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by marccanby on 7/17/20.
 */
public class Character extends AbstractCharacter {

    CharacterClass clas; // casting it as CharacterClass for usage in this file

    public Character(CharacterClass clas, Network network, HashMap<Integer, Double> dlcModifiers, int id) {
        super(clas, network, dlcModifiers, id);
        this.clas = clas;
    }

    public Pair<HashSet<Integer>, Integer> getRootState() {
        double rando = clas.randomProvider.nextDouble(clas.randomProvider.indexMapping.get("evolution"));
        int state;
        if (rando < clas.h_root) {
            state = 0;
        } else {
            state = 1;
        }
        int next_state = state + 1;
        HashSet<Integer> hs = new HashSet<>();
        hs.add(state);
        return new Pair<>(hs, next_state);
    }

    public Pair<HashMap<Integer, HashSet<Integer>>, Integer> evolveCharacterGenetically(Vertex node, HashSet<Integer> current_state, int next_state, boolean stopAtReticulate, boolean noprint) {

        // Since this fxn is just for regular evolution (no polymorphism), can assert len is 1 and grab it
        assert current_state.size() == 1;
        int cstate = (new ArrayList<Integer>(current_state)).get (0);

        // Grab rates matrix and initialize return hashmap
        HashMap<String, Double> ir = clas.getInfinitesimalRates();
        HashMap<Integer, HashSet<Integer>> ret = new HashMap<>();

        for (Edge e : node.OutgoingEdges) {

            // Init new_state/new_value, and draw a random number
            int new_state = -1;
            double dice = clas.randomProvider.nextDouble(clas.randomProvider.indexMapping.get("evolution"));

            // Create common variables
            double main_exp = -EdgeLengthModifiers.get(e.Id) * e.length*(ir.get("qhn") + ir.get("qnh"));
            double main_den = ir.get("qhn") + ir.get("qnh");

            // Evolve based on the current state
            if (cstate == 0) { // homoplastic
                double phh = (ir.get("qnh") + ir.get("qhn") * Math.exp(main_exp)) / main_den;
                double phn = (ir.get("qhn") * (1-Math.exp(main_exp))) / main_den;
                assert 1.0 - Math.pow(10,-6) <  phh + phn && phh + phn < 1.0 + Math.pow(10,-6);

                if (dice < phh) {
                    new_state = 0;
                } else {
                    new_state = next_state;
                    next_state = next_state + 1;
                }

            } else { // non-homoplsatic
                double sec_exp = -EdgeLengthModifiers.get(e.Id) * e.length * (1-ir.get("qnn"));
                double pnh = ir.get("qnh") * (1-Math.exp(main_exp)) / main_den;
                double pnm = (ir.get("qhn") + ir.get("qnh") * Math.exp(main_exp)) / main_den - Math.exp(sec_exp);
                double pnn = Math.exp(sec_exp);
                assert 1.0 - Math.pow(10,-6) < pnh + pnm + pnn && pnh + pnm + pnn < 1.0 + Math.pow(10,-6);

                if (dice < pnh) { // Becomes homoplastic
                    new_state = 0;
                } else if (dice < pnh + pnm) { // New regular state
                    new_state = next_state;
                    next_state = next_state + 1;
                } else { // Stays the same
                    new_state = cstate;
                }
            }

            // Given the new state/value, save it and evolve below this (if it's not reticulate!)
            HashSet<Integer> hs = new HashSet<Integer>();
            hs.add(new_state);
            ret.put(e.child.Id, hs);
            if (!stopAtReticulate || e.child.reticulateEdge == null) {
                Pair<HashMap<Integer, HashSet<Integer>>, Integer> hashMapIntegerPair = evolveCharacterGenetically(e.child, hs, next_state, stopAtReticulate, noprint);
                next_state = hashMapIntegerPair.snd;
                ret.putAll(hashMapIntegerPair.fst);
            } else{
                int x=0;
            }

        }

        return new Pair<>(ret, next_state);

    }

    public void handleReticulateEdge(ReticulateEdge retEdge, HashMap<Integer, HashSet<Integer>> stateMap, boolean noprint) {
        // Will modify state map.

        double probability_transfer = transmissionSiteFactor * retEdge.transmission_strength;
        double dice = Class.randomProvider.nextDouble(Class.randomProvider.indexMapping.get("evolution"));
        if (dice < probability_transfer) {
            int left = retEdge.left.Id;
            int right = retEdge.right.Id;

            double dice2 = Class.randomProvider.nextDouble(Class.randomProvider.indexMapping.get("evolution"));
            if (dice2 > 0.5) { // l2r
                if (!noprint) System.out.println("Reticulate transfer L2R on character " + Integer.toString(id) + " at ret edge " + Integer.toString(retEdge.Id) + "!");
                HashSet<Integer> hs = new HashSet<>(); // Deep copy just in case
                for (int x : stateMap.get(left)) hs.add(x);
                stateMap.put(right, hs);
            } else { // r2l
                if (!noprint) System.out.println("Reticulate transfer R2L on character " + Integer.toString(id) + " at ret edge " + Integer.toString(retEdge.Id) + "!");
                HashSet<Integer> hs = new HashSet<>(); // Deep copy just in case
                for (int x : stateMap.get(right)) hs.add(x);
                stateMap.put(left, hs);
            }
        }
    }

}
