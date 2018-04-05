package fun.platonic.pulsar.dom;

import org.jsoup.helper.Validate;

import java.util.*;

/**
 * The features of an Element.
 * <p>
 * Features are treated as a map: there can be only one value associated with an feature key/name.
 * </p>
 * <p>
 * Feature name and value comparisons are  generally <b>case sensitive</b>. By default for HTML, feature names are
 * normalized to lower-case on parsing. That means you should use lower-case strings when referring to features by
 * name.
 * </p>
 *
 * @author Jonathan Hedley, jonathan@hedley.net
 * @author Vincent Zhang, galaxyeye@live.cn
 */
public class Features implements Iterable<Feature>, Cloneable {
    private static final int InitialCapacity = 30;

    // manages the key/val arrays
    private static final int GrowthFactor = 2;
    public static final int[] Empty = {};
    public static final double[] EmptyDouble = {};
    public static final int NotFound = -1;
    public static final double Zero = 0.0;

    private int size = 0; // number of slots used (not capacity, which is keys.length
    int[] keys = Empty;
    double[] vals = EmptyDouble;

    // check there's room for more
    private void checkCapacity(int minNewSize) {
        Validate.isTrue(minNewSize >= size);
        int curSize = keys.length;
        if (curSize >= minNewSize)
            return;

        int newSize = curSize >= InitialCapacity ? size * GrowthFactor : InitialCapacity;
        if (minNewSize > newSize)
            newSize = minNewSize;

        keys = copyOf(keys, newSize);
        vals = copyOf(vals, newSize);
    }

    // simple implementation of Arrays.copy, for support of Android API 8.
    private static int[] copyOf(int[] orig, int size) {
        final int[] copy = new int[size];
        System.arraycopy(orig, 0, copy, 0, Math.min(orig.length, size));
        return copy;
    }

    // simple implementation of Arrays.copy, for support of Android API 8.
    private static double[] copyOf(double[] orig, int size) {
        final double[] copy = new double[size];
        System.arraycopy(orig, 0, copy, 0, Math.min(orig.length, size));
        return copy;
    }

    int indexOfKey(int key) {
        Validate.notNull(key);
        for (int i = 0; i < size; i++) {
            if (key == keys[i]) {
                return i;
            }
        }
        return NotFound;
    }

    private int indexOfKeyIgnoreCase(int key) {
        Validate.notNull(key);
        for (int i = 0; i < size; i++) {
            if (key == keys[i]) {
                return i;
            }
        }
        return NotFound;
    }

    public int[] getKeys() {
        return keys;
    }

    public double[] getVals() {
        return vals;
    }

    /**
     * Get an attribute value by key.
     *
     * @param key the (case-sensitive) attribute key
     * @return the attribute value if set; or zero if not set.
     * @see #hasKey(int)
     */
    public double get(int key) {
        int i = indexOfKey(key);
        return i == NotFound ? Zero : vals[i];
    }

    // adds without checking if this key exists
    private void add(int key, double value) {
        checkCapacity(size + 1);
        keys[size] = key;
        vals[size] = value;
        size++;
    }

    /**
     * Set a new attribute, or replace an existing one by key.
     *
     * @param key   case sensitive attribute key
     * @param value attribute value
     * @return these features, for chaining
     */
    public Features put(int key, double value) {
        int i = indexOfKey(key);
        if (i != NotFound)
            vals[i] = value;
        else
            add(key, value);
        return this;
    }

    /**
     * Set a new attribute, or replace an existing one by key.
     *
     * @param feature feature with case sensitive key
     * @return these features, for chaining
     */
    public Features put(Feature feature) {
        Validate.notNull(feature);
        put(feature.getKey(), feature.getValue());
        return this;
    }

    // removes and shifts up
    private void removeIndex(int index) {
        Validate.isFalse(index >= size);
        int shifted = size - index - 1;
        if (shifted > 0) {
            System.arraycopy(keys, index + 1, keys, index, shifted);
            System.arraycopy(vals, index + 1, vals, index, shifted);
        }
        size--;
        keys[size] = Integer.MIN_VALUE; // release hold
        vals[size] = Zero;
    }

    /**
     * Remove an attribute by key. <b>Case sensitive.</b>
     *
     * @param key attribute key to remove
     */
    public void remove(int key) {
        int i = indexOfKey(key);
        if (i != NotFound)
            removeIndex(i);
    }

    /**
     * Tests if these features contain an attribute with this key.
     *
     * @param key case-sensitive key to check for
     * @return true if key exists, false otherwise
     */
    public boolean hasKey(int key) {
        return indexOfKey(key) != NotFound;
    }

    /**
     * Get the number of features in this set.
     *
     * @return size
     */
    public int size() {
        return size;
    }

    /**
     * Add all the features from the incoming set to this set.
     *
     * @param incoming features to add to these features.
     */
    public void addAll(Features incoming) {
        if (incoming.size() == 0) {
            return;
        }

        checkCapacity(size + incoming.size);

        for (Feature feature : incoming) {
            // todo - should this be case insensitive?
            put(feature);
        }
    }

    public Iterator<Feature> iterator() {
        return new Iterator<Feature>() {
            int i = 0;

            @Override
            public boolean hasNext() {
                return i < size;
            }

            @Override
            public Feature next() {
                final Feature feature = new Feature(keys[i], vals[i]);
                i++;
                return feature;
            }

            @Override
            public void remove() {
                Features.this.remove(--i); // next() advanced, so rewind
            }
        };
    }

    /**
     * Get the features as a List, for iteration.
     *
     * @return an view of the features as an unmodifialbe List.
     */
    public List<Feature> asList() {
        ArrayList<Feature> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(new Feature(keys[i], vals[i]));
        }
        return Collections.unmodifiableList(list);
    }

    /**
     * Retrieves a filtered view of features that are HTML5 custom data features; that is, features with keys
     * starting with {@code data-}.
     *
     * @return map of custom data features.
     */
    public Map<Integer, Double> dataset() {
        return new Dataset(this);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < size; i++) {
            sb.append(keys[i]).append(":").append(vals[i]).append(' ');
        }
        return sb.toString();
    }

    /**
     * Checks if these features are equal to another set of features, by comparing the two sets
     *
     * @param o features to compare with
     * @return if both sets of features have the same content
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Features that = (Features) o;

        if (size != that.size) return false;
        if (!Arrays.equals(keys, that.keys)) return false;
        return Arrays.equals(vals, that.vals);
    }

    /**
     * Calculates the hashcode of these features, by iterating all features and summing their hashcodes.
     *
     * @return calculated hashcode
     */
    @Override
    public int hashCode() {
        int result = size;
        result = 31 * result + Arrays.hashCode(keys);
        result = 31 * result + Arrays.hashCode(vals);
        return result;
    }

    @Override
    public Features clone() {
        Features clone;
        try {
            clone = (Features) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
        clone.size = size;
        keys = copyOf(keys, size);
        vals = copyOf(vals, size);
        return clone;
    }

    private static class Dataset extends AbstractMap<Integer, Double> {
        private final Features features;

        private Dataset(Features features) {
            this.features = features;
        }

        @Override
        public Set<Entry<Integer, Double>> entrySet() {
            return new EntrySet();
        }

        @Override
        public Double put(Integer key, Double value) {
            double oldValue = features.hasKey(key) ? features.get(key) : null;
            features.put(key, value);
            return oldValue;
        }

        private class EntrySet extends AbstractSet<Entry<Integer, Double>> {
            @Override
            public Iterator<Entry<Integer, Double>> iterator() {
                return new DatasetIterator();
            }

            @Override
            public int size() {
                int count = 0;
                Iterator iter = new DatasetIterator();
                while (iter.hasNext())
                    count++;
                return count;
            }
        }

        private class DatasetIterator implements Iterator<Entry<Integer, Double>> {
            private Iterator<Feature> featurIter = features.iterator();
            private Feature feature;

            public boolean hasNext() {
                while (featurIter.hasNext()) {
                    feature = featurIter.next();
                    return true;
                }
                return false;
            }

            public Entry<Integer, Double> next() {
                return new Feature(feature.getKey(), feature.getValue());
            }

            public void remove() {
                features.remove(feature.getKey());
            }
        }
    }
}
