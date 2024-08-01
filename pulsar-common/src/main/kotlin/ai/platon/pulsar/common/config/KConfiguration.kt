package ai.platon.pulsar.common.config

import ai.platon.pulsar.common.ResourceLoader
import ai.platon.pulsar.common.SParser
import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.stringify
import com.ctc.wstx.io.StreamBootstrapper
import com.ctc.wstx.io.SystemId
import org.codehaus.stax2.XMLStreamReader2
import org.slf4j.LoggerFactory
import java.io.BufferedInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.JarURLConnection
import java.net.URI
import java.net.URISyntaxException
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import javax.xml.stream.XMLStreamConstants
import javax.xml.stream.XMLStreamException
import javax.xml.stream.XMLStreamReader

class KConfiguration(
    private val loadDefaults: Boolean = true
) : Iterable<Map.Entry<String, String?>> {

    companion object {
        private val logger = LoggerFactory.getLogger(KConfiguration::class.java)
        
        // The resources that are loaded by default. The resources are hadoop compatible.
        const val APPLICATION_SPECIFIED_RESOURCES = "pulsar-default.xml"
        val DEFAULT_RESOURCES = LinkedHashSet<String>()
        private val FULL_PATH_RESOURCES = LinkedHashSet<URL>()
        
        private val defaultResources = CopyOnWriteArrayList<String>()
    }
    
    private var rawResources = mutableSetOf<String>()
    private var resources = ArrayList<Resource>()
    private var properties: Properties? = null
    private val wstxInputFactory = com.ctc.wstx.stax.WstxInputFactory()

    val loadedResources get() = resources.map { it.name }

    constructor(conf: KConfiguration): this() {
        synchronized(conf) {
            conf.props
            this.resources = conf.resources.clone() as ArrayList<Resource>
            conf.properties?.let { properties = it.clone() as Properties }
        }
    }

    /**
     * Set the `value` of the `name` property. If
     * `name` is deprecated or there is a deprecated name associated to it,
     * it sets the value to both names. Name will be trimmed before put into
     * configuration.
     *
     * @param name property name.
     * @param value property value.
     */
    operator fun set(name: String, value: String?) {
        if (value == null) {
            unset(name)
        } else {
            props[name] = value
        }
    }

    fun unset(name: String) {
        props.remove(name)
    }

    operator fun get(name: String): String? {
        return props[name]?.toString()
    }

    fun get(name: String, defaultValue: String): String {
        return get(name) ?: defaultValue
    }

    fun setStrings(name: String, vararg values: String) {
        set(name, Strings.arrayToString(values))
    }

    @Synchronized
    fun setIfUnset(name: String?, value: String?) {
        if (get(name!!) == null) {
            set(name, value)
        }
    }

    @Synchronized
    fun reloadConfiguration() {
        properties = null // trigger reload
    }
    
    fun addLegacyResources(profile: String, mode: String, loadDefaults: Boolean, extraResources: Iterable<String>) {
        synchronized(FULL_PATH_RESOURCES) {
            findLegacyConfResources0(profile, mode, loadDefaults, extraResources)
        }
    }
    
    private fun findLegacyConfResources0(profile: String, mode: String, loadDefaults: Boolean, extraResources: Iterable<String>) {
        extraResources.toCollection(rawResources)
        if (!loadDefaults) {
            return
        }
        if (profile.isNotEmpty()) {
            set(CapabilityTypes.LEGACY_CONFIG_PROFILE, profile)
        }
        val specifiedResources =
            System.getProperty(CapabilityTypes.SYSTEM_PROPERTY_SPECIFIED_RESOURCES,
                APPLICATION_SPECIFIED_RESOURCES
            )
        specifiedResources.split(",".toRegex()).forEach { rawResources.add(it) }
        for (name in rawResources) {
            val realResource = findRealResource(profile, mode, name)
            if (realResource != null) {
                if (realResource !in FULL_PATH_RESOURCES) {
                    logger.info("Found legacy configuration: $realResource")
                    FULL_PATH_RESOURCES.add(realResource)
                }
            } else {
                logger.info("Resource not find: $name")
            }
        }
        
        FULL_PATH_RESOURCES.forEach { addResource(it) }
        // logger.info("legacy config profile: <$profile> | $conf")
    }
    
    private fun findRealResource(profile: String, mode: String, name: String): URL? {
        val prefix = "config/legacy"
        val suffix = "$mode/$name"
        val searchPaths = arrayOf(
            "$prefix/$suffix", "$prefix/$profile/$suffix",
            "$prefix/$name", "$prefix/$profile/$name",
            name
        ).map { it.replace("//", "/") }.distinct().sortedByDescending { it.length }

        return searchPaths.firstNotNullOfOrNull { SParser.wrap(it).resource }
    }

    @Synchronized
    private fun addResourceObject(resource: Resource) {
        resources.add(resource) // add to resources
        loadProps(properties, resources.size - 1, false)
    }

    @get:Synchronized
    protected val props: Properties
        get() {
            if (properties == null) {
                properties = Properties()
                loadProps(properties, 0, true)
            }
            return properties!!
        }

    /**
     * Loads the resource at a given index into the properties.
     *
     * @param props      the object containing the loaded properties.
     * @param startIdx   the index where the new resource has been added.
     * @param fullReload flag whether we do complete reload of the conf instead
     * of just loading the new resource.
     */
    @Synchronized
    private fun loadProps(props: Properties?, startIdx: Int, fullReload: Boolean) {
        props?.let { loadResources(it, resources, startIdx, fullReload) }
    }

    fun addResource(name: String) = addResourceObject(Resource(name))

    fun addResource(url: URL) = addResourceObject(Resource(url))

    /**
     * Return the number of keys in the configuration.
     *
     * @return number of keys in the configuration.
     */
    fun size() = props.size

    /**
     * Clears all keys from the configuration.
     */
    fun clear() {
        if (properties != null) {
            props.clear()
        }
    }

    override fun iterator(): MutableIterator<Map.Entry<String, String>> {
        // Get a copy of just the string to string pairs. After the old object
        // methods that allow non-strings to be put into configurations are removed,
        // we could replace properties with a Map<String,String> and get rid of this
        // code.
        val result: MutableMap<String, String> = HashMap()
        for ((key, value) in props) {
            if (key is String && value is String) {
                result[key] = value
            }
        }
        return result.entries.iterator()
    }

    override fun toString(): String {
        return resources.joinToString(", ", "[", "]") {
            it.name.substringAfterLast("/")
        }
    }

    private fun loadResources(
        properties: Properties, resources: ArrayList<Resource>, startIdx: Int, fullReload: Boolean
    ) {
        if (loadDefaults && fullReload) {
            defaultResources.forEach { loadResource(properties, Resource(it)) }
        }
        for (i in startIdx until resources.size) {
            loadResource(properties, resources[i])?.let { resources[i] = it }
        }
        // this.addTags(properties);
    }

    private fun overlay(to: Properties, from: Properties) {
        synchronized(from) {
            for ((key, value) in from) {
                to[key] = value
            }
        }
    }

    private fun loadResource(properties: Properties, wrapper: Resource): Resource? {
        return try {
            val resource = wrapper.resource
            val name = wrapper.name
            var returnCachedProperties = false
            if (resource is InputStream) {
                returnCachedProperties = true
            } else if (resource is Properties) {
                overlay(properties, resource)
            }

            val reader = getStreamReader(wrapper) ?: throw RuntimeException("$resource not found")
            var toAddTo = properties
            if (returnCachedProperties) {
                toAddTo = Properties()
            }

            val items: List<ParsedItem> = Parser(reader, wrapper, this).parse()
            for (item in items) {
                loadProperty(toAddTo, item.name, item.key, item.value, item.isFinal, item.sources)
            }
            reader.close()

            if (returnCachedProperties) {
                overlay(properties, toAddTo)
                return Resource(toAddTo, name)
            }

            null
        } catch (e: Exception) {
            logger.warn(e.stringify())
            throw RuntimeException(e)
        }
    }

    private fun loadProperty(
        properties: Properties, name: String, key: String,
        value: String?, finalParameter: Boolean, source: Array<String>?
    ) {
        if (value != null) {
            properties.setProperty(key, value)
        } else {
            properties.remove(key)
        }
//        var value: String? = value
//        if (value != null || allowNullValueProperties) {
//            if (!finalParameters.contains(attr)) {
//                properties.setProperty(attr, value)
//                source?.let { putIntoUpdatingResource(attr, it) }
//            } else {
//                // This is a final parameter so check for overrides.
//                checkForOverride(this.properties, name, attr, value)
//                if (this.properties !== properties) {
//                    checkForOverride(properties, name, attr, value)
//                }
//            }
//        }
//        if (finalParameter && attr != null) {
//            finalParameters.add(attr)
//        }
    }

    @Throws(XMLStreamException::class, IOException::class)
    internal fun getStreamReader(wrapper: Resource): XMLStreamReader2? {
        val resource = wrapper.resource
        return when (resource) {
            is URL -> parse(resource) as XMLStreamReader2?
            is String -> {
                // a CLASSPATH resource
                val url = ResourceLoader.getResource(resource) ?: return null
                parse(url)
            }
            is Path -> {
                // a file resource
                // Can't use FileSystem API or we get an infinite loop
                // since FileSystem uses Configuration API.  Use java.io.File instead.
                val file = File(resource.toUri().path).absoluteFile.takeIf { it.exists() } ?: return null
                parse(BufferedInputStream(Files.newInputStream(file.toPath())), resource.toString())
            }
            is InputStream -> parse(resource, null)
            else -> null
        } as XMLStreamReader2?
    }
    
    @Throws(IOException::class, XMLStreamException::class)
    private fun parse(url: URL): XMLStreamReader {
        val connection = url.openConnection()
        (connection as? JarURLConnection)?.useCaches = false
        return parse(connection.getInputStream(), url.toString())
    }

    @Throws(IOException::class, XMLStreamException::class)
    private fun parse(input: InputStream, systemId: String?): XMLStreamReader {
        val id = SystemId.construct(systemId)

        val readerConfig = wstxInputFactory.createPrivateConfig()

        val bootstrapper = StreamBootstrapper.getInstance(null, id, input)
        return wstxInputFactory.createSR(readerConfig, systemId, bootstrapper, false, true)
    }

    internal class Resource(val resource: Any, val name: String = resource.toString()) {
        override fun toString() = name
    }

    private class ParsedItem(
        var name: String, var key: String, var value: String?,
        var isFinal: Boolean, var sources: Array<String>
    )

    /**
     * Parser to consume SAX stream of XML elements from a Configuration.
     */
    private class Parser(
        val reader: XMLStreamReader2,
        val wrapper: Resource,
        val conf: KConfiguration
    ) {
        private val name: String = wrapper.name
        private val nameSingletonArray: Array<String> = arrayOf(name)
        private val token = StringBuilder()
        private var key: String? = null
        private var confValue: String? = null
        private var confInclude: String? = null
        private var confTag: String? = null
        private var confFinal = false
        private var parseToken = false
        private val confSource: MutableList<String> = ArrayList()
        private val results: MutableList<ParsedItem> = ArrayList<ParsedItem>()

        @Throws(IOException::class, XMLStreamException::class)
        fun parse(): List<ParsedItem> {
            while (reader.hasNext()) {
                parseNext()
            }
            return results
        }

        @Throws(XMLStreamException::class, IOException::class)
        private fun handleStartElement() {
            when (reader.localName) {
                "property" -> handleStartProperty()
                "name", "value", "final", "source", "tag" -> {
                    parseToken = true
                    token.setLength(0)
                }
                "include" -> handleInclude()
                "configuration" -> {
                }
                else -> {
                }
            }
        }

        private fun handleStartProperty() {
            key = null
            confValue = null
            confFinal = false
            confTag = null
            confSource.clear()

            // First test for short format configuration
            val attrCount = reader.attributeCount
            for (i in 0 until attrCount) {
                when (reader.getAttributeLocalName(i)) {
                    "name" -> key = reader.getAttributeValue(i)?.intern()
                    "value" -> confValue = reader.getAttributeValue(i)?.intern()
                    "final" -> confFinal = "true" == reader.getAttributeValue(i)
                    "source" -> reader.getAttributeValue(i)?.intern()?.let { confSource.add(it) }
                    "tag" -> confTag = reader.getAttributeValue(i)?.intern()
                }
            }
        }

        @Throws(XMLStreamException::class, IOException::class)
        private fun handleInclude() {
            // Determine href for xi:include
            confInclude = null
            val attrCount = reader.attributeCount
            var items: List<ParsedItem>
            for (i in 0 until attrCount) {
                val attrName = reader.getAttributeLocalName(i)
                if ("href" == attrName) {
                    confInclude = reader.getAttributeValue(i)
                }
            }
            
            val confInclude2 = confInclude ?: return

            // Determine if the included resource is a classpath resource
            // otherwise fallback to a file resource
            // xi:include are treated as inline and retain current source
            val include: URL? = ResourceLoader.getResource(confInclude2)
            if (include != null) {
                val classpathResource = Resource(include, name)
                // This is only called recursively while the lock is already held
                // by this thread, but synchronizing avoids a findbugs warning.
                synchronized(conf) {
                    val includeReader: XMLStreamReader2 = conf.getStreamReader(classpathResource)
                        ?: throw RuntimeException("$classpathResource not found")
                    items = Parser(includeReader, classpathResource, conf).parse()
                }
            } else {
                var url: URL
                try {
                    url = URL(confInclude2)
                    url.openConnection().connect()
                } catch (ioe: IOException) {
                    var href = File(confInclude2)
                    if (!href.isAbsolute) {
                        // Included resources are relative to the current resource
                        var baseFile = try {
                            File(URI(name))
                        } catch (e: IllegalArgumentException) {
                            File(name)
                        } catch (e: URISyntaxException) {
                            File(name)
                        }
                        baseFile = baseFile.parentFile
                        href = File(baseFile, href.path)
                    }
                    if (!href.exists()) {
                        // Resource errors are non-fatal iff there is 1 xi:fallback
//                        fallbackAllowed = true
                        return
                    }
                    url = href.toURI().toURL()
                }
                val uriResource = Resource(url, name)
                // This is only called recursively while the lock is already held
                // by this thread, but synchronizing avoids a findbugs warning.
                synchronized(conf) {
                    val includeReader =
                        conf.getStreamReader(uriResource) ?: throw RuntimeException("$uriResource not found")
                    items = Parser(includeReader, uriResource, conf).parse()
                }
            }

            results.addAll(items)
        }

        @Throws(IOException::class)
        fun handleEndElement() {
            val tokenStr = token.toString()
            when (reader.localName) {
                "name" -> if (token.isNotEmpty()) key = tokenStr.trim { it <= ' ' }.intern()
                "value" -> if (token.isNotEmpty()) confValue = tokenStr.intern()
                "final" -> confFinal = "true" == tokenStr
                "source" -> confSource.add(tokenStr.intern())
                "tag" -> if (token.isNotEmpty()) confTag = tokenStr.intern()
                "property" -> handleEndProperty()
                else -> {
                }
            }
        }

        fun handleEndProperty() {
            val confSourceArray = if (confSource.isEmpty()) {
                nameSingletonArray
            } else {
                confSource.add(name)
                confSource.toTypedArray()
            }

            // Read tags and put them in propertyTagsMap
//            if (confTag != null) {
//                readTagFromConfig(confTag, confName, confValue, confSourceArray)
//            }

            val key0 = key ?: return
            results.add(ParsedItem(name, key0, confValue, confFinal, confSourceArray))
        }

        @Throws(IOException::class, XMLStreamException::class)
        fun parseNext() {
            when (reader.next()) {
                XMLStreamConstants.START_ELEMENT -> handleStartElement()
                XMLStreamConstants.CHARACTERS, XMLStreamConstants.CDATA -> if (parseToken) {
                    val text = reader.textCharacters
                    token.appendRange(text, reader.textStart, reader.textStart + reader.textLength)
                }
                XMLStreamConstants.END_ELEMENT -> handleEndElement()
                else -> {
                }
            }
        }
    }
}
