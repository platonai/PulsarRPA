package ai.platon.pulsar.common;

import com.google.common.primitives.Ints;
import org.apache.commons.math3.analysis.function.Sigmoid;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * Created by vincent on 17-4-20.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 *
 * TODO: Use bigint
 */
public class ScoreVector implements Comparable<ScoreVector> {

    // Reserved
    private int dimension;
    private ArrayList<ScoreEntry> entries;

    public static ScoreVector ZERO = new ScoreVector(0);

    /**
     * Create zero score vector with dimension {dimension}
     * */
    public ScoreVector(int dimension) {
        this.dimension = dimension;
        entries = new ArrayList<>(dimension);
        for (int i = 0; i < dimension; ++i) {
            entries.add(new ScoreEntry("s" + i, i, 0, 0));
        }
    }

    /**
     * @param dimensionStr The string representation of a integer
     *                 Use string just to explicitly say it's not a score
     * @param scores   Score value for each dimension, the size
     */
    public ScoreVector(String dimensionStr, int... scores) {
        this(Integer.parseInt(dimensionStr), Ints.asList(scores));
    }

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

    public ScoreVector(int dimension, ScoreEntry... scores) {
        if (dimension != scores.length) {
            throw new IllegalArgumentException("Illegal dimension");
        }
        this.dimension = dimension;
        entries = new ArrayList<>(dimension);
        entries.addAll(Arrays.asList(scores));
    }

    public ScoreVector(int dimension, List<ScoreEntry> scores) {
        if (dimension != scores.size()) {
            throw new IllegalArgumentException("Illegal dimension");
        }
        this.dimension = dimension;
        entries = new ArrayList<>(scores);
    }

    public static ScoreVector parse(String multiValueScore) throws IllegalFormatException {
        String[] parts = multiValueScore.split(",");

        ScoreVector score = new ScoreVector(parts.length);
        for (int i = 0; i < parts.length; ++i) {
            score.setValue(i, Integer.parseInt(parts[i]));
        }

        return score;
    }

    public static ScoreVector combine(ScoreVector s1, ScoreVector s2) {
        List<ScoreEntry> entries = new ArrayList<>();
        entries.addAll(s1.entries);
        entries.addAll(s2.entries);
        return new ScoreVector(entries.size(), entries);
    }

    public static ScoreVector add(ScoreVector... scores) {
        List<ScoreEntry> entries = new ArrayList<>();
        for (ScoreVector score : scores) {
            entries.addAll(score.entries);
        }
        return new ScoreVector(entries.size(), entries);
    }

    public List<ScoreEntry> getEntries() {
        return entries;
    }

    public int getDimension() {
        return dimension;
    }

    public int getDigits() {
        return entries.stream().mapToInt(ScoreEntry::getDigits).sum();
    }

    public int size() {
        assert (dimension == entries.size());
        return entries.size();
    }

    public void setValue(int i, int value) {
        get(i).setValue(value);
    }

    public void setValue(int i, float value) {
        get(i).setValue((int) value);
    }

    public void setValue(int i, double value) {
        get(i).setValue((int) value);
    }

    public void setValue(int... values) {
        for (int i = 0; i < values.length && i < entries.size(); ++i) {
            entries.get(i).setValue(values[i]);
        }
    }

    public ScoreEntry get(int i) {
        return entries.get(i);
    }

    /**
     * TODO: numeric overflow
     * */
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

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int hash = 1;
        for (ScoreEntry scoreEntry : entries) {
            hash = PRIME * hash + scoreEntry.getValue();
        }
        return hash;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ScoreVector)) {
            return false;
        }

        return compareTo((ScoreVector) other) == 0;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (ScoreEntry scoreEntry : entries) {
            sb.append(",").append(scoreEntry.getValue());
        }
        sb.replace(0, 1, "");
        return sb.toString();
    }
}
