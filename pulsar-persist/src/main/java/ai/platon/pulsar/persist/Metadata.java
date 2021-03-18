package ai.platon.pulsar.persist;

import ai.platon.pulsar.common.DateTimes;
import ai.platon.pulsar.persist.metadata.Name;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.gora.util.ByteUtils;

import javax.annotation.Nonnull;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * Created by vincent on 17-7-26.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 * <p>
 * Unstable fields are persisted as a metadata, they should be moved to a real database field if it's getting stable,
 * in which case, the database schema should be changed
 *
 * @author vincent
 * @version $Id: $Id
 */
public class Metadata {

    private Map<CharSequence, ByteBuffer> data;

    private Metadata(Map<CharSequence, ByteBuffer> data) {
        this.data = data;
    }

    /**
     * <p>box.</p>
     *
     * @param data a {@link java.util.Map} object.
     * @return a {@link ai.platon.pulsar.persist.Metadata} object.
     */
    @Nonnull
    public static Metadata box(@Nonnull Map<CharSequence, ByteBuffer> data) {
        return new Metadata(data);
    }

    /**
     * <p>unbox.</p>
     *
     * @return a {@link java.util.Map} object.
     */
    public Map<CharSequence, ByteBuffer> unbox() {
        return data;
    }

    /**
     * <p>set.</p>
     *
     * @param name a {@link ai.platon.pulsar.persist.metadata.Name} object.
     * @param value a {@link java.lang.String} object.
     */
    public void set(Name name, String value) {
        set(name.text(), value);
    }

    /**
     * <p>set.</p>
     *
     * @param key a {@link java.lang.String} object.
     * @param value a {@link java.lang.String} object.
     */
    public void set(String key, String value) {
        data.put(WebPage.u8(key), value == null ? null : ByteBuffer.wrap(value.getBytes()));
    }

    /**
     * <p>set.</p>
     *
     * @param name a {@link ai.platon.pulsar.persist.metadata.Name} object.
     * @param value a int.
     */
    public void set(Name name, int value) {
        set(name, String.valueOf(value));
    }

    /**
     * <p>set.</p>
     *
     * @param name a {@link ai.platon.pulsar.persist.metadata.Name} object.
     * @param value a long.
     */
    public void set(Name name, long value) {
        set(name, String.valueOf(value));
    }

    /**
     * <p>set.</p>
     *
     * @param name a {@link ai.platon.pulsar.persist.metadata.Name} object.
     * @param value a {@link java.time.Instant} object.
     */
    public void set(Name name, Instant value) {
        set(name, DateTimes.isoInstantFormat(value));
    }

    /**
     * <p>putAll.</p>
     *
     * @param data a {@link java.util.Map} object.
     */
    public void putAll(Map<String, String> data) {
        data.forEach(this::set);
    }

    /**
     * Copy All key-value pairs from properties.
     *
     * @param properties properties to copy from
     */
    public void putAll(Properties properties) {
        Enumeration<?> names = properties.propertyNames();
        while (names.hasMoreElements()) {
            String name = (String) names.nextElement();
            set(name, properties.getProperty(name));
        }
    }

    /**
     * <p>getByteBuffer.</p>
     *
     * @param name a {@link ai.platon.pulsar.persist.metadata.Name} object.
     * @return a {@link java.nio.ByteBuffer} object.
     */
    public ByteBuffer getByteBuffer(Name name) {
        return getByteBuffer(name.text());
    }

    /**
     * <p>getByteBuffer.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @return a {@link java.nio.ByteBuffer} object.
     */
    public ByteBuffer getByteBuffer(String name) {
        return data.get(WebPage.u8(name));
    }

    /**
     * <p>get.</p>
     *
     * @param name a {@link ai.platon.pulsar.persist.metadata.Name} object.
     * @return a {@link java.lang.String} object.
     */
    public String get(Name name) {
        return get(name.text());
    }

    /**
     * <p>get.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public String get(String name) {
        ByteBuffer bvalue = getByteBuffer(name);
        return bvalue == null ? null : ByteUtils.toString(bvalue.array());
    }

    /**
     * <p>getOrDefault.</p>
     *
     * @param name a {@link ai.platon.pulsar.persist.metadata.Name} object.
     * @param defaultValue a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public String getOrDefault(Name name, String defaultValue) {
        String value = get(name);
        return value == null ? defaultValue : value;
    }

    /**
     * <p>getOrDefault.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @param defaultValue a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public String getOrDefault(String name, String defaultValue) {
        String value = get(name);
        return value == null ? defaultValue : value;
    }

    /**
     * <p>getInt.</p>
     *
     * @param name a {@link ai.platon.pulsar.persist.metadata.Name} object.
     * @param defaultValue a int.
     * @return a int.
     */
    public int getInt(Name name, int defaultValue) {
        String s = get(name.text());
        return NumberUtils.toInt(s, defaultValue);
    }

    /**
     * <p>getLong.</p>
     *
     * @param name a {@link ai.platon.pulsar.persist.metadata.Name} object.
     * @param defaultValue a long.
     * @return a long.
     */
    public long getLong(Name name, long defaultValue) {
        String s = get(name.text());
        return NumberUtils.toLong(s, defaultValue);
    }

    /**
     * <p>getFloat.</p>
     *
     * @param name a {@link ai.platon.pulsar.persist.metadata.Name} object.
     * @param defaultValue a float.
     * @return a float.
     */
    public float getFloat(Name name, float defaultValue) {
        String s = get(name.text());
        return NumberUtils.toFloat(s, defaultValue);
    }

    /**
     * <p>getBoolean.</p>
     *
     * @param name a {@link ai.platon.pulsar.persist.metadata.Name} object.
     * @param defaultValue a {@link java.lang.Boolean} object.
     * @return a boolean.
     */
    public boolean getBoolean(Name name, Boolean defaultValue) {
        String s = get(name);
        if (s == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(s);
    }

    /**
     * <p>getInstant.</p>
     *
     * @param name a {@link ai.platon.pulsar.persist.metadata.Name} object.
     * @param defaultValue a {@link java.time.Instant} object.
     * @return a {@link java.time.Instant} object.
     */
    public Instant getInstant(Name name, Instant defaultValue) {
        return DateTimes.parseInstant(get(name), defaultValue);
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
     * @param key a {@link java.lang.String} object.
     * @return a boolean.
     */
    public boolean contains(String key) {
        return getByteBuffer(key) != null;
    }

    /**
     * Remove a data and all its associated values.
     *
     * @param name data name to remove
     */
    public void remove(String name) {
        if (get(name) != null) {
            set(name, null);
        }
    }

    /**
     * <p>remove.</p>
     *
     * @param name a {@link ai.platon.pulsar.persist.metadata.Name} object.
     */
    public void remove(Name name) {
        remove(name.text());
    }

    /**
     * Remove all mappings from data.
     */
    public void clear() {
        data.clear();
    }

    /**
     * <p>clear.</p>
     *
     * @param prefix a {@link java.lang.String} object.
     */
    public void clear(String prefix) {
        data.keySet().stream()
                .filter(key -> key.toString().startsWith(prefix))
                .map(Object::toString)
                .forEach(this::remove);
    }

    /**
     * <p>asStringMap.</p>
     *
     * @return a {@link java.util.Map} object.
     */
    public Map<String, String> asStringMap() {
        return data.entrySet().stream()
                .filter(e -> e.getValue() != null && e.getValue().hasArray())
                .collect(Collectors.toMap(e -> e.getKey().toString(), e -> ByteUtils.toString(e.getValue().array())));
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object o) {
        return o instanceof Metadata && data.equals(((Metadata) o).data);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return asStringMap().toString();
    }
}
