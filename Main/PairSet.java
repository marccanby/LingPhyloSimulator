package Main;

import java.util.HashSet;

public class PairSet<T, U> {

    private PairMap<T, U, Integer> map; // just always use 0 for the integer

    public PairSet() {
        map = new PairMap<>();
    }

    public void add(Pair<T, U> pair) {
        map.put(pair, 0);
    }

    public int size() {
        return map.size();
    }

    public HashSet<Pair<T,U>> keySet() {
        return map.keySet();
    }
}
