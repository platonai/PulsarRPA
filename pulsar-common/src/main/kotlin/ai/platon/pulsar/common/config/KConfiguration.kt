package ai.platon.pulsar.common.config

import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.ResourceLoader.getURLOrNull
import ai.platon.pulsar.common.config.KConfiguration.Companion.EXTERNAL_RESOURCE_BASE_DIR
import ai.platon.pulsar.common.urls.UrlUtils
import com.ctc.wstx.io.StreamBootstrapper
import com.ctc.wstx.io.SystemId
import org.codehaus.stax2.XMLStreamReader2
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
import java.util.concurrent.atomic.AtomicInteger
import javax.xml.stream.XMLStreamConstants
import javax.xml.stream.XMLStreamException
import javax.xml.stream.XMLStreamReader
import kotlin.io.path.isReadable
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries

class KConfiguration(
    /**
     * The profile of the configuration.
     * */
    val profile: String = "",
    @Deprecated("Use profile instead")
    val mode: String = profile,
    /**
     * The extra resources to load.
     * */
    val extraResources: Iterable<String> = listOf(),
    /**
     * Whether to load the default resources.
     * */
    private val loadDefaults: Boolean = true,
) : Iterable<Map.Entry<String, String?>> {

    companion object {
        val DEFAULT_RESOURCES = mutableSetOf("pulsar-default.xml")
        val EXTERNAL_RESOURCE_BASE_DIR = AppPaths.CONF_DIR.resolve("conf-enabled")
        val SYSTEM_DEFAULT_RESOURCES = mutableSetOf<String>()
        private val ID_SUPPLIER = AtomicInteger()
    }

    private var impl: ConfigurationImpl? = null
    private val assuredImplementation: ConfigurationImpl
        get() {
            if (impl == null) {
                impl = ConfigurationImpl(profile, profile, extraResources, loadDefaults)
                impl?.load()
            }
            
            return impl!!
        }
    
    val id = ID_SUPPLIER.incrementAndGet()
    val loadedResources: List<String> get() = impl?.resources?.map { it.name } ?: listOf()
    
    constructor(conf: KConfiguration) : this(conf.profile, conf.profile, conf.extraResources, conf.loadDefaults)
    
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
            assuredImplementation[name] = value
        }
    }
    
    fun unset(name: String) {
        assuredImplementation.remove(name)
    }
    
    operator fun get(name: String): String? {
        return assuredImplementation[name]?.toString()
    }
    
    fun get(name: String, defaultValue: String): String {
        return get(name) ?: defaultValue
    }
    
    fun setStrings(name: String, vararg values: String) {
        set(name, Strings.arrayToString(values))
    }
    
    fun setIfUnset(name: String?, value: String?) {
        if (get(name!!) == null) {
            set(name, value)
        }
    }
    
    /**
     * Return the number of keys in the configuration.
     *
     * @return number of keys in the configuration.
     */
    fun size() = assuredImplementation.size()
    
    /**
     * Clears all keys from the configuration.
     */
    fun clear() {
        impl = null
    }
    
    override fun iterator(): MutableIterator<Map.Entry<String, String>> {
        // Get a copy of just the string to string pairs. After the old object
        // methods that allow non-strings to be put into configurations are removed,
        // we could replace properties with a Map<String,String> and get rid of this
        // code.
        val result: MutableMap<String, String> = HashMap()
        for ((key, value) in assuredImplementation.properties) {
            if (key is String && value is String) {
                result[key] = value
            }
        }
        return result.entries.iterator()
    }
    
    @Synchronized
    fun reloadConfiguration() {
        impl = null // trigger reload
    }
    
    override fun toString() = assuredImplementation.toString()
}

private class ConfigurationImpl(
    private val profile: String,
    private val mode: String,
    private val extraResources: Iterable<String>,
    private val loadDefaults: Boolean,
) {
    private val logger = getLogger(this)

    val resourceNames = mutableSetOf<String>()
    val resourceURLs = mutableSetOf<String>()
    val resources = ArrayList<Resource>()
    val properties = Properties()

    private val wstxInputFactory = com.ctc.wstx.stax.WstxInputFactory()

    @Synchronized
    fun load() {
        resourceNames.clear()
        resourceURLs.clear()
        resources.clear()

        collectResourcePaths()
        resourceURLs.mapNotNull { UrlUtils.getURLOrNull(it) }.forEach { addResource(it) }
        if (loadDefaults && Files.isDirectory(EXTERNAL_RESOURCE_BASE_DIR)) {
            addExternalResource(EXTERNAL_RESOURCE_BASE_DIR)
        }
    }

    fun addResource(name: String) = addResourceObject(Resource(name))

    fun addResource(url: URL) = addResourceObject(Resource(url))

    fun addResource(path: Path) = addResourceObject(Resource(path))

    fun addExternalResource(baseDir: Path) {
        val externalResources = baseDir.listDirectoryEntries("*.xml").filter { it.isRegularFile() && it.isReadable() }
        if (externalResources.isEmpty()) {
            logger.info("You can add extra configuration files to the directory: {}", baseDir)
        } else {
            externalResources.onEach { logger.info("Found configuration: {}", it) }.forEach { addResource(it) }
        }
    }

    operator fun get(name: String): Any? = properties[name]

    /**
     * Provided for parallelism with the {@code getProperty} method. Enforces use of
     * strings for property keys and values. The value returned is the
     * result of the {@code Hashtable} call to {@code put}.
     *
     * @param key the key to be placed into this property list.
     * @param value the value corresponding to {@code key}.
     * @return     the previous value of the specified key in this property
     *             list, or {@code null} if it did not have one.
     * @see #getProperty
     */
    operator fun set(name: String, value: String): Any? = properties.setProperty(name, value)
    
    fun remove(name: String) = properties.remove(name)
    
    fun size() = properties.size
    
    override fun toString(): String {
        return resources.joinToString(", ", "[", "]") {
            it.name.substringAfterLast("/")
        }
    }

    private fun collectResourcePaths() {
        if (profile.isNotEmpty()) {
            set(CapabilityTypes.PROFILE_KEY, profile)
        }

        resourceNames.addAll(extraResources)
        if (loadDefaults) {
            resourceNames.addAll(KConfiguration.DEFAULT_RESOURCES)
        }

        for (name in resourceNames) {
            val realResource = findRealResource(profile, mode, name)?.toString()
            if (realResource != null && realResource !in resourceURLs) {
                resourceURLs.add(realResource)
                logger.info("Found configuration: {}", realResource)
            } else {
                logger.info("Resource not find: $name")
            }
        }
    }

    private fun findRealResource(profile: String, mode: String, name: String): URL? {
        return findNewStyleRealResource(profile, name) ?: findLegacyStyleRealResource(profile, mode, name)
    }

    private fun findLegacyStyleRealResource(profile: String, mode: String, name: String): URL? {
        val prefix = "config/legacy"
        val suffix = "$mode/$name"
        val searchPaths = arrayOf(
            "$prefix/$suffix", "$prefix/$profile/$suffix",
            "$prefix/$name", "$prefix/$profile/$name",
            name
        ).map { it.replace("/+".toRegex(), "/") }.distinct().sortedByDescending { it.length }

        return searchPaths.firstNotNullOfOrNull { SParser.wrap(it).resource }
    }

    private fun findNewStyleRealResource(profile: String, name: String): URL? {
        val prefix = "config"
        val extension = name.substringAfterLast(".")
        val nameWithoutExtension = name.substringBeforeLast(".")

        val searchPaths = arrayOf(
            "$prefix/$nameWithoutExtension-$profile.$extension"
        )
            .map { it.replace("/+".toRegex(), "/") }   // replace "//" with "/"
            .map { it.replace("-\\.".toRegex(), ".") } // replace "-." with ".", when profile is empty
            .distinct().sortedByDescending { it.length }

        return searchPaths.firstNotNullOfOrNull { SParser.wrap(it).resource }
    }
    
    @Synchronized
    private fun addResourceObject(resource: Resource) {
        resources.add(resource) // add to resources
        loadProps(resources.size - 1, false)
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
    private fun loadProps(startIdx: Int, fullReload: Boolean) {
        loadResources(resources, startIdx, fullReload)
    }
    
    private fun loadResources(
        resources: ArrayList<Resource>, startIdx: Int, fullReload: Boolean
    ) {
        if (loadDefaults && fullReload) {
            KConfiguration.SYSTEM_DEFAULT_RESOURCES.forEach { loadResource(Resource(it)) }
        }
        
        for (i in startIdx until resources.size) {
            loadResource(resources[i])?.let { resources[i] = it }
        }
        // this.addTags(properties);
    }
    
    private fun loadResource(wrapper: Resource): Resource? {
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
    
    private fun overlay(to: Properties, from: Properties) {
        synchronized(from) {
            for ((key, value) in from) {
                to[key] = value
            }
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
                val url = getURLOrNull(resource) ?: return null
                parse(url)
            }
            is Path -> {
                // a file resource
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
    
    class Resource(
        val resource: Any,
        val name: String = resource.toString()
    ) {
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
        val configurationImpl: ConfigurationImpl
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
            val include: URL? = getURLOrNull(confInclude2)
            if (include != null) {
                val classpathResource = Resource(include, name)
                // This is only called recursively while the lock is already held
                // by this thread, but synchronizing avoids a findbugs warning.
                synchronized(configurationImpl) {
                    val includeReader: XMLStreamReader2 = configurationImpl.getStreamReader(classpathResource)
                        ?: throw RuntimeException("$classpathResource not found")
                    items = Parser(includeReader, classpathResource, configurationImpl).parse()
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
                synchronized(configurationImpl) {
                    val includeReader =
                        configurationImpl.getStreamReader(uriResource)
                            ?: throw RuntimeException("$uriResource not found")
                    items = Parser(includeReader, uriResource, configurationImpl).parse()
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
