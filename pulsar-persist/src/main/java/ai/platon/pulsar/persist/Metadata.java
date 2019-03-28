package ai.platon.pulsar.persist;

import ai.platon.pulsar.common.DateTimeUtil;
import ai.platon.pulsar.persist.metadata.Name;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.hadoop.hbase.util.Bytes;

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
 */
public class Metadata {

    private Map<CharSequence, ByteBuffer> data;

    private Metadata(Map<CharSequence, ByteBuffer> data) {
        this.data = data;
    }

    @Nonnull
    public static Metadata box(@Nonnull Map<CharSequence, ByteBuffer> data) {
        return new Metadata(data);
    }

    public Map<CharSequence, ByteBuffer> unbox() {
        return data;
    }

    public void set(Name name, String value) {
        set(name.text(), value);
    }

    public void set(String key, String value) {
        data.put(WebPage.u8(key), value == null ? null : ByteBuffer.wrap(value.getBytes()));
    }

    public void set(Name name, int value) {
        set(name, String.valueOf(value));
    }

    public void set(Name name, long value) {
        set(name, String.valueOf(value));
    }

    public void set(Name name, Instant value) {
        set(name, DateTimeUtil.isoInstantFormat(value));
    }

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

    public ByteBuffer getByteBuffer(Name name) {
        return getByteBuffer(name.text());
    }

    public ByteBuffer getByteBuffer(String name) {
        return data.get(WebPage.u8(name));
    }

    public String get(Name name) {
        return get(name.text());
    }

    public String get(String name) {
        ByteBuffer bvalue = getByteBuffer(name);
        return bvalue == null ? null : Bytes.toString(bvalue.array());
    }

    public String getOrDefault(Name name, String defaultValue) {
        String value = get(name);
        return value == null ? defaultValue : value;
    }

    public String getOrDefault(String name, String defaultValue) {
        String value = get(name);
        return value == null ? defaultValue : value;
    }

    public int getInt(Name name, int defaultValue) {
        String s = get(name.text());
        return NumberUtils.toInt(s, defaultValue);
    }

    public long getLong(Name name, long defaultValue) {
        String s = get(name.text());
        return NumberUtils.toLong(s, defaultValue);
    }

    public float getFloat(Name name, float defaultValue) {
        String s = get(name.text());
        return NumberUtils.toFloat(s, defaultValue);
    }

    public boolean getBoolean(Name name, Boolean defaultValue) {
        String s = get(name);
        if (s == null) {
            return defaultValue;
        }
        return Boolean.valueOf(s);
    }

    public Instant getInstant(Name name, Instant defaultValue) {
        return DateTimeUtil.parseInstant(get(name), defaultValue);
    }

    public boolean contains(Name name) {
        return contains(name.text());
    }

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

    public void remove(Name name) {
        remove(name.text());
    }

    /**
     * Remove all mappings from data.
     */
    public void clear() {
        data.clear();
    }

    public void clear(String prefix) {
        data.keySet().stream()
                .filter(key -> key.toString().startsWith(prefix))
                .map(Object::toString)
                .forEach(this::remove);
    }

    public Map<String, String> asStringMap() {
        return data.entrySet().stream()
                .filter(e -> e.getValue() != null && e.getValue().hasArray())
                .collect(Collectors.toMap(e -> e.getKey().toString(), e -> Bytes.toString(e.getValue().array())));
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof Metadata)) {
            return false;
        }

        Metadata other;
        try {
            other = (Metadata) o;
        } catch (ClassCastException e) {
            return false;
        }

        return data.equals(other.data);
    }

    @Override
    public String toString() {
        return asStringMap().toString();
    }
}
