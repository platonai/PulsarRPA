package ai.platon.pulsar.common.options

import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.persist.HyperlinkPersistable
import ai.platon.pulsar.persist.gora.generated.GHypeLink
import com.beust.jcommander.Parameter
import java.util.*
import java.util.function.Predicate

/**
 * Created by vincent on 17-3-18.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
class LinkOptions : PulsarOptions {
    @Parameter(names = ["-css", "--restrict-css"], description = "Path to the DOM to follow links")
    var restrictCss = "body"
    @Parameter(names = ["-amin", "--anchor-min-length"], description = "Anchor min length")
    var minAnchorLength = 5
    @Parameter(names = ["-amax", "--anchor-max-length"], description = "Anchor max length")
    var maxAnchorLength = 50
    @Parameter(names = ["-areg", "--anchor-regex"], description = "Anchor regex")
    var anchorRegex = ".+"
    @Parameter(names = ["-umin", "--url-min-length"], description = "Min url length")
    var minUrlLength = 23
    @Parameter(names = ["-umax", "--url-max-length"], description = "Max url length")
    var maxUrlLength = 150
    @Parameter(names = ["-upre", "--url-prefix"], description = "Url prefix")
    var urlPrefix = ""
    @Parameter(names = ["-ucon", "--url-contains"], description = "Url contains")
    var urlContains = ""
    @Parameter(names = ["-upos", "--url-postfix"], description = "Url postfix")
    var urlPostfix = ""
    @Parameter(names = ["-ureg", "--url-regex"], description = "Url regex")
    var urlRegex = ".+"
    @Parameter(names = ["-log", "--log-level"], description = "Log level")
    var logLevel = 0

    private val report: MutableList<String> = LinkedList()

    constructor(): super() {}
    constructor(args: String) : super(args) {}
    constructor(args: Array<String>) : super(args) {}

    constructor(args: String, conf: ImmutableConfig) : super(args) {
        init(conf)
    }
    constructor(args: Array<String>, conf: ImmutableConfig) : super(args) {
        init(conf)
    }

    constructor(args: Map<String, String>) : super(args) {}

    private fun init(conf: ImmutableConfig) {
        minAnchorLength = conf.getUint(CapabilityTypes.PARSE_MIN_ANCHOR_LENGTH, 8)
        maxAnchorLength = conf.getUint(CapabilityTypes.PARSE_MAX_ANCHOR_LENGTH, 40)
    }

    fun filter(l: HyperlinkPersistable): Int {
        return filter(l.url, l.text)
    }

    fun filter(url: String, anchor: String): Int {
        if (anchor.length < minAnchorLength || anchor.length > maxAnchorLength) {
            return 100
        }
        if (anchorRegex.isNotEmpty() && ".+" != anchorRegex) {
            if (!anchor.matches(anchorRegex.toRegex())) {
                return 101
            }
        }
        return filter(url)
    }

    fun filter(url: String): Int {
        if (url.length < minUrlLength || url.length > maxUrlLength) {
            return 200
        }
        if (urlPrefix.isNotEmpty() && !url.startsWith(urlPrefix)) {
            return 210
        }
        if (urlPostfix.isNotEmpty() && !url.endsWith(urlPostfix)) {
            return 211
        }
        if (urlContains.isNotEmpty() && !url.contains(urlContains)) {
            return 212
        }
        return if (urlRegex.isNotEmpty() && !url.matches(urlRegex.toRegex())) {
            213
        } else 0
    }

    fun asUrlPredicate(): Predicate<String> {
        report.clear()
        return Predicate { url: String ->
            val r = this.filter(url)
            if (logLevel > 0) {
                report.add("$r <- $url")
            }
            0 == r
        }
    }

    fun asPredicate(): Predicate<HyperlinkPersistable> {
        report.clear()
        return Predicate { l: HyperlinkPersistable ->
            val r = this.filter(l.url, l.text)
            if (logLevel > 0) {
                report.add(r.toString() + " <- " + l.url + "\t" + l.text)
            }
            0 == r
        }
    }

    fun asGHypeLinkPredicate(): Predicate<GHypeLink> {
        report.clear()
        return Predicate { l: GHypeLink ->
            val r = this.filter(l.url.toString(), l.anchor.toString())
            if (logLevel > 0) {
                report.add(r.toString() + " <- " + l.url + "\t" + l.anchor)
            }
            0 == r
        }
    }

    override fun getParams(): Params {
        return Params.of(
                "-css", restrictCss,
                "-amin", minAnchorLength,
                "-amax", maxAnchorLength,
                "-areg", anchorRegex,
                "-umin", minUrlLength,
                "-umax", maxUrlLength,
                "-upre", urlPrefix,
                "-ucon", urlContains,
                "-upos", urlPostfix,
                "-ureg", urlRegex
        )
                .filter { it.value != null }
                .filter { it.value.toString().isNotEmpty() }
    }

    fun build(): String {
        return params.withKVDelimiter(" ").formatAsLine()
    }

    fun getReport(): List<String> {
        return report
    }

    override fun toString(): String {
        return build()
    }

    companion object {
        // shortest url example: http://news.baidu.com/
        // longest url example: http://data.news.163.com/special/datablog/
        const val DEFAULT_SEED_ARGS = "-amin 2 -amax 4 -umin 23 -umax 45"
        val DEFAULT_SEED_OPTIONS = parse(DEFAULT_SEED_ARGS)
        val DEFAULT = LinkOptions()
        fun parse(args: String): LinkOptions {
            val options = LinkOptions(args)
            options.parse()
            return options
        }

        @JvmStatic
        fun parse(args: String, conf: ImmutableConfig): LinkOptions {
            val options = LinkOptions(args, conf)
            options.parse()
            return options
        }
    }
}
