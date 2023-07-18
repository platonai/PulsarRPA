package ai.platon.pulsar.persist;

import ai.platon.pulsar.common.DateTimes;
import ai.platon.pulsar.persist.metadata.Name;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.gora.util.ByteUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Enumeration;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * Created by vincent on 17-7-26.
 * Copyright @ 2013-2023 Platon AI. All rights reserved
 * <p>
 * Unstable fields are persisted as a metadata, they should be moved to a real database field if it's getting stable,
 * in which case, the database schema should be changed
 * </p>
 *
 * @author vincent, ivincent.zhang@gmail.com
 */
public class Metadata {

    private final Map<CharSequence, ByteBuffer> data;

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
        set(name, DateTimes.isoInstantFormat(value));
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

    @Nullable
    public ByteBuffer getByteBuffer(String name) {
        return data.get(WebPage.u8(name));
    }

    @Nullable
    public String get(Name name) {
        return get(name.text());
    }

    @Nullable
    public String get(String name) {
        ByteBuffer bvalue = getByteBuffer(name);
        return bvalue == null ? null : ByteUtils.toString(bvalue.array());
    }

    @Nonnull
    public Optional<String> getOptional(String name) {
        return Optional.ofNullable(get(name));
    }

    @Nonnull
    public Optional<String> getOptional(Name name) {
        return Optional.ofNullable(get(name));
    }

    @Nonnull
    public String getOrDefault(Name name, String defaultValue) {
        String value = get(name);
        return value == null ? defaultValue : value;
    }

    @Nonnull
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
        return Boolean.parseBoolean(s);
    }

    @Nonnull
    public Instant getInstant(Name name, Instant defaultValue) {
        String value = get(name);
        return value == null ? defaultValue : DateTimes.parseInstant(value, defaultValue);
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
                .collect(Collectors.toMap(e -> e.getKey().toString(), e -> ByteUtils.toString(e.getValue().array())));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        return o instanceof Metadata && data.equals(((Metadata) o).data);
    }

    @Override
    public String toString() {
        return asStringMap().toString();
    }
}
