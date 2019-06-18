package ai.platon.pulsar.common.options

import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.CapabilityTypes.STORAGE_DATUM_EXPIRES
import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.persist.metadata.BrowserType
import ai.platon.pulsar.persist.metadata.FetchMode
import com.beust.jcommander.Parameter
import java.time.Duration
import java.util.*

/**
 * Created by vincent on 19-4-24.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 *
 * The expires field supports both ISO-8601 standard and hadoop time duration format
 * ISO-8601 standard : PnDTnHnMn.nS
 * Hadoop time duration format : Valid units are : ns, us, ms, s, m, h, d.
 */
open class LoadOptions : CommonOptions {
    @Parameter(names = ["-i", "-expires", "--expires"], converter = DurationConverter::class, description = "Page datum expires time")
    var expires: Duration? = null
    @Parameter(names = ["-pst", "-persist", "--persist"], description = "Persist page(s) once fetched")
    var persist = true
    @Parameter(names = ["-shortenKey", "--shorten-key"], description = "Page key is generated from baseUrl with parameters removed")
    var shortenKey = false

    @Parameter(names = ["-retry", "--retry"], description = "Retry fetching the page if it's failed last time")
    var retry = false
    @Parameter(names = ["-lazyFlush", "--lazy-flush"], description = "Flush db only explicit called")
    var lazyFlush = false
    @Parameter(names = ["-preferParallel", "--prefer-parallel"], description = "Parallel fetch urls whenever applicable")
    var preferParallel = false
    @Parameter(names = ["-fetchMode", "--fetch-mode"], converter = FetchModeConverter::class, description = "The fetch mode")
    var fetchMode = FetchMode.SELENIUM
    @Parameter(names = ["-browser", "--browser"], converter = BrowserTypeConverter::class, description = "The browser to use")
    var browser = BrowserType.CHROME
    @Parameter(names = ["-scrollCount"])
    var scrollCount = 10
    @Parameter(names = ["-scrollWaitTime"], converter = DurationConverter::class)
    var scrollWaitTime: Duration = Duration.ofMillis(1000)
    @Parameter(names = ["-pageLoadTimeout"], converter = DurationConverter::class)
    var pageLoadTimeout: Duration = Duration.ofSeconds(60)

    @Parameter(names = ["-background", "--background"], description = "Fetch the page in background")
    var background: Boolean = false
    @Parameter(names = ["-noRedirect", "--no-redirect"], description = "Do not redirect")
    var noRedirect = false
    @Parameter(names = ["-hardRedirect", "--hard-redirect"],
            description = "If false, return the temp page with the target's content, " +
                    "otherwise, return the entire page record when redirects")
    var hardRedirect = false
    @Parameter(names = ["-ps", "-parse", "--parse"], description = "Parse the page")
    var parse = false
    @Parameter(names = ["-q", "-query", "--query"], description = "Extract query to extract data from")

    var query: String? = null
    @Parameter(names = ["-m", "-withModel", "--with-model"], description = "Also load page model")
    var withModel = false
    @Parameter(names = ["-lk", "-withLinks", "--with-links"], description = "Contains links when loading page model")
    var withLinks = false
    @Parameter(names = ["-tt", "-withText", "--with-text"], description = "Contains text when loading page model")
    var withText = false
    @Parameter(names = ["-rpl", "-reparseLinks", "--reparse-links"], description = "Re-parse all links if the page is parsed")
    var reparseLinks = false
    @Parameter(names = ["-noNorm", "--no-link-normalizer"], arity = 1, description = "No normalizer is applied to parse links")
    var noNorm = true
    @Parameter(names = ["-noFilter", "--no-link-filter"], arity = 1, description = "No filter is applied to parse links")
    var noFilter = true

    // A volatile config is usually session scoped
    // TODO: easy to cause bugs, a better way to init volatile config is required
    var volatileConfig: VolatileConfig? = null
        set(value) {
            value?.setInt(CapabilityTypes.FETCH_SCROLL_DOWN_COUNT, scrollCount)
            value?.setDuration(CapabilityTypes.FETCH_SCROLL_DOWN_WAIT, scrollWaitTime)
            value?.setDuration(CapabilityTypes.FETCH_PAGE_LOAD_TIMEOUT, pageLoadTimeout)
            field = value
        }

    val realExpires: Duration
        get() {
            val d = Duration.ofDays(3650)
            return expires?:(volatileConfig?.getDuration(STORAGE_DATUM_EXPIRES, d)?:d)
        }

    val modifiedParams: Params get() {
        val rowFormat = "%40s: %s"
        val fields = this.javaClass.declaredFields
        return fields.filter { it.annotations.any { it is Parameter } && !isDefault(it.name) }
                .onEach { it.isAccessible = true }
                .filter { it.get(this) != null }
                .associate { "-${it.name}" to it.get(this) }
                .let { Params.of(it).withRowFormat(rowFormat) }
    }

    val modifiedOptions: Map<String, Any> get() {
        val fields = this.javaClass.declaredFields
        return fields.filter { it.annotations.any { it is Parameter } && !isDefault(it.name) }
                .onEach { it.isAccessible = true }
                .filter { it.get(this) != null }
                .associate { it.name to it.get(this) }
    }

    constructor() {
        addObjects(this)
    }

    constructor(args: Array<String>) : super(args) {
        addObjects(this)
    }

    constructor(args: String) : super(args.trim { it <= ' ' }.replace("=".toRegex(), " ")) {
        addObjects(this)
    }

    open fun isDefault(optionName: String): Boolean {
        val value = this.javaClass.declaredFields.find { it.name == optionName }
                ?.also { it.isAccessible = true }?.get(this)
        return value == defaultParams[optionName]
    }

    override fun getParams(): Params {
        val rowFormat = "%40s: %s"
        val fields = this.javaClass.declaredFields
        return fields.filter { it.annotations.any { it is Parameter } }
                .onEach { it.isAccessible = true }
                .associate { "-${it.name}" to it.get(this) }
                .filter { it.value != null }
                .let { Params.of(it).withRowFormat(rowFormat) }
    }

    override fun toString(): String {
        return modifiedParams.withCmdLineStyle(true).withKVDelimiter(" ")
                .formatAsLine().replace("\\s+".toRegex(), " ")
    }

    /**
     * Merge this LoadOptions and other LoadOptions, return a new LoadOptions
     * */
    fun mergeModified(other: LoadOptions): LoadOptions {
        val modified = other.modifiedOptions

        this.javaClass.declaredFields.forEach {
            if (it.name in modified.keys) {
                it.set(this, modified[it.name])
            }
        }

        return this
    }

    companion object {

        val default = LoadOptions()
        val defaultParams = default.javaClass.declaredFields.associate { it.name to it.get(default) }
        val defaultArgsMap = default.toArgsMap()
        val optionNames: List<String> = default.javaClass.declaredFields
                .filter { it.annotations.any { it is Parameter } }.map { it.name }

        fun create(): LoadOptions {
            val options = LoadOptions()
            options.parse()
            return options
        }

        @JvmOverloads
        fun parse(args: String, volatileConfig: VolatileConfig? = null): LoadOptions {
            val options = LoadOptions(args)
            options.parse()
            options.volatileConfig = volatileConfig
            return options
        }

        @JvmOverloads
        fun mergeModified(o1: LoadOptions, o2: LoadOptions, volatileConfig: VolatileConfig? = null): LoadOptions {
            val options = LoadOptions()
            options.volatileConfig = volatileConfig
            return options.mergeModified(o1).mergeModified(o2)
        }
    }
}
