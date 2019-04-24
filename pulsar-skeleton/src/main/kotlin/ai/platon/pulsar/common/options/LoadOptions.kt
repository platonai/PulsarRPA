package ai.platon.pulsar.common.options

import ai.platon.pulsar.common.config.MutableConfig
import ai.platon.pulsar.common.config.Params
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
class LoadOptions : CommonOptions {

    @Parameter(names = ["-i", "--expires"], converter = DurationConverter::class, description = "Page datum expire time")
    var expires = Duration.ofDays(3650)

    @Parameter(names = ["-pst", "-persist", "--persist"], description = "Persist page(s) once fetched")
    var isPersist = true

    @Parameter(names = ["-shortenKey", "--shorten-key"], description = "Page key is generated from baseUrl with parameters removed")
    var isShortenKey = false

    @Parameter(names = ["-retry", "--retry"], description = "Retry fetching the page if it's failed last time")
    var isRetry = false

    @Parameter(names = ["-lazyFlush", "--lazy-flush"], description = "Flush db only explicit called")
    var isLazyFlush = false

    @Parameter(names = ["-preferParallel", "--prefer-parallel"], description = "Parallel fetch urls whenever applicable")
    var isPreferParallel = false

    @Parameter(names = ["-fetchMode", "--fetch-mode"], converter = FetchModeConverter::class, description = "The fetch mode")
    var fetchMode = FetchMode.SELENIUM

    @Parameter(names = ["-browser", "--browser"], converter = BrowserTypeConverter::class, description = "The browser to use")
    var browser: String? = null

    @Parameter(names = ["-ignoreFailed", "--ignore-failed"], arity = 1, description = "Ignore all failed pages in batch loading")
    var isIgnoreFailed = true

    @Parameter(names = ["-background", "--background"], description = "Fetch the page in background")
    var isBackground: Boolean = false

    @Parameter(names = ["-nord", "-noRedirect", "--no-redirect"], description = "Do not redirect")
    var isNoRedirect = false

    @Parameter(names = ["-hardRedirect", "--hard-redirect"], arity = 1, description = "Return the entire page record " + "instead of the temp page with the target's content when redirect")
    var isHardRedirect = true

    @Parameter(names = ["-ps", "-parse", "--parse"], description = "Parse the page")
    var isParse = false

    @Parameter(names = ["-q", "-query", "--query"], description = "Extract query to extract data from")
    var query: String? = null

    @Parameter(names = ["-m", "-withModel", "--with-model"], description = "Also load page model")
    var isWithModel = false

    @Parameter(names = ["-lk", "-withLinks", "--with-links"], description = "Contains links when loading page model")
    var isWithLinks = false

    @Parameter(names = ["-tt", "-withText", "--with-text"], description = "Contains text when loading page model")
    var isWithText = false

    @Parameter(names = ["-rpl", "-reparseLinks", "--reparse-links"], description = "Re-parse all links if the page is parsed")
    var isReparseLinks = false

    @Parameter(names = ["-nolf", "-noLinkFilter", "--no-link-filter"], description = "No filters applied to parse links")
    var isNoLinkFilter = false

    var mutableConfig: MutableConfig? = null

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
        return Params.of(
                "-ps", isParse,
                "-q", query,
                "-m", isWithModel,
                "-lk", isWithLinks,
                "-tt", isWithText,
                "-retry", isRetry,
                "-rpl", isReparseLinks,
                "-nord", isNoRedirect,
                "-nolf", isNoLinkFilter,
                "-prst", isPersist,
                "-shortenKey", isShortenKey,
                "-expires", expires,
                "-lazyFlush", isLazyFlush,
                "-fetchMode", fetchMode,
                "-browser", browser,
                "-preferParallel", isPreferParallel,
                "-background", isBackground,
                "-hardRedirect", isHardRedirect
        )
    }

    override fun toString(): String {
        return params.withCmdLineStyle(true).withKVDelimiter(" ")
                .formatAsLine().replace("\\s+".toRegex(), " ")
    }

    companion object {

        var DEFAULT = LoadOptions()

        fun create(): LoadOptions {
            val options = LoadOptions()
            options.parse()
            return options
        }

        fun parse(args: String): LoadOptions {
            val options = LoadOptions(args)
            options.parse()
            return options
        }

        fun parse(args: String, mutableConfig: MutableConfig): LoadOptions {
            val options = LoadOptions(args)
            options.parse()
            options.mutableConfig = mutableConfig
            return options
        }
    }
}
