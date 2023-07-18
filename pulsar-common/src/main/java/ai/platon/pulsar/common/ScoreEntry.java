package ai.platon.pulsar.common;

import ai.platon.pulsar.common.config.Params;

import javax.annotation.Nonnull;

/**
 * Created by vincent on 17-4-20.
 * Copyright @ 2013-2023 Platon AI. All rights reserved
 *
 * @author vincent
 * @version $Id: $Id
 */
public class ScoreEntry implements Comparable<ScoreEntry> {

    
    public static final int DEFAULT_DIGITS = 5;

    private String name;
    private int priority;
    private int value;
    // Reserved for optimization
    private int digits = DEFAULT_DIGITS;

    /**
     * <p>Constructor for ScoreEntry.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @param value a int.
     */
    public ScoreEntry(String name, int value) {
        this(name, 0, value, DEFAULT_DIGITS);
    }

    /**
     * <p>Constructor for ScoreEntry.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @param priority a int.
     * @param value a int.
     */
    public ScoreEntry(String name, int priority, int value) {
        this(name, priority, value, DEFAULT_DIGITS);
    }

    /**
     * <p>Constructor for ScoreEntry.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @param priority a int.
     * @param value a int.
     * @param digits a int.
     */
    public ScoreEntry(String name, int priority, int value, int digits) {
        this.name = name;
        this.priority = priority;
        this.value = value;
        this.digits = digits;
    }

    /**
     * <p>Constructor for ScoreEntry.</p>
     *
     * @param other a {@link ai.platon.pulsar.common.ScoreEntry} object.
     */
    public ScoreEntry(ScoreEntry other) {
        this.name = other.name;
        this.priority = other.priority;
        this.value = other.value;
        this.digits = other.digits;
    }

    /**
     * <p>Getter for the field <code>name</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getName() {
        return name;
    }

    /**
     * <p>Setter for the field <code>name</code>.</p>
     *
     * @param name a {@link java.lang.String} object.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * <p>Getter for the field <code>priority</code>.</p>
     *
     * @return a int.
     */
    public int getPriority() {
        return priority;
    }

    /**
     * <p>Setter for the field <code>priority</code>.</p>
     *
     * @param priority a int.
     */
    public void setPriority(int priority) {
        this.priority = priority;
    }

    /**
     * <p>Getter for the field <code>value</code>.</p>
     *
     * @return a int.
     */
    public int getValue() {
        return value;
    }

    /**
     * <p>Setter for the field <code>value</code>.</p>
     *
     * @param value a int.
     */
    public void setValue(int value) {
        this.value = value;
    }

    /**
     * <p>Getter for the field <code>digits</code>.</p>
     *
     * @return a int.
     */
    public int getDigits() {
        return digits;
    }

    /**
     * <p>Setter for the field <code>digits</code>.</p>
     *
     * @param digits a int.
     */
    public void setDigits(int digits) {
        this.digits = digits;
    }

    /** {@inheritDoc} */
    @Override
    public ScoreEntry clone() {
        return new ScoreEntry(name, priority, value, digits);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return Params.formatAsLine(
                "name", name,
                "priority", priority,
                "value", value,
                "digits", digits
        );
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        return other instanceof ScoreEntry && compareTo((ScoreEntry) other) == 0;
    }

    /** {@inheritDoc} */
    @Override
    public int compareTo(@Nonnull ScoreEntry scoreEntry) {
        int diff = priority - scoreEntry.priority;
        if (diff != 0) {
            return diff;
        }

        return value - scoreEntry.value;
    }
}
