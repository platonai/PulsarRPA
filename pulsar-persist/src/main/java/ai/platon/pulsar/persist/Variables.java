package ai.platon.pulsar.persist;

import ai.platon.pulsar.persist.metadata.Name;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by vincent on 17-7-26.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 *
 * @author vincent
 * @version $Id: $Id
 */
public class Variables {

    /**
     * Temporary variables used in the process stream, all temporary fields will not persist to storage
     */
    private Map<String, Object> variables = new HashMap<>();

    /**
     * <p>Getter for the field <code>variables</code>.</p>
     *
     * @return a {@link java.util.Map} object.
     */
    public Map<String, Object> getVariables() {
        return variables;
    }

    /**
     * <p>get.</p>
     *
     * @param name a {@link ai.platon.pulsar.persist.metadata.Name} object.
     * @return a {@link java.lang.Object} object.
     */
    public Object get(Name name) {
        return get(name.text());
    }

    /**
     * <p>get.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @return a {@link java.lang.Object} object.
     */
    public Object get(String name) {
        return variables.get(name);
    }

    /**
     * <p>get.</p>
     *
     * @param name a {@link ai.platon.pulsar.persist.metadata.Name} object.
     * @param defaultValue a T object.
     * @param <T> a T object.
     * @return a T object.
     */
    public <T> T get(Name name, T defaultValue) {
        return get(name.text(), defaultValue);
    }

    /**
     * <p>get.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @param defaultValue a T object.
     * @param <T> a T object.
     * @return a T object.
     */
    public <T> T get(String name, T defaultValue) {
        Object o = variables.get(name);
        return o == null ? defaultValue : (T) o;
    }

    /**
     * <p>getString.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public String getString(String name) {
        return getString(name, "");
    }

    /**
     * <p>getString.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @param defaultValue a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public String getString(String name, String defaultValue) {
        Object value = variables.get(name);
        return value == null ? defaultValue : value.toString();
    }

    /**
     * <p>contains.</p>
     *
     * @param name a {@link ai.platon.pulsar.persist.metadata.Name} object.
     * @return a boolean.
     */
    public boolean contains(Name name) {
        return contains(name.text());
    }

    /**
     * <p>contains.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @return a boolean.
     */
    public boolean contains(String name) {
        return variables.containsKey(name);
    }

    /**
     * <p>set.</p>
     *
     * @param name a {@link ai.platon.pulsar.persist.metadata.Name} object.
     * @param value a {@link java.lang.Object} object.
     */
    public void set(Name name, Object value) {
        set(name.text(), value);
    }

    /**
     * <p>set.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @param value a {@link java.lang.Object} object.
     */
    public void set(String name, Object value) {
        variables.put(name, value);
    }

    /**
     * <p>remove.</p>
     *
     * @param name a {@link ai.platon.pulsar.persist.metadata.Name} object.
     * @return a {@link java.lang.Object} object.
     */
    public Object remove(Name name) {
        return remove(name.text());
    }

    /**
     * <p>remove.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @return a {@link java.lang.Object} object.
     */
    public Object remove(String name) {
        return variables.remove(name);
    }
}
