package ai.platon.pulsar.common.options

import ai.platon.pulsar.common.config.CapabilityTypes.STORAGE_DATUM_EXPIRES
import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.persist.metadata.BrowserType
import ai.platon.pulsar.persist.metadata.FetchMode
import com.beust.jcommander.Parameter
import java.time.Duration

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

    @Parameter(names = ["-noLinkFilter", "--no-link-filter"], description = "No filters applied to parse links")
    var noLinkFilter = false

    // A volatile config is usually session scoped
    // TODO: easy to cause bugs, a better way to init volatile config is required
    var volatileConfig: VolatileConfig? = null

    val realExpires: Duration
        get() {
            val d = Duration.ofDays(3650)
            return expires?:(volatileConfig?.getDuration(STORAGE_DATUM_EXPIRES, d)?:d)
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

    override fun getParams(): Params {
        val rowFormat = "%40s: %s"
        val fields = this.javaClass.declaredFields
        return fields.filter { it.annotations.any { it is Parameter } }
                .associate { "-${it.name}" to it.get(this) }
                .filter { it.value != null }
                .let { Params.of(it).withRowFormat(rowFormat) }
    }

    override fun toString(): String {
        return params.withCmdLineStyle(true).withKVDelimiter(" ")
                .formatAsLine().replace("\\s+".toRegex(), " ")
    }

    /**
     * Merge this LoadOptions and other LoadOptions, return a new LoadOptions
     * */
    fun merge(other: LoadOptions): LoadOptions {
        val argsMap = toMutableArgsMap()
        val modifiedArgs = other.toArgsMap().entries.filter { it.value != defaultArgsMap[it.key] }

        modifiedArgs.forEach { (k, v) ->
            if (argsMap[k] != v) {
                argsMap[k] = v
            }
        }

        val args = argsMap.entries.joinToString(" ") { "${it.key} ${it.value}" }
        return LoadOptions.parse(args)
    }

    companion object {

        val default = LoadOptions()
        val defaultArgsMap = default.toArgsMap()

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
    }
}
