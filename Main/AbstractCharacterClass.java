package Main;

import java.util.*;

/**
 * Created by marccanby on 10/9/20.
 */
public abstract class AbstractCharacterClass {
    // Character Class contains the settings for a group of characters, while Character is a specific character.
    // I have an abstract character class, so we can support different models (Tandy's model, polymorphic, etc)

    public String PML; // Type of character

    // Parameters for exponential Gaussian distribution for dlc and het.
    public double sigma_dlc;
    public boolean dlc_is_individual; // Mode for assigning dlc across chars within the class, Uniform or Individual
        //09/13/05 - Tandy says Uniform is the only one that makes sense
        //as Individual is too close to n.c.m. - Francois
        //In 05/2022 Tandy seems fine with Individual
    public double sigma_het;

    public double class_height_factor; // Character Class height modifier, not redundant with mean and stddev because of skewedness

    public double mu_trm; // Mean of distribution from which site transmission strength drawn
    public double sigma_trm; // Stdev of distribution from which site transmission strength drawn

    public int size; // number of characters in the class

    // Computed variables
    ArrayList<AbstractCharacter> Characters = new ArrayList<>();

    // Other
    public RandomProvider randomProvider;
    public Network Network;

    public AbstractCharacterClass(String type, HashMap<String, String> params, RandomProvider randomProvider, Network network) {
        PML = type;

        size = Integer.parseInt(params.get("nchar"));
        sigma_dlc = Float.parseFloat(params.get("sigma_dlc"));
        dlc_is_individual = Boolean.parseBoolean(params.get("dlc_is_individual"));
        sigma_het = Float.parseFloat(params.get("sigma_het"));
        class_height_factor = Float.parseFloat(params.get("height_factor"));
        mu_trm = Float.parseFloat(params.get("mu_trm_site"));
        sigma_trm = Float.parseFloat(params.get("sigma_trm_site"));

        this.randomProvider = randomProvider;
        Network = network;
    }


    public void createCharacters() {
        Characters.add(generateCharacter(null, 0));
        for (int i = 1; i < size; i++) {
            if (!dlc_is_individual) Characters.add(generateCharacter(Characters.get(0).DlcModifiers, i));
            else Characters.add(generateCharacter(null, i));
        }
    }

    public abstract AbstractCharacter generateCharacter(HashMap<Integer, Double> edgeLengthModifiers, int id);

    public HashMap<Integer, ArrayList<HashSet<Integer>>> evolveCharacters(boolean evolveWithReticulate) {
        HashMap<Integer, ArrayList<HashSet<Integer>>> result = new HashMap<>();
        for (int i = 0; i < Characters.size(); i++) {
            HashMap<Integer, HashSet<Integer>> charResult = Characters.get(i).evolveCharacter(evolveWithReticulate);
            for (Integer key : charResult.keySet()) {
                if (i == 0) {
                    ArrayList<HashSet<Integer>> ar = new ArrayList<>();
                    ar.add(charResult.get(key));
                    result.put(key, ar);
                } else {
                    result.get(key).add(charResult.get(key));
                }
            }
        }
        return result;
    }


    private static ArrayList<HashMap<Integer, HashSet<Integer>>> invertSequences(HashMap<Integer, ArrayList<HashSet<Integer>>> sequences) {
        ArrayList<HashMap<Integer, HashSet<Integer>>> res = new ArrayList<>();
        int nchar = sequences.get(sequences.keySet().toArray()[0]).size();
        for (int i = 0; i < nchar; i++) {
            res.add(new HashMap<>());
        }
        for (int node : sequences.keySet()) {
            for (int i = 0; i < sequences.get(node).size(); i++) {
                res.get(i).put(node, sequences.get(node).get(i));
            }
        }
        return res;
    }

    public static String printSequences(HashMap<Integer, ArrayList<HashSet<Integer>>> sequences, Network network, boolean leaves_only) {
        HashSet<Integer> leaves = network.getLeaves();
        System.out.println("----------------------------------");

        // Figure out how many tabs follow each character
        HashMap<Integer, Integer> numTabsByChar = new HashMap<>();
        int nchar = -1;
        for (int node: sequences.keySet()) {
            if (leaves_only && !leaves.contains(node)) continue;
            ArrayList<HashSet<Integer>> sequence = sequences.get(node);
            nchar = sequence.size();
            for (int j = 0; j < sequence.size(); j++) {
                String lil_string = "";
                ArrayList<Integer> set = new ArrayList<>(sequence.get(j));
                for (int k = 0; k < set.size(); k++) {
                    if (k == 0) lil_string += String.valueOf(set.get(k));
                    else lil_string += "," + String.valueOf(set.get(k));
                }
                if (!numTabsByChar.containsKey(j)) numTabsByChar.put(j, Math.max(2, lil_string.length() / 4 + 1));
                else {
                    int curr = numTabsByChar.get(j);
                    if (curr < lil_string.length() / 4 + 1) numTabsByChar.put(j, Math.max(2, lil_string.length() / 4 + 1));
                }
            }
        }

        // Make header (char numbers)
        String s1 = "";
        String s2 = "";
        for (int j = 0; j < nchar; j++) {
            int id = j; //Characters.get(j).id;
            s1 += String.valueOf(id);
            for (int k =0; k < numTabsByChar.get(j); k++) {
                s1 += "\t";
                s2 += "----";
            }
        }
        System.out.println("\t\t\t"+s1);
        System.out.println("\t\t\t"+s2);

        // For each state observed at a leaf, I want to count how many total leaves it's seen in
        HashMap<Integer, HashMap<Integer, Integer>> leafStateCount = new HashMap<>(); // Char: {State Id: Count}

        String leafs = "";
        for (int node : sequences.keySet()) {
            if (leaves_only && !leaves.contains(node)) continue;
            ArrayList<HashSet<Integer>> sequence = sequences.get(node);
            String s = "";
            for (int j = 0; j < sequence.size(); j++) {
                String lil_string = "";
                ArrayList<Integer> set = new ArrayList<>(sequence.get(j));
                for (int k = 0; k < set.size(); k++) {
                    int state_id = set.get(k);
                    if (k == 0) lil_string += String.valueOf(state_id);
                    else lil_string += "," + String.valueOf(state_id);
                    if (leaves.contains(node)) {
                        if (!leafStateCount.containsKey(j)) leafStateCount.put(j, new HashMap<>());
                        else leafStateCount.get(j).put(state_id, leafStateCount.get(j).containsKey(state_id) ? leafStateCount.get(j).get(state_id) + 1 : 1);
                    }
                }
                s += lil_string;
                int num_tabs_already = lil_string.length() / 4;
                if (num_tabs_already < numTabsByChar.get(j)) {
                    for (int k = 0; k < numTabsByChar.get(j) - num_tabs_already; k++) {
                        s += "\t";
                    }
                }
            }
            if (network.getNodes().get(node).OutgoingEdges.size() == 0) leafs += "Leaf #" + String.valueOf(node) + ":\t" + s + "\n";
            else System.out.println("Node #" + String.valueOf(node) + ":\t" + s);
        }

        System.out.println(leafs);

//        // Decide I want to print for each character, the numbers of most observed state to least observed....
//        HashMap<Integer, ArrayList<String>> stateCounts = new HashMap<>();
//        for (int ch : leafStateCount.keySet()) {
//            HashMap<Integer, Integer> map = leafStateCount.get(ch);
//            for (int i : map.keySet()) map.put(i, -map.get(i));
//            LinkedHashMap<Integer, Integer> lhm = sortHashMapByValues(map);
//
//            ArrayList<String> al = new ArrayList<>();
//            for (int x: lhm.keySet()) {
//                al.add(String.valueOf(-lhm.get(x))+ ":" + String.valueOf(x) );
//            }
//            stateCounts.put(ch, al);
//
//        }
//        int iter = 1;
//        while (true) {
//            String line = "Rank " + String.valueOf(iter) + (iter < 10 ? ":\t\t" : ":\t");
//            boolean all0 = true;
//            for (int ch : stateCounts.keySet()) {
//                ArrayList<String> al = stateCounts.get(ch);
//                String to_app = " ";
//                if (al.size() > 0) {
//                    all0 = false;
//                    to_app = al.get(0);
//                    stateCounts.get(ch).remove(0);
//                }
//                line += to_app;
//                int num_tabs_already = to_app.length() / 4;
//                if (num_tabs_already < numTabsByChar.get(ch)) {
//                    for (int k = 0; k < numTabsByChar.get(ch) - num_tabs_already; k++) {
//                        line += "\t";
//                    }
//                }            }
//            if (all0) break;
//            System.out.println(line);
//            iter += 1;
//
//        }
        System.out.println("----------------------------------");

        // Print CSV
        ArrayList<HashMap<Integer, HashSet<Integer>>> inv = invertSequences(sequences);
        String header = "id,feature,weight";
        ArrayList<Integer> nodeOrder = new ArrayList<>();
        for (int node: sequences.keySet()) if (leaves.contains(node)) nodeOrder.add(node);
        for (int node :nodeOrder) {
            header += (','+network.getNodes().get(node).Taxon);
        }
        //System.out.println(header);
        for (int i =0; i < inv.size(); i++) {
            String row = "c" + String.valueOf(i) + ",cc"+String.valueOf(i) + ",1";
            for (int node : nodeOrder) {
                ArrayList<Integer> set = new ArrayList<>(inv.get(i).get(node));
                row += (',' + String.valueOf(set.get(0)));
            }
            header += ("\n" + row);
        }
        System.out.println(header);
        return header;

    }

    public LinkedHashMap<Integer, Integer> sortHashMapByValues(
            HashMap<Integer, Integer> passedMap) {
        List<Integer> mapKeys = new ArrayList<>(passedMap.keySet());
        List<Integer> mapValues = new ArrayList<>(passedMap.values());
        Collections.sort(mapValues);
        Collections.sort(mapKeys);

        LinkedHashMap<Integer, Integer> sortedMap =
                new LinkedHashMap<>();

        Iterator<Integer> valueIt = mapValues.iterator();
        while (valueIt.hasNext()) {
            Integer val = valueIt.next();
            Iterator<Integer> keyIt = mapKeys.iterator();

            while (keyIt.hasNext()) {
                Integer key = keyIt.next();
                Integer comp1 = passedMap.get(key);
                Integer comp2 = val;

                if (comp1.equals(comp2)) {
                    keyIt.remove();
                    sortedMap.put(key, val);
                    break;
                }
            }
        }
        return sortedMap;
    }


}
