package fun.platonic.pulsar.crawl.scoring;

import fun.platonic.pulsar.common.config.Params;

import javax.annotation.Nonnull;

/**
 * Created by vincent on 17-4-20.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
public class ScoreEntry implements Comparable<ScoreEntry> {

    public static final int DEFAULT_DIGITS = 5;

    private String name = "";
    private int priority = 0;
    private int value = 0;
    // Reserved for optimization
    private int digits = DEFAULT_DIGITS;

    public ScoreEntry(String name, int value) {
        this(name, 0, value, DEFAULT_DIGITS);
    }

    public ScoreEntry(String name, int priority, int value, int digits) {
        this.name = name;
        this.priority = priority;
        this.value = value;
        this.digits = digits;
    }

    public ScoreEntry(ScoreEntry other) {
        this.name = other.name;
        this.priority = other.priority;
        this.value = other.value;
        this.digits = other.digits;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public int getDigits() {
        return digits;
    }

    public void setDigits(int digits) {
        this.digits = digits;
    }

    @Override
    public ScoreEntry clone() {
        return new ScoreEntry(name, priority, value, digits);
    }

    @Override
    public String toString() {
        return Params.formatAsLine(
                "name", name,
                "priority", priority,
                "value", value,
                "digits", digits
        );
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ScoreEntry && compareTo((ScoreEntry) other) == 0;
    }

    @Override
    public int compareTo(@Nonnull ScoreEntry scoreEntry) {
        int diff = priority - scoreEntry.priority;
        if (diff != 0) {
            return diff;
        }

        return value - scoreEntry.value;
    }
}
