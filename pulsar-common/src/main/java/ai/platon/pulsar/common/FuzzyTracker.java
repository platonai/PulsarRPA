package ai.platon.pulsar.common;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

/**
 * <p>FuzzyTracker class.</p>
 *
 * @author vincent
 * @version $Id: $Id
 */
public class FuzzyTracker<T extends Comparable<T>> {

    private Map<T, Double> trackees = new TreeMap<>();

    /**
     * <p>Constructor for FuzzyTracker.</p>
     */
    public FuzzyTracker() {
    }

    /**
     * <p>size.</p>
     *
     * @return a int.
     */
    public int size() {
        return trackees.size();
    }

    /**
     * <p>isEmpty.</p>
     *
     * @return a boolean.
     */
    public boolean isEmpty() {
        return trackees.size() == 0;
    }

    /**
     * <p>remove.</p>
     *
     * @param t a T object.
     * @return a double.
     */
    public double remove(T t) {
        Double r = trackees.remove(t);
        return r == null ? 0.0 : r;
    }

    /**
     * <p>clear.</p>
     */
    public void clear() {
        trackees.clear();
    }

    /**
     * <p>get.</p>
     *
     * @param t a T object.
     * @return a double.
     */
    public double get(T t) {
        Double sim = trackees.get(t);

        if (sim == null)
            sim = 0.0;

        return sim;
    }

    /**
     * <p>set.</p>
     *
     * @param t a T object.
     * @param p a {@link ai.platon.pulsar.common.FuzzyProbability} object.
     */
    public void set(T t, FuzzyProbability p) {
        set(t, p.floor());
    }

    /**
     * <p>set.</p>
     *
     * @param t a T object.
     * @param sim a double.
     */
    public void set(T t, double sim) {
        Double oldValue = trackees.get(t);

        if (sim > 1.0) {
            sim = 1.0;
        }

        if (oldValue == null || oldValue < sim) {
            trackees.put(t, sim);
        }
    }

    /**
     * <p>inc.</p>
     *
     * @param t a T object.
     * @param sim a double.
     * @return a double.
     */
    public double inc(T t, double sim) {
        Double oldSim = trackees.get(t);

        if (oldSim != null) {
            sim += oldSim;
        }

        if (sim > 1) {
            sim = 1;
        }

        trackees.put(t, sim);

        return sim;
    }

    /**
     * <p>dec.</p>
     *
     * @param t a T object.
     * @param sim a double.
     * @return a double.
     */
    public double dec(T t, double sim) {
        Double oldSim = trackees.get(t);

        if (oldSim != null) {
            sim = oldSim - sim;
        }

        if (sim < FuzzyProbability.STRICTLY_NOT.ceiling()) {
            sim = 0.0;
            trackees.remove(t);
        } else {
            trackees.put(t, sim);
        }

        return sim;
    }

    // 寻找相似度值最大的项
    /**
     * <p>primaryKey.</p>
     *
     * @return a T object.
     */
    public T primaryKey() {
        T lastType = null;
        Double lastSim = 0.0;

        for (Entry<T, Double> entry : trackees.entrySet()) {
            if (lastSim < entry.getValue()) {
                lastSim = entry.getValue();
                lastType = entry.getKey();
            }
        }

        return lastType;
    }

    /**
     * <p>keySet.</p>
     *
     * @return a {@link java.util.Set} object.
     */
    public Set<T> keySet() {
        return trackees.keySet();
    }

    /**
     * <p>keySet.</p>
     *
     * @param p a {@link ai.platon.pulsar.common.FuzzyProbability} object.
     * @return a {@link java.util.Set} object.
     */
    public Set<T> keySet(FuzzyProbability p) {
        Set<T> keys = new HashSet<T>();

        for (T key : trackees.keySet()) {
            if (is(key, p)) {
                keys.add(key);
            }
        }

        return keys;
    }

    /**
     * <p>is.</p>
     *
     * @param key a T object.
     * @param p a {@link ai.platon.pulsar.common.FuzzyProbability} object.
     * @return a boolean.
     */
    public boolean is(T key, FuzzyProbability p) {
        Double sim = trackees.get(key);
        if (sim == null) {
            return false;
        }

        FuzzyProbability p2 = FuzzyProbability.of(sim);

        return p2.floor() >= p.floor();
    }

    /**
     * <p>maybe.</p>
     *
     * @param key a T object.
     * @return a boolean.
     */
    public boolean maybe(T key) {
        Double p = trackees.get(key);
        if (p == null) {
            return false;
        }

        return FuzzyProbability.maybe(p);
    }

    /**
     * <p>veryLikely.</p>
     *
     * @param key a T object.
     * @return a boolean.
     */
    public boolean veryLikely(T key) {
        Double p = trackees.get(key);
        if (p == null) {
            return false;
        }

        return FuzzyProbability.veryLikely(p);
    }

    /**
     * <p>mustBe.</p>
     *
     * @param key a T object.
     * @return a boolean.
     */
    public boolean mustBe(T key) {
        Double p = trackees.get(key);
        if (p == null) {
            return false;
        }

        return FuzzyProbability.mustBe(p);
    }

    /**
     * <p>certainly.</p>
     *
     * @param key a T object.
     * @return a boolean.
     */
    public boolean certainly(T key) {
        Double p = trackees.get(key);
        if (p == null) {
            return false;
        }

        return FuzzyProbability.certainly(p);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        int i = 0;
        for (Entry<T, Double> entry : trackees.entrySet()) {
            if (i++ > 0) {
                sb.append(",");
            }

            sb.append(entry.getKey());
            sb.append(":");
            sb.append(String.format("%1.2f", entry.getValue()));
        }

        return sb.toString();
    }
}
