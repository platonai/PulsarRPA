package ai.platon.pulsar.common.config

import org.apache.commons.lang3.ArrayUtils
import org.apache.commons.lang3.StringUtils
import java.lang.Boolean.toString
import java.lang.Integer.toString
import java.time.Duration
import java.time.Instant

/**
 *
 * MutableConfig class.
 *
 * @author vincent
 * @version $Id: $Id
 */
open class MutableConfig : ImmutableConfig {

    constructor(): this(false)

    constructor(profile: String): this(profile, true, listOf())

    constructor(loadDefaults: Boolean): this(
        System.getProperty(CapabilityTypes.PROFILE_KEY, ""),
        loadDefaults, listOf()
    )

    constructor(
        profile: String,
        loadDefaults: Boolean,
        resources: Iterable<String> = mutableSetOf()
    ): super(profile, loadDefaults, resources)

    constructor(conf: ImmutableConfig) : super(conf.unbox()) {
        this.environment = conf.environment
    }

    /**
     * Set the `value` of the `name` property. If
     * `name` is deprecated or there is a deprecated name associated to it,
     * it sets the value to both names. Name will be trimmed before put into
     * configuration.
     *
     * @param name  property name.
     * @param value property value.
     */
    operator fun set(name: String, value: String?) {
        localFileConfiguration[name] = value
    }

    /**
     *
     * setIfNotNull.
     *
     * @param name a [java.lang.String] object.
     * @param value a [java.lang.String] object.
     */
    fun setIfNotNull(name: String?, value: String?) {
        if (name != null && value != null) {
            set(name, value)
        }
    }

    /**
     *
     * setIfNotEmpty.
     *
     * @param name a [java.lang.String] object.
     * @param value a [java.lang.String] object.
     */
    fun setIfNotEmpty(name: String, value: String) {
        if (StringUtils.isNoneEmpty(name, value)) {
            set(name, value)
        }
    }

    /**
     *
     * getAndSet.
     *
     * @param name a [java.lang.String] object.
     * @param value a [java.lang.String] object.
     * @return a [java.lang.String] object.
     */
    fun getAndSet(name: String, value: String): String? {
        val old = get(name)
        if (old != null) {
            this[name] = value
        }
        return old
    }

    /**
     *
     * getAndUnset.
     *
     * @param name a [java.lang.String] object.
     * @return a [java.lang.String] object.
     */
    fun getAndUnset(name: String): String? {
        val old = get(name)
        if (old != null) {
            unset(name)
        }
        return old
    }

    /**
     * Set the array of string values for the `name` property as
     * as comma delimited values.
     *
     * @param name   property name.
     * @param values The values
     */
    fun setStrings(name: String?, vararg values: String) {
        localFileConfiguration.setStrings(name!!, *values)
    }

    /**
     * Set the value of the `name` property to an `int`.
     *
     * @param name  property name.
     * @param value `int` value of the property.
     */
    fun setInt(name: String, value: Int) {
        set(name, toString(value))
    }

    /**
     * Set the value of the `name` property to a `long`.
     *
     * @param name  property name.
     * @param value `long` value of the property.
     */
    fun setLong(name: String, value: Long) {
        set(name, value.toString())
    }

    /**
     * Set the value of the `name` property to a `float`.
     *
     * @param name  property name.
     * @param value property value.
     */
    fun setFloat(name: String, value: Float) {
        set(name, value.toString())
    }

    /**
     * Set the value of the `name` property to a `double`.
     *
     * @param name  property name.
     * @param value property value.
     */
    fun setDouble(name: String, value: Double) {
        set(name, value.toString())
    }

    /**
     * Set the value of the `name` property to a `boolean`.
     *
     * @param name  property name.
     * @param value `boolean` value of the property.
     */
    fun setBoolean(name: String, value: Boolean) {
        set(name, toString(value))
    }

    /**
     * Set the given property, if it is currently unset.
     *
     * @param name  property name
     * @param value new value
     */
    fun setBooleanIfUnset(name: String, value: Boolean) {
        unbox().setIfUnset(name, toString(value))
    }

    /**
     * Set the value of the `name` property to the given type. This
     * is equivalent to `set(<name>, value.toString())`.
     *
     * @param name  property name
     * @param value new value
     * @param <T> a T object.
    </T> */
    fun <T : Enum<T>> setEnum(name: String, value: T) {
        set(name, value.name)
    }

    /**
     *
     * setInstant.
     *
     * @param name a [java.lang.String] object.
     * @param time a [java.time.Instant] object.
     */
    fun setInstant(name: String, time: Instant) {
        set(name, time.toString())
    }

    /**
     *
     * setDuration.
     *
     * @param name a [java.lang.String] object.
     * @param duration a [java.time.Duration] object.
     */
    fun setDuration(name: String, duration: Duration) {
        set(name, duration.toString())
    }

    /**
     *
     * unset.
     *
     * @param name a [java.lang.String] object.
     */
    fun unset(name: String) {
        localFileConfiguration.unset(name)
    }

    /**
     *
     * clear.
     */
    fun clear() {
        localFileConfiguration.clear()
    }

    /**
     *
     * reset.
     */
    fun reset(conf: LocalFileConfiguration) {
        for ((key) in conf) {
            unset(key)
        }
        for ((key, value) in conf) {
            set(key, value)
        }
    }

    /**
     *
     * merge.
     *
     * @param names a [java.lang.String] object.
     */
    fun merge(conf: LocalFileConfiguration, vararg names: String?) {
        for ((key, value) in conf) {
            if (names.isEmpty() || ArrayUtils.contains(names, key)) {
                set(key, value)
            }
        }
    }

    /**
     *
     * toVolatileConfig.
     *
     * @return a [ai.platon.pulsar.common.config.VolatileConfig] object.
     */
    override fun toVolatileConfig(): VolatileConfig {
        return VolatileConfig(this)
    }
}
