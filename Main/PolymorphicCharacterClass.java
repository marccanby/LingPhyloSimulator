package Main;

import java.util.HashMap;

public class PolymorphicCharacterClass extends AbstractCharacterClass {

    // Properties unique to the original evolution model
    public double h_root; // probability that the root starts in the homoplastic state
    public double h_factor; // the homoplasy factor

    public double birth_rate; // lambda
    public double death_rate; // mu
    public double death_power;

    public PolymorphicCharacterClass(String type, HashMap<String, String> params, RandomProvider randomProvider, Network network) {
        super(type, params, randomProvider, network);
        if (type.equals("Morphological")) {
            h_root = 0.1;
            h_factor = 0.1;
        } else if (type.equals("Lexical")) {
            h_root = 0.1;
            h_factor = 0.1;
        } else {
            assert false;
        }
        birth_rate = 2;
        death_rate = 1;
        death_power = 3;
    }

    public AbstractCharacter generateCharacter(HashMap<Integer, Double> dlcModifiers, int id) {
        return new PolymorphicCharacter(this, Network, dlcModifiers, id);
    }
}