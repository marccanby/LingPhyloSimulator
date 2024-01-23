package Main;

import java.util.HashMap;

public class PolymorphicCharacterClass extends AbstractCharacterClass {

    // Properties unique to the original evolution model
    public String name = "PolymorphicCharacterClass";
    public double h_root; // probability that the root starts in the homoplastic state
    public double h_factor; // the homoplasy factor

    public double birth_rate; // lambda
    public double death_rate; // mu
    public double death_power;

    public PolymorphicCharacterClass(String type, HashMap<String, String> params, RandomProvider randomProvider, Network network) {
        super(type, params, randomProvider, network);
        h_root = Float.parseFloat(params.get("h_root"));
        h_factor = Float.parseFloat(params.get("h_factor"));

        birth_rate = Float.parseFloat(params.get("birth_rate"));
        death_rate = Float.parseFloat(params.get("death_rate"));
        death_power = Float.parseFloat(params.get("death_power"));
    }

    public AbstractCharacter generateCharacter(HashMap<Integer, Double> dlcModifiers, int id) {
        return new PolymorphicCharacter(this, Network, dlcModifiers, id);
    }
}