package ai.platon.pulsar.common.config

import ai.platon.pulsar.common.Strings
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

/**
 * Configuration is a set of key/value pairs. Keys are always strings, values can be any type.
 *
 * @property loadDefaults Whether to load the default resources.
 * */
class MultiSourceProperties(
    private val loadDefaults: Boolean = true,
) : Iterable<Map.Entry<String, String?>> {

    companion object {
        private val ID_SUPPLIER = AtomicInteger()
    }

    private var volatileProperties = Properties()

    private var globalLoadedProperties: LocalResourceProperties? = null
    private val loadedProperties: LocalResourceProperties
        get() {
            synchronized(MultiSourceProperties::class.java) {
                if (globalLoadedProperties == null) {
                    globalLoadedProperties = LocalResourceProperties(loadDefaults).also { it.load() }
                }
                return globalLoadedProperties!!
            }
        }

    val id = ID_SUPPLIER.incrementAndGet()

    constructor(props: MultiSourceProperties) : this(props.loadDefaults)

    /**
     * @param name property name.
     * @param value property value.
     */
    operator fun set(name: String, value: String?) {
        if (value == null) {
            unset(name)
        } else {
            volatileProperties[name] = value
        }
    }

    fun unset(name: String) {
        val removed = volatileProperties.remove(name)
        loadedProperties.remove(name)
    }

    operator fun get(name: String): String? {
        return getVolatile(name) ?: getPermanent(name)
    }

    fun getVolatile(name: String): String? {
        return volatileProperties[name]?.toString()
    }

    fun getPermanent(name: String): String? {
        return loadedProperties[name]?.toString()
    }

    fun get(name: String, defaultValue: String): String {
        return get(name) ?: defaultValue
    }

    fun setStrings(name: String, vararg values: String) {
        set(name, Strings.arrayToString(values))
    }

    fun setIfUnset(name: String, value: String?) {
        if (get(name) == null) {
            set(name, value)
        }
    }

    /**
     * Return the number of keys in the configuration.
     *
     * @return number of keys in the configuration.
     */
    fun size() = volatileProperties.size + loadedProperties.size()

    /**
     * Clears all keys from the configuration.
     */
    fun clear() {
        volatileProperties.clear()
        globalLoadedProperties = null
    }

    @Synchronized
    fun reload() {
        volatileProperties = Properties()
        globalLoadedProperties = null // trigger reload
    }

    override fun iterator(): Iterator<Map.Entry<String, String>> {
        // Get a copy of just the string to string pairs. After the old object
        // methods that allow non-strings to be put into configurations are removed,
        // we could replace properties with a Map<String,String> and get rid of this
        // code.
        val result: MutableMap<String, String> = HashMap()

        for ((key, value) in loadedProperties.properties) {
            if (key is String) {
                result[key] = value.toString()
            }
        }

        for ((key, value) in volatileProperties) {
            if (key is String) {
                result[key] = value.toString()
            }
        }

        return result.entries.iterator()
    }

    override fun toString() = loadedProperties.toString()
}
