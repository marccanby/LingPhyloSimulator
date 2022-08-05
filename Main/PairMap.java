package Main;

import java.util.HashMap;
import java.util.HashSet;

public class PairMap<T, U, V> {

    private HashMap<T, HashMap<U, V>> map;

    public PairMap() {
        map = new HashMap<T, HashMap<U, V>>();
    }

    public void put(Pair<T, U> key, V value) {
        if (!map.containsKey(key.fst)) {
            map.put(key.fst, new HashMap<U, V>());
        }
        map.get(key.fst).put(key.snd, value);
    }

    public V get(Pair<T, U> key) {
        if (!map.containsKey(key.fst)) return null;
        HashMap<U,V> snd = map.get(key.fst);
        if (!snd.containsKey(key.snd)) return null;
        return snd.get(key.snd);
    }

    public HashSet<Pair<T,U>> keySet() {
        HashSet<Pair<T,U>> ret = new HashSet<Pair<T,U>>();
        for (T t : map.keySet()) {
            for (U u : map.get(t).keySet()) {
                Pair<T, U> pair = new Pair(t, u);
                ret.add(pair);
            }
        }
        return ret;
    }

    public void remove(Pair<T, U> key) {
        if (!map.containsKey(key.fst)) return;
        HashMap<U, V> snd = map.get(key.fst);
        if (!snd.containsKey(key.snd)) return;
        snd.remove(key.snd);
        if (snd.size() == 0) map.remove(key.fst);
    }

    public void putAll(PairMap<T, U, V> oldMapping) {
        for (Pair<T, U> pair: oldMapping.keySet()) {
            this.put(pair, oldMapping.get(pair));
        }
    }

    public int size() {
        int sm = 0;
        for (T t : map.keySet()) {
            sm += map.get(t).size();
        }
        return sm;
    }

}
