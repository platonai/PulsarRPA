package ai.platon.pulsar.persist;

import ai.platon.pulsar.persist.metadata.Name;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by vincent on 17-7-26.
 * Copyright @ 2013-2017 Platon AI. All rights reserved.
 *
 * @author vincent
 * @version $Id: $Id
 */
public class Variables {

    /**
     * Temporary variables, all temporary fields will not persist to storage.
     */
    private Map<String, Object> variables = new ConcurrentHashMap<>();

    public Map<String, Object> getVariables() {
        return variables;
    }

    public Object get(Name name) {
        return get(name.text());
    }

    public Object get(String name) {
        return variables.get(name);
    }

    public <T> T get(Name name, T defaultValue) {
        return get(name.text(), defaultValue);
    }

    public <T> T get(String name, T defaultValue) {
        Object o = variables.get(name);
        return o == null ? defaultValue : (T) o;
    }

    public String getString(String name) {
        return getString(name, "");
    }

    public String getString(String name, String defaultValue) {
        Object value = variables.get(name);
        return value == null ? defaultValue : value.toString();
    }

    public boolean contains(Name name) {
        return contains(name.text());
    }

    public boolean contains(String name) {
        return variables.containsKey(name);
    }

    public void set(Name name, Object value) {
        set(name.text(), value);
    }

    public void set(String name, Object value) {
        variables.put(name, value);
    }

    public Object remove(Name name) {
        return remove(name.text());
    }

    public Object remove(String name) {
        return variables.remove(name);
    }
}
