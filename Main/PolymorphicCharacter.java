package Main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by marccanby on 2/20/22.
 */
public class PolymorphicCharacter extends AbstractCharacter {

    PolymorphicCharacterClass clas;

    public PolymorphicCharacter(PolymorphicCharacterClass clas, Network network, HashMap<Integer, Double> dlcModifiers, int id) {
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

        // Initialize return hashmap & rate constant
        HashMap<Integer, HashSet<Integer>> ret = new HashMap<>();

        for (Edge e : node.OutgoingEdges) {

            HashSet<Integer> new_state = new HashSet<>();
            for (int x : current_state) new_state.add(x);

            double t = e.child.Time; // Time of child node (does not change throughout loop)
            double t0 = node.Time; // Changes as we perform more events along this edge

            // Scale these so that it accounts for edge lengths modifiers and character height
            t = t * EdgeLengthModifiers.get(e.Id);
            t0 = t0 * EdgeLengthModifiers.get(e.Id);

            // Loop until stop doing events on this edge
            while (true) {
                // Figure out if an event will occur on this edge or not
                // Probability that event occurs on this edge is P(t0+tw >=t)=1-e^(-L(t-t0))
                double L = new_state.size() > 1 ? clas.birth_rate + Math.pow(new_state.size(), clas.death_power) * clas.death_rate : clas.birth_rate;
                double prob_event = 1- Math.exp(-L * (t-t0));

                // Draw a random number, and see if above/below threshold
                double dice = clas.randomProvider.nextDouble(clas.randomProvider.indexMapping.get("evolution")); // U0

                if (dice < prob_event) {
                    // Event occurs

                    // Figure out waiting time
                    double dice2 = clas.randomProvider.nextDouble(clas.randomProvider.indexMapping.get("evolution"));
                    double C = 1/(1-Math.exp(-L*(t-t0)));
                    double tw = 1 / L * Math.log(C/(C-dice2));
                    assert t0 + tw < t;

                    // Now decide birth or death
                    double prob_birth = clas.birth_rate / L;
                    if (new_state.size() == 1) assert prob_birth == 1;
                    double dice3 = clas.randomProvider.nextDouble(clas.randomProvider.indexMapping.get("evolution"));
                    if (dice3 < prob_birth) { // Birth
                        if (new_state.contains(0)) { // Can't add homoplastic since already there
                            new_state.add(next_state);
                            next_state = next_state + 1;
                        } else {
                            double dice4 = clas.randomProvider.nextDouble(clas.randomProvider.indexMapping.get("evolution"));
                            if (dice4 < clas.h_factor) new_state.add(0);
                            else {
                                new_state.add(next_state);
                                next_state = next_state + 1;
                            }
                        }
                    } else { // Death
                        assert new_state.size() > 1;
                        ArrayList<Integer> al = new ArrayList<>(new_state);
                        double dice4 = clas.randomProvider.nextDouble(clas.randomProvider.indexMapping.get("evolution"));
                        dice4 = Math.floor(dice4 * al.size());
                        int item_to_rmv = al.get((int) dice4);
                        new_state.remove(item_to_rmv);
                    }

                    t0 = t0 + tw;
                }
                else {
                    // No event

                    break; // Done with this edge
                }

            }

            // Given the new state/value, save it and evolve below this (if it's not reticulate!)
            ret.put(e.child.Id, new_state);
            if (!stopAtReticulate || e.child.reticulateEdge == null) {
                Pair<HashMap<Integer, HashSet<Integer>>, Integer> hashMapIntegerPair = evolveCharacterGenetically(e.child, new_state, next_state, stopAtReticulate, noprint);
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
                // Pick random element in the left side, and add it to the right side (regardless if already there)
                ArrayList<Integer> al = new ArrayList<>(stateMap.get(left));
                double dice3 = clas.randomProvider.nextDouble(clas.randomProvider.indexMapping.get("evolution"));
                dice3 = Math.floor(dice3 * al.size());
                int item_to_transfer = al.get((int) dice3);
                stateMap.get(right).add(item_to_transfer);

                if (!noprint) System.out.println("Reticulate transfer L2R on character " + Integer.toString(id) + " at ret edge " + Integer.toString(retEdge.Id) + "!");
            } else { // r2l
                // Pick random element in the right side, and add it to the left side (regardless if already there)
                ArrayList<Integer> al = new ArrayList<>(stateMap.get(right));
                double dice3 = clas.randomProvider.nextDouble(clas.randomProvider.indexMapping.get("evolution"));
                dice3 = Math.floor(dice3 * al.size());
                int item_to_transfer = al.get((int) dice3);
                stateMap.get(left).add(item_to_transfer);
                if (!noprint) System.out.println("Reticulate transfer R2L on character " + Integer.toString(id) + " at ret edge " + Integer.toString(retEdge.Id) + "!");
            }
        }
    }

}
