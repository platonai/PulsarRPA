package ai.platon.pulsar.common;

import com.google.common.primitives.Ints;
import org.apache.commons.math3.analysis.function.Sigmoid;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by vincent on 17-4-20.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 *
 * @author vincent
 * @version $Id: $Id
 */
public class ScoreVector implements Comparable<ScoreVector> {

    // Reserved
    private final int dimension;
    private final ArrayList<ScoreEntry> entries;

    /** Constant <code>ZERO</code> */
    public static ScoreVector ZERO = new ScoreVector(0);

    /**
     * Create zero score vector with dimension {dimension}
     *
     * @param dimension a int.
     */
    public ScoreVector(int dimension) {
        this.dimension = dimension;
        entries = new ArrayList<>(dimension);
        for (int i = 0; i < dimension; ++i) {
            entries.add(new ScoreEntry("s" + i, i, 0, 0));
        }
    }

    /**
     * <p>Constructor for ScoreVector.</p>
     *
     * @param dimensionStr The string representation of a integer
     *                 Use string just to explicitly say it's not a score
     * @param scores   Score value for each dimension, the size
     */
    public ScoreVector(String dimensionStr, int... scores) {
        this(Integer.parseInt(dimensionStr), Ints.asList(scores));
    }

    /**
     * <p>Constructor for ScoreVector.</p>
     *
     * @param dimension a int.
     * @param scores a {@link java.util.Collection} object.
     */
    public ScoreVector(int dimension, Collection<Integer> scores) {
        Integer[] list = scores.toArray(new Integer[0]);
        if (dimension != list.length) {
            throw new IllegalArgumentException("Illegal dimension");
        }
        this.dimension = dimension;
        entries = new ArrayList<>(dimension);
        for (int i = 0; i < dimension; ++i) {
            entries.add(new ScoreEntry("s" + i, i, list[i], 0));
        }
    }

    /**
     * <p>Constructor for ScoreVector.</p>
     *
     * @param dimension a int.
     * @param scores a {@link ai.platon.pulsar.common.ScoreEntry} object.
     */
    public ScoreVector(int dimension, ScoreEntry... scores) {
        if (dimension != scores.length) {
            throw new IllegalArgumentException("Illegal dimension");
        }
        this.dimension = dimension;
        entries = new ArrayList<>(dimension);
        entries.addAll(Arrays.asList(scores));
    }

    /**
     * <p>Constructor for ScoreVector.</p>
     *
     * @param dimension a int.
     * @param scores a {@link java.util.List} object.
     */
    public ScoreVector(int dimension, List<ScoreEntry> scores) {
        if (dimension != scores.size()) {
            throw new IllegalArgumentException("Illegal dimension");
        }
        this.dimension = dimension;
        entries = new ArrayList<>(scores);
    }

    /**
     * <p>create.</p>
     *
     * @param template a {@link ai.platon.pulsar.common.ScoreVector} object.
     * @return a {@link ai.platon.pulsar.common.ScoreVector} object.
     */
    public static ScoreVector create(ScoreVector template) {
        List<ScoreEntry> newEntries = template.entries.stream().map(ScoreEntry::clone).collect(Collectors.toList());
        newEntries.forEach(scoreEntry -> scoreEntry.setValue(0));
        return new ScoreVector(template.dimension, newEntries);
    }

    /**
     * <p>parse.</p>
     *
     * @param multiValueScore a {@link java.lang.String} object.
     * @return a {@link ai.platon.pulsar.common.ScoreVector} object.
     * @throws java.util.IllegalFormatException if any.
     */
    public static ScoreVector parse(String multiValueScore) throws IllegalFormatException {
        String[] parts = multiValueScore.split(",");

        ScoreVector score = new ScoreVector(parts.length);
        for (int i = 0; i < parts.length; ++i) {
            score.setValue(i, Integer.parseInt(parts[i]));
        }

        return score;
    }

    /**
     * <p>combine.</p>
     *
     * @param s1 a {@link ai.platon.pulsar.common.ScoreVector} object.
     * @param s2 a {@link ai.platon.pulsar.common.ScoreVector} object.
     * @return a {@link ai.platon.pulsar.common.ScoreVector} object.
     */
    public static ScoreVector combine(ScoreVector s1, ScoreVector s2) {
        List<ScoreEntry> entries = new ArrayList<>();
        entries.addAll(s1.entries);
        entries.addAll(s2.entries);
        return new ScoreVector(entries.size(), entries);
    }

    /**
     * <p>add.</p>
     *
     * @param scores a {@link ai.platon.pulsar.common.ScoreVector} object.
     * @return a {@link ai.platon.pulsar.common.ScoreVector} object.
     */
    public static ScoreVector add(ScoreVector... scores) {
        List<ScoreEntry> entries = new ArrayList<>();
        for (ScoreVector score : scores) {
            entries.addAll(score.entries);
        }
        return new ScoreVector(entries.size(), entries);
    }

    /**
     * <p>Getter for the field <code>entries</code>.</p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<ScoreEntry> getEntries() {
        return entries;
    }

    /**
     * <p>Getter for the field <code>dimension</code>.</p>
     *
     * @return a int.
     */
    public int getDimension() {
        return dimension;
    }

    /**
     * <p>getDigits.</p>
     *
     * @return a int.
     */
    public int getDigits() {
        return entries.stream().mapToInt(ScoreEntry::getDigits).sum();
    }

    /**
     * <p>size.</p>
     *
     * @return a int.
     */
    public int size() {
        assert (dimension == entries.size());
        return entries.size();
    }

    /**
     * <p>setValue.</p>
     *
     * @param i a int.
     * @param value a int.
     */
    public void setValue(int i, int value) {
        get(i).setValue(value);
    }

    /**
     * <p>setValue.</p>
     *
     * @param i a int.
     * @param value a float.
     */
    public void setValue(int i, float value) {
        get(i).setValue((int) value);
    }

    /**
     * <p>setValue.</p>
     *
     * @param i a int.
     * @param value a double.
     */
    public void setValue(int i, double value) {
        get(i).setValue((int) value);
    }

    /**
     * <p>setValue.</p>
     *
     * @param values a int.
     */
    public void setValue(int... values) {
        for (int i = 0; i < values.length && i < entries.size(); ++i) {
            entries.get(i).setValue(values[i]);
        }
    }

    /**
     * <p>get.</p>
     *
     * @param i a int.
     * @return a {@link ai.platon.pulsar.common.ScoreEntry} object.
     */
    public ScoreEntry get(int i) {
        return entries.get(i);
    }

    /** {@inheritDoc} */
    @Override
    public ScoreVector clone() {
        List<ScoreEntry> newEntries = entries.stream().map(ScoreEntry::clone).collect(Collectors.toList());
        return new ScoreVector(dimension, newEntries);
    }

    /**
     * TODO: numeric overflow, use bigint
     *
     * @return a double.
     */
    public double toDouble() {
        // TODO: normalization
        Sigmoid sig = new Sigmoid(0, 1);

        double sum = 0.0;
        for (ScoreEntry entry : entries) {
            double s = entry.getValue();
            s = sig.value(s);
            s = (int) Math.min(100 * s, 99);
            sum = 100 * sum + s;
        }
        return sum;
    }

    /**
     * {@inheritDoc}
     *
     * TODO : consider about the "bigger dimension, bigger value" semantics
     */
    @Override
    public int compareTo(@Nonnull ScoreVector other) {
        if (size() != other.size()) {
            return size() - other.size();
        }

        for (int i = 0; i < entries.size(); ++i) {
            ScoreEntry v1 = entries.get(i);
            ScoreEntry v2 = other.entries.get(i);

            int comp = v1.compareTo(v2);
            if (comp != 0) {
                return comp;
            }
        }

        return 0;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        final int PRIME = 31;
        int hash = 1;
        for (ScoreEntry scoreEntry : entries) {
            hash = PRIME * hash + scoreEntry.getValue();
        }
        return hash;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ScoreVector)) {
            return false;
        }

        return compareTo((ScoreVector) other) == 0;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return entries.stream().map(ScoreEntry::getValue).map(String::valueOf)
                .collect(Collectors.joining(","));
    }
}
