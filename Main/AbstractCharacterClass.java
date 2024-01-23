package Main;

import java.util.*;

/**
 * Created by marccanby on 10/9/20.
 */
public abstract class AbstractCharacterClass {
    // Character Class contains the settings for a group of characters, while Character is a specific character.
    // I have an abstract character class, so we can support different models

    public String PML; // Type of character
    public double weight; // Weight of character

    // Parameters for exponential Gaussian distribution for dlc and het.
    public double sigma_dlc;
    public boolean dlc_is_individual; // Mode for assigning dlc across chars within the class, Uniform or Individual
    public double sigma_het;

    public double class_height_factor; // Character Class height modifier, not redundant with mean and stddev because of skewedness

    public double  alpha_trm; // Mean of distribution from which site transmission strength drawn
    public double beta_trm; // Stdev of distribution from which site transmission strength drawn

    public int size; // number of characters in the class

    // Computed variables
    ArrayList<AbstractCharacter> Characters = new ArrayList<>();

    // Other
    public RandomProvider randomProvider;
    public Network Network;

    public AbstractCharacterClass(String type, HashMap<String, String> params, RandomProvider randomProvider, Network network) {
        PML = type;

        size = Integer.parseInt(params.get("nchar"));
        weight = Double.parseDouble(params.get("weight"));
        sigma_dlc = Float.parseFloat(params.get("sigma_dlc"));
        dlc_is_individual = Boolean.parseBoolean(params.get("dlc_is_individual"));
        sigma_het = Float.parseFloat(params.get("sigma_het"));
        class_height_factor = Float.parseFloat(params.get("height_factor"));
        alpha_trm = Double.parseDouble(params.get("alpha_trm_site"));
        beta_trm = Double.parseDouble(params.get("beta_trm_site"));

        this.randomProvider = randomProvider;
        this.randomProvider.addBetaDistribution("site", alpha_trm, beta_trm);
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

    public HashMap<Integer, ArrayList<HashSet<Integer>>> evolveCharacters(boolean evolveWithReticulate, boolean noprint) {
        HashMap<Integer, ArrayList<HashSet<Integer>>> result = new HashMap<>();
        for (int i = 0; i < Characters.size(); i++) {
            HashMap<Integer, HashSet<Integer>> charResult = Characters.get(i).evolveCharacter(evolveWithReticulate, noprint);
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


    private static LinkedHashMap<String, HashMap<Integer, HashSet<Integer>>> invertSequences(HashMap<Integer, LinkedHashMap<String, HashSet<Integer>>> sequences) {
        LinkedHashMap<String, HashMap<Integer, HashSet<Integer>>> res = new LinkedHashMap<>();
        LinkedHashMap<String, HashSet<Integer>> firstCharSet = sequences.get(sequences.keySet().toArray()[0]);
        for (String chara : firstCharSet.keySet()) {
            res.put(chara, new HashMap<>());
        }
        for (int node : sequences.keySet()) {
            for (String chara : sequences.get(node).keySet()) {
                res.get(chara).put(node, sequences.get(node).get(chara));
            }
        }
        return res;
    }

    public static String printSequences(HashMap<Integer, LinkedHashMap<String, HashSet<Integer>>> sequences, Network network, HashMap<String, Double> weightsByClass, boolean leaves_only, boolean noprint) {
        HashSet<Integer> leaves = network.getLeaves();
        if (!noprint) System.out.println("----------------------------------");

        // Figure out how many tabs follow each character
        HashMap<String, Integer> numTabsByChar = new HashMap<>();
        LinkedHashMap<String, HashSet<Integer>> firstCharSet = null;
        for (int node: sequences.keySet()) {
            if (leaves_only && !leaves.contains(node)) continue;
            LinkedHashMap<String, HashSet<Integer>> sequence = sequences.get(node);
            firstCharSet = sequence;
            for (String key : sequence.keySet()) {
                String lil_string = "";
                ArrayList<Integer> set = new ArrayList<>(sequence.get(key));
                for (int k = 0; k < set.size(); k++) {
                    if (k == 0) lil_string += String.valueOf(set.get(k));
                    else lil_string += "/" + String.valueOf(set.get(k));
                }
                if (!numTabsByChar.containsKey(key)) numTabsByChar.put(key, Math.max(2, lil_string.length() / 4 + 1));
                else {
                    int curr = numTabsByChar.get(key);
                    if (curr < lil_string.length() / 4 + 1) numTabsByChar.put(key, Math.max(2, lil_string.length() / 4 + 1));
                }
            }
        }

        // Make header (char numbers)
        String s1 = "";
        String s2 = "";
        int id = 1;
        for (String key : firstCharSet.keySet()) {
            s1 += String.valueOf(id); // Really should print name..... i.e. key.......
            for (int k =0; k < numTabsByChar.get(key); k++) {
                s1 += "\t";
                s2 += "----";
            }
        }
        if (!noprint) System.out.println("\t\t\t"+s1);
        if (!noprint) System.out.println("\t\t\t"+s2);

        // For each state observed at a leaf, I want to count how many total leaves it's seen in
        HashMap<String, HashMap<Integer, Integer>> leafStateCount = new HashMap<>(); // Char: {State Id: Count}

        String leafs = "";
        for (int node : sequences.keySet()) {
            if (leaves_only && !leaves.contains(node)) continue;
            LinkedHashMap<String, HashSet<Integer>> sequence = sequences.get(node);
            String s = "";
            for (String key : sequence.keySet()) {
                String lil_string = "";
                ArrayList<Integer> set = new ArrayList<>(sequence.get(key));
                for (int k = 0; k < set.size(); k++) {
                    int state_id = set.get(k);
                    if (k == 0) lil_string += String.valueOf(state_id);
                    else lil_string += "/" + String.valueOf(state_id);
                    if (leaves.contains(node)) {
                        if (!leafStateCount.containsKey(key)) leafStateCount.put(key, new HashMap<>());
                        else leafStateCount.get(key).put(state_id, leafStateCount.get(key).containsKey(state_id) ? leafStateCount.get(key).get(state_id) + 1 : 1);
                    }
                }
                s += lil_string;
                int num_tabs_already = lil_string.length() / 4;
                if (num_tabs_already < numTabsByChar.get(key)) {
                    for (int k = 0; k < numTabsByChar.get(key) - num_tabs_already; k++) {
                        s += "\t";
                    }
                }
            }
            if (network.getNodes().get(node).OutgoingEdges.size() == 0) leafs += "Leaf #" + String.valueOf(node) + ":\t" + s + "\n";
            else if (!noprint) System.out.println("Node #" + String.valueOf(node) + ":\t" + s);
        }

        if (!noprint) System.out.println(leafs);

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
        if (!noprint) System.out.println("----------------------------------");

        // Print CSV
        LinkedHashMap<String, HashMap<Integer, HashSet<Integer>>> inv = invertSequences(sequences);
        String header = "id,feature,weight";

        // Get leaves in order I want them printed
        HashMap<Integer, Vertex> vertexes = network.getNodes();
        ArrayList<String> realNodeOrder = new ArrayList<>();
        boolean all_start_t = true;
        for (int node: sequences.keySet()) if (leaves.contains(node)) {
            Vertex vert = vertexes.get(node);
            realNodeOrder.add(vert.Taxon);
            if (!(vert.Taxon.charAt(0) == 't')) all_start_t = false;
        }
        if (!all_start_t) Collections.sort(realNodeOrder);
        else {
           // In this case, all start with t, so just get the rest as int and sort that numerically
           ArrayList<Integer> newList = new ArrayList<>();
           for (String x : realNodeOrder) {
               newList.add(Integer.parseInt(x.substring(1, x.length())));
           }
           Collections.sort(newList);
           realNodeOrder = new ArrayList<>();
           for (int x : newList) realNodeOrder.add("t" + x);
        }
        ArrayList<Integer> nodeOrder = new ArrayList<>();
        for (String x : realNodeOrder) for (int y : vertexes.keySet()) if (vertexes.get(y).Taxon != null && vertexes.get(y).Taxon.equals(x)) nodeOrder.add(y);

        // Now actually print
        for (int node :nodeOrder) {
            header += (','+network.getNodes().get(node).Taxon);
        }
        //System.out.println(header);
        for (String key : inv.keySet()) {
            String row = key + ","+key;
            double weight = weightsByClass.get(key.replaceAll("\\d", ""));
            row += ("," + String.valueOf(weight));

            for (int node : nodeOrder) {
                ArrayList<Integer> set = new ArrayList<>(inv.get(key).get(node));
                String lil_string = "";
                for (int ls : set) {
                    if (lil_string.equals("")) lil_string += String.valueOf(ls);
                    else lil_string += ("/" + String.valueOf(ls));
                }
                row += (',' + lil_string);
            }
            header += ("\n" + row);
        }
        if (!noprint) System.out.println(header);
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
