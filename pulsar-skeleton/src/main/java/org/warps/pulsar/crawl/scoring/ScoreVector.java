package org.warps.pulsar.crawl.scoring;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IllegalFormatException;
import java.util.List;

/**
 * Created by vincent on 17-4-20.
 * Copyright @ 2013-2017 Warpspeed Information. All rights reserved
 * <p>
 * TODO : optimization
 */
public class ScoreVector implements Comparable<ScoreVector> {

    // Reserved
    private int arity;
    private ArrayList<ScoreEntry> scoreEntries;

    public ScoreVector(int arity) {
        this.arity = arity;
        scoreEntries = new ArrayList<>(arity);
        for (int i = 0; i < arity; ++i) {
            scoreEntries.add(new ScoreEntry("s" + i, i, 0, 0));
        }
    }

    /**
     * @param arityStr The string representation of a integer
     *                 Use string just to explicitly say it's not a score
     * @param scores   Score value for each arity, the size
     */
    public ScoreVector(String arityStr, int... scores) {
        int arity = Integer.valueOf(arityStr);
        if (arity != scores.length) {
            throw new IllegalArgumentException("Illegal ScoreEntry number");
        }
        this.arity = arity;
        scoreEntries = new ArrayList<>(arity);
        for (int i = 0; i < arity; ++i) {
            scoreEntries.add(new ScoreEntry("s" + i, i, scores[i], 0));
        }
    }

    public ScoreVector(int arity, ScoreEntry... scores) {
        if (arity != scores.length) {
            throw new IllegalArgumentException("Illegal ScoreEntry number");
        }
        this.arity = arity;
        scoreEntries = new ArrayList<>(arity);
        scoreEntries.addAll(Arrays.asList(scores));
    }

    public ScoreVector(int arity, List<ScoreEntry> scores) {
        if (arity != scores.size()) {
            throw new IllegalArgumentException("Illegal ScoreEntry number");
        }
        this.arity = arity;
        scoreEntries = new ArrayList<>(scores);
    }

    public static ScoreVector parse(String multiValueScore) throws IllegalFormatException {
        String[] parts = multiValueScore.split(",");

        ScoreVector score = new ScoreVector(parts.length);
        for (int i = 0; i < parts.length; ++i) {
            score.setValue(i, Integer.valueOf(parts[i]));
        }

        return score;
    }

    public int getArity() {
        return arity;
    }

    public int getDigits() {
        return scoreEntries.stream().mapToInt(ScoreEntry::getDigits).sum();
    }

    public int size() {
        assert (arity == scoreEntries.size());
        return scoreEntries.size();
    }

    public void setValue(int i, int value) {
        get(i).setValue(value);
    }

    public void setValue(int i, float value) {
        get(i).setValue((int) value);
    }

    public void setValue(int... values) {
        for (int i = 0; i < values.length && i < scoreEntries.size(); ++i) {
            scoreEntries.get(i).setValue(values[i]);
        }
    }

    public ScoreEntry get(int i) {
        return scoreEntries.get(i);
    }

    /**
     * TODO : consider the "bigger arity, bigger value" semantics
     */
    @Override
    public int compareTo(@Nonnull ScoreVector other) {
        if (size() != other.size()) {
            return size() - other.size();
        }

        for (int i = 0; i < scoreEntries.size(); ++i) {
            ScoreEntry v1 = scoreEntries.get(i);
            ScoreEntry v2 = other.scoreEntries.get(i);

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
        for (ScoreEntry scoreEntry : scoreEntries) {
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
        for (ScoreEntry scoreEntry : scoreEntries) {
            sb.append(",").append(scoreEntry.getValue());
        }
        sb.replace(0, 1, "");
        return sb.toString();
    }
}
