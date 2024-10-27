package ai.platon.pulsar.common.config

import ai.platon.pulsar.common.SParser
import ai.platon.pulsar.common.config.KConfiguration.Companion.DEFAULT_RESOURCES
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment
import org.springframework.core.env.get
import java.io.InputStream
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import java.util.*

/**
 * Created by vincent on 17-1-17.
 * Copyright @ 2013-2023 Platon AI. All rights reserved.
 *
 * @author vincent
 */
abstract class AbstractConfiguration {
    protected val logger = LoggerFactory.getLogger(AbstractConfiguration::class.java)

    private val resources = LinkedHashSet<String>()
    var name = "Configuration#" + hashCode()
    var profile = ""
        private set
    val mode get() = if (isDistributedFs) "cluster" else "local"

    /**
     * Hadoop compatible configuration.
     */
    protected val conf: KConfiguration

    /**
     * Spring core is the first class dependency now.
     */
    var environment: Environment? = null
    
    constructor(
        profile: String = System.getProperty(CapabilityTypes.PROFILE_KEY, ""),
        loadDefaults: Boolean = true,
        resources: Iterable<String> = DEFAULT_RESOURCES
    ) {
        conf = KConfiguration(profile = profile, extraResources = resources, loadDefaults = loadDefaults)
    }
    
    constructor(conf: KConfiguration) {
        this.conf = KConfiguration(conf)
    }
    
    /**
     * Check if we are running on hdfs.
     *
     * @return a boolean.
     */
    private val isDistributedFs: Boolean
        get() = get("fs.defaultFS")?.startsWith("hdfs") == true

    /**
     * Return the boxed KConfiguration.
     */
    fun unbox() = conf

    /**
     * The configured item size.
     */
    fun size() = conf.size()

    /**
     * Get the value of the `name` property, `null` if no such property exists.
     *
     * @param name the property name, will be trimmed before get value.
     * @return the value of the `name`, or null if no such property exists.
     */
    open operator fun get(name: String): String? {
        return System.getenv(name) ?: System.getProperty(name) ?: environment?.get(name) ?: conf[name]
    }

    /**
     * Get the value of the `name`. If the key is deprecated,
     * it returns the value of the first key which replaces the deprecated key and is not null.
     * If no such property exists, then `defaultValue` is returned.
     *
     * @param name         property name, will be trimmed before get value.
     * @param defaultValue default value.
     * @return property value, or `defaultValue` if the property doesn't exist.
     */
    open operator fun get(name: String, defaultValue: String) = get(name) ?: defaultValue

    /**
     * Get the value of the `name` property as an `int`.
     *
     * If no such property exists, the provided default value is returned, or if the specified value is not a valid `int`,
     * then an error is thrown.
     *
     * @param name         property name.
     * @param defaultValue default value.
     * @return property value as an `int`, or `defaultValue`.
     * @throws java.lang.NumberFormatException when the value is invalid
     */
    fun getInt(name: String, defaultValue: Int) = p(name).getInt(defaultValue)

    /**
     * Get the value of the `name` property as a set of comma-delimited `int` values.
     *
     * If no such property exists, an empty array is returned.
     *
     * @param name property name
     * @return property value interpreted as an array of comma-delimited `int` values
     */
    fun getInts(name: String) = p(name).ints

    /**
     * Get the value of the `name` property as a `long`.
     * If no such property exists, the provided default value is returned,
     * or if the specified value is not a valid `long`,
     * then an error is thrown.
     *
     * @param name         property name.
     * @param defaultValue default value.
     * @return property value as a `long`, or `defaultValue`.
     * @throws java.lang.NumberFormatException when the value is invalid
     */
    fun getLong(name: String, defaultValue: Long) = p(name).getLong(defaultValue)

    /**
     * Get the value of the `name` property as a `float`.
     * If no such property exists, the provided default value is returned,
     * or if the specified value is not a valid `float`,
     * then an error is thrown.
     *
     * @param name         property name.
     * @param defaultValue default value.
     * @return property value as a `float`, or `defaultValue`.
     * @throws java.lang.NumberFormatException when the value is invalid
     */
    fun getFloat(name: String, defaultValue: Float) = p(name).getFloat(defaultValue)

    /**
     * Get the value of the `name` property as a `double`.
     * If no such property exists, the provided default value is returned,
     * or if the specified value is not a valid `double`,
     * then an error is thrown.
     *
     * @param name         property name.
     * @param defaultValue default value.
     * @return property value as a `double`,
     * or `defaultValue`.
     * @throws java.lang.NumberFormatException when the value is invalid
     */
    fun getDouble(name: String, defaultValue: Double) = p(name).getDouble(defaultValue)

    /**
     * Get the value of the `name` property as a `boolean`.
     * If no such property is specified, or if the specified value is not a valid
     * `boolean`, then `defaultValue` is returned.
     *
     * @param name         property name.
     * @param defaultValue default value.
     * @return property value as a `boolean`,
     * or `defaultValue`.
     */
    fun getBoolean(name: String, defaultValue: Boolean) = p(name).getBoolean(defaultValue)

    /**
     * Return value matching this enumerated type.
     *
     * @param name         Property name
     * @param defaultValue Value returned if no mapping exists
     * @throws java.lang.IllegalArgumentException If mapping is illegal for the type
     * provided
     * @param <T> a T object.
     * @return a T object.
    </T> */
    fun <T : Enum<T>?> getEnum(name: String, defaultValue: T) = p(name).getEnum(defaultValue)

    /**
     * Get the comma delimited values of the `name` property as
     * a collection of `String`s.
     * If no such property is specified then empty collection is returned.
     *
     *
     * This is an optimized version of [.getStrings]
     *
     * @param name property name.
     * @return property value as a collection of `String`s.
     */
    fun getStringCollection(name: String) = p(name).stringCollection

    /**
     * Get the comma delimited values of the `name` property as
     * an array of `String`s.
     * If no such property is specified then `null` is returned.
     *
     * @param name property name.
     * @return property value as an array of `String`s,
     * or `null`.
     */
    fun getStrings(name: String) = p(name).strings ?: arrayOf()

    /**
     * Get the comma delimited values of the `name` property as
     * an array of `String`s.
     * If no such property is specified then default value is returned.
     *
     * @param name         property name.
     * @param defaultValue The default value
     * @return property value as an array of `String`s,
     * or default value.
     */
    fun getStrings(name: String, vararg defaultValue: String) = p(name).getStrings(*defaultValue)

    /**
     * Get the comma delimited values of the `name` property as
     * a collection of `String`s, trimmed of the leading and trailing whitespace.
     * If no such property is specified then empty `Collection` is returned.
     *
     * @param name property name.
     * @return property value as a collection of `String`s, or empty `Collection`
     */
    fun getTrimmedStringCollection(name: String) = p(name).trimmedStringCollection

    /**
     * Get the comma delimited values of the `name` property as
     * an array of `String`s, trimmed of the leading and trailing whitespace.
     * If no such property is specified then an empty array is returned.
     *
     * @param name property name.
     * @return property value as an array of trimmed `String`s,
     * or empty array.
     */
    fun getTrimmedStrings(name: String) = p(name).trimmedStrings

    /**
     * Get the comma delimited values of the `name` property as
     * an array of `String`s, trimmed of the leading and trailing whitespace.
     * If no such property is specified then default value is returned.
     *
     * @param name         property name.
     * @param defaultValue The default value
     * @return property value as an array of trimmed `String`s,
     * or default value.
     */
    fun getTrimmedStrings(name: String, vararg defaultValue: String): Array<String> {
        return p(name).getTrimmedStrings(*defaultValue)
    }

    /**
     * Get an unsigned integer, if the configured value is negative or not set, return the default value
     *
     * @param name         The property name
     * @param defaultValue The default value return if the configured value is negative
     * @return a positive integer
     */
    fun getUint(name: String, defaultValue: Int): Int {
        var value = getInt(name, defaultValue)
        if (value < 0) {
            value = defaultValue
        }
        return value
    }

    /**
     * Get a unsigned long integer, if the configured value is negative, return the default value
     *
     * @param name         The property name
     * @param defaultValue The default value return if the configured value is negative
     * @return a positive long integer
     */
    fun getUlong(name: String, defaultValue: Long): Long {
        var value = getLong(name, defaultValue)
        if (value < 0) {
            value = defaultValue
        }
        return value
    }

    /**
     * Support both ISO-8601 standard and hadoop time duration format
     * ISO-8601 standard : PnDTnHnMn.nS
     * Hadoop time duration format : Valid units are : ns, us, ms, s, m, h, d.
     *
     * @param name a [java.lang.String] object.
     * @return a [java.time.Duration] object.
     */
    fun getDuration(name: String) = p(name).duration

    /**
     *
     * getDuration.
     *
     * @param name a [java.lang.String] object.
     * @param defaultValue a [java.time.Duration] object.
     * @return a [java.time.Duration] object.
     */
    fun getDuration(name: String, defaultValue: Duration) = p(name).getDuration(defaultValue)

    /**
     *
     * getInstant.
     *
     * @param name a [java.lang.String] object.
     * @param defaultValue a [java.time.Instant] object.
     * @return a [java.time.Instant] object.
     */
    fun getInstant(name: String, defaultValue: Instant) = p(name).getInstant(defaultValue)

    /**
     *
     * getPath.
     *
     * @param name a [java.lang.String] object.
     * @param elsePath a [java.nio.file.Path] object.
     * @return a [java.nio.file.Path] object.
     */
    fun getPath(name: String, elsePath: Path) = p(name).getPath(elsePath)

    /**
     *
     * getPathOrNull.
     *
     * @param name a [java.lang.String] object.
     * @return a [java.nio.file.Path] object.
     */
    fun getPathOrNull(name: String) = p(name).pathOrNull

    /**
     *
     * getKvs.
     *
     * @param name a [java.lang.String] object.
     * @return a [java.util.Map] object.
     */
    fun getKvs(name: String) = p(name).kvs

    /**
     *
     * getConfResourceAsInputStream.
     *
     * @param resource a [java.lang.String] object.
     * @return a [java.io.InputStream] object.
     */
    fun getConfResourceAsInputStream(resource: String): InputStream {
        return SParser.wrap(resource).resourceAsInputStream
    }

    /**
     *
     * getConfResourceAsReader.
     *
     * @param resource a [java.lang.String] object.
     * @return a [java.io.Reader] object.
     */
    fun getConfResourceAsReader(resource: String) = SParser.wrap(resource).resourceAsReader

    /**
     *
     * getResource.
     *
     * @param resource a [java.lang.String] object.
     * @return a [java.net.URL] object.
     */
    fun getResource(resource: String) = SParser.wrap(resource).resource

    /**
     * Get the value of the `name` property as a `Class`.
     * If no such property is specified, then `defaultValue` is
     * returned.
     *
     * @param name         the property name of class.
     * @param defaultValue default value.
     * @return property value as a `Class`,
     * or `defaultValue`.
     */
    fun getClass(name: String, defaultValue: Class<*>): Class<*> {
        return p(name).getClass(defaultValue)
    }

    /**
     * Get the value of the `name` property as a `Class`
     * implementing the interface specified by `xface`.
     *
     * If no such property is specified, then `defaultValue` is
     * returned.
     *
     * An exception is thrown if the returned class does not implement the named
     * interface.
     *
     * @param name         the property name of class.
     * @param defaultValue default value.
     * @param xface        the interface implemented by the named class.
     * @return property value as a `Class`,
     * or `defaultValue`.
     * @param <U> a U object.
    </U> */
    fun <U> getClass(name: String, defaultValue: Class<out U>, xface: Class<U>): Class<out U> {
        return p(name).getClass(defaultValue, xface)
    }

    private fun p(name: String) = SParser(get(name))

    override fun toString() = "profile: <$profile> | $conf"
}
