package ai.platon.pulsar.common.config;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.conf.Configuration;

import javax.annotation.Nullable;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * <p>MutableConfig class.</p>
 *
 * @author vincent
 * @version $Id: $Id
 */
public class MutableConfig extends ImmutableConfig {

    /**
     * <p>Constructor for MutableConfig.</p>
     */
    public MutableConfig() {
        this(true);
    }

    /**
     * <p>Constructor for MutableConfig.</p>
     *
     * @param loadDefaultDesource a boolean.
     */
    public MutableConfig(boolean loadDefaultDesource) {
        super(loadDefaultDesource);
    }

    /**
     * TODO: copy on write
     *
     * @param conf a {@link ai.platon.pulsar.common.config.ImmutableConfig} object.
     */
    public MutableConfig(ImmutableConfig conf) {
        super(conf.unbox());
    }

    /**
     * Set the <code>value</code> of the <code>name</code> property. If
     * <code>name</code> is deprecated or there is a deprecated name associated to it,
     * it sets the value to both names. Name will be trimmed before put into
     * configuration.
     *
     * @param name  property name.
     * @param value property value.
     */
    public void set(String name, String value) {
        Objects.requireNonNull(name);

        unbox().set(name, value);
    }

    /**
     * <p>setIfNotNull.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @param value a {@link java.lang.String} object.
     */
    public void setIfNotNull(String name, String value) {
        if (name != null && value != null) {
            set(name, value);
        }
    }

    /**
     * <p>setIfNotEmpty.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @param value a {@link java.lang.String} object.
     */
    public void setIfNotEmpty(String name, String value) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(value);

        if (StringUtils.isNoneEmpty(name, value)) {
            set(name, value);
        }
    }

    /**
     * <p>getAndSet.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @param value a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    @Nullable
    public String getAndSet(String name, String value) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(value);

        String old = get(name);
        if (old != null) {
            this.set(name, value);
        }
        return old;
    }

    /**
     * <p>getAndUnset.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    @Nullable
    public String getAndUnset(String name) {
        Objects.requireNonNull(name);

        String old = get(name);
        if (old != null) {
            unset(name);
        }
        return old;
    }

    /**
     * Set the array of string values for the <code>name</code> property as
     * as comma delimited values.
     *
     * @param name   property name.
     * @param values The values
     */
    public void setStrings(String name, String... values) {
        unbox().setStrings(name, values);
    }

    /**
     * Set the value of the <code>name</code> property to an <code>int</code>.
     *
     * @param name  property name.
     * @param value <code>int</code> value of the property.
     */
    public void setInt(String name, int value) {
        set(name, Integer.toString(value));
    }

    /**
     * Set the value of the <code>name</code> property to a <code>long</code>.
     *
     * @param name  property name.
     * @param value <code>long</code> value of the property.
     */
    public void setLong(String name, long value) {
        set(name, Long.toString(value));
    }

    /**
     * Set the value of the <code>name</code> property to a <code>float</code>.
     *
     * @param name  property name.
     * @param value property value.
     */
    public void setFloat(String name, float value) {
        set(name, Float.toString(value));
    }

    /**
     * Set the value of the <code>name</code> property to a <code>double</code>.
     *
     * @param name  property name.
     * @param value property value.
     */
    public void setDouble(String name, double value) {
        set(name, Double.toString(value));
    }

    /**
     * Set the value of the <code>name</code> property to a <code>boolean</code>.
     *
     * @param name  property name.
     * @param value <code>boolean</code> value of the property.
     */
    public void setBoolean(String name, boolean value) {
        set(name, Boolean.toString(value));
    }

    /**
     * Set the given property, if it is currently unset.
     *
     * @param name  property name
     * @param value new value
     */
    public void setBooleanIfUnset(String name, boolean value) {
        unbox().setIfUnset(name, Boolean.toString(value));
    }

    /**
     * Set the value of the <code>name</code> property to the given type. This
     * is equivalent to <code>set(&lt;name&gt;, value.toString())</code>.
     *
     * @param name  property name
     * @param value new value
     * @param <T> a T object.
     */
    public <T extends Enum<T>> void setEnum(String name, T value) {
        set(name, value.toString());
    }

    /**
     * <p>setInstant.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @param time a {@link java.time.Instant} object.
     */
    public void setInstant(String name, Instant time) {
        set(name, time.toString());
    }

    /**
     * <p>setDuration.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @param duration a {@link java.time.Duration} object.
     */
    public void setDuration(String name, Duration duration) {
        set(name, duration.toString());
    }

    /**
     * <p>unset.</p>
     *
     * @param name a {@link java.lang.String} object.
     */
    public void unset(String name) {
        unbox().unset(name);
    }

    /**
     * <p>clear.</p>
     */
    public void clear() {
        unbox().clear();
    }

    /**
     * <p>reset.</p>
     *
     * @param conf a {@link org.apache.hadoop.conf.Configuration} object.
     */
    public void reset(Configuration conf) {
        for (Map.Entry<String, String> prop : unbox()) {
            unset(prop.getKey());
        }

        for (Map.Entry<String, String> prop : conf) {
            set(prop.getKey(), prop.getValue());
        }
    }

    /**
     * <p>merge.</p>
     *
     * @param conf a {@link org.apache.hadoop.conf.Configuration} object.
     * @param names a {@link java.lang.String} object.
     */
    public void merge(Configuration conf, String... names) {
        for (Map.Entry<String, String> prop : conf) {
            String key = prop.getKey();
            if (names.length == 0 || ArrayUtils.contains(names, key)) {
                set(key, prop.getValue());
            }
        }
    }

    /**
     * <p>toVolatileConfig.</p>
     *
     * @return a {@link ai.platon.pulsar.common.config.VolatileConfig} object.
     */
    public VolatileConfig toVolatileConfig() {
        return new VolatileConfig(this);
    }
}
