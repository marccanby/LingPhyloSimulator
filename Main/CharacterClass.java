package Main;

import java.util.HashMap;

/**
 * Created by marccanby on 7/17/20.
 */
public class CharacterClass extends AbstractCharacterClass {

    // Properties unique to the original evolution model
    public String name = "CharacterClass";
    public double h_root; // probability that the root starts in the homoplastic state
    public double h_factor; // the homoplasy factor such that q(n,n') = 1-h ; q(n,h*) = h ; q(h*,h*) = 0

    public CharacterClass(String type, HashMap<String, String> params, RandomProvider randomProvider, Network network) {
        super(type, params, randomProvider, network);
        h_root = Float.parseFloat(params.get("h_root"));
        h_factor = Float.parseFloat(params.get("h_factor"));
    }

    public AbstractCharacter generateCharacter(HashMap<Integer, Double> dlcModifiers, int id) {
        return new Character(this, Network, dlcModifiers, id);
    }

    public HashMap<String, Double> getInfinitesimalRates() {
        HashMap<String, Double> ret = new HashMap<>();
        ret.put("qhh" , 0.0);
        ret.put("qhn" , 1.0);
        ret.put("qnn" , 0.0);
        ret.put("qnm" , 1-h_factor); // Not sure if this is quite right for phonological chars, look into that
        ret.put("qnh" , h_factor);
        return ret;
    }


}
