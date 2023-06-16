package ai.platon.pulsar.common.options.deprecated

import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.common.options.CommonOptions
import ai.platon.pulsar.common.options.DurationConverter
import ai.platon.pulsar.common.options.LinkOptions
import ai.platon.pulsar.common.options.WeightedKeywordsConverter
import com.beust.jcommander.Parameter
import org.apache.commons.lang3.StringUtils
import java.text.DecimalFormat
import java.time.Duration
import java.time.ZoneId
import java.util.*

/**
 * Created by vincent on 17-3-18.
 * Copyright @ 2013-2023 Platon AI. All rights reserved
 */
class CrawlOptions(argv: Array<String>) : CommonOptions(argv) {

    @Parameter(names = ["-verbose"], description = "Log level for this crawl task")
    var verbose = 0
    @Parameter(names = ["-i", "--fetch-interval"], converter = DurationConverter::class, description = "Fetch interval")
    var fetchInterval = Duration.ofHours(1)
    @Parameter(names = ["-p", "--fetch-priority"], description = "Fetch priority")
    var fetchPriority = AppConstants.FETCH_PRIORITY_DEFAULT
    @Parameter(names = ["-s", "--score"], description = "Injected score")
    var score = 0
    @Parameter(names = ["-d", "--depth"], description = "Max crawl depth. Do not crawl anything deeper")
    var depth = 1
    @Parameter(names = ["-z", "--zone-id"], description = "The zone id of the website we crawl")
    var zoneId = ZoneId.systemDefault().id

    @Parameter(names = ["-w", "--keywords"], converter = WeightedKeywordsConverter::class, description = "Keywords with weight, ")
    var keywords: Map<String, Double> = HashMap()

    @Parameter(names = ["-idx", "--indexer-url"], description = "Indexer url")
    var indexerUrl: String = ""

    var linkOptions = LinkOptions()
        private set

    init {
        addObjects(this, linkOptions)
    }

    constructor(): this("")

    constructor(args: String): this(split(args.replace("=".toRegex(), " ")))

    constructor(args: String, conf: ImmutableConfig): this(split(args.replace("=".toRegex(), " "))) {
        this.init(conf)
    }

    constructor(argv: Array<String>, conf: ImmutableConfig): this(argv) {
        this.init(conf)
    }

    private fun init(conf: ImmutableConfig) {
        this.fetchInterval = conf.getDuration(CapabilityTypes.FETCH_INTERVAL, fetchInterval)
        this.score = conf.getInt(CapabilityTypes.INJECT_SCORE, score)
        this.depth = conf.getUint(CapabilityTypes.CRAWL_MAX_DISTANCE, depth)
        this.linkOptions = LinkOptions("", conf)
    }

    private fun formatKeywords(): String {
        val df = DecimalFormat("##.#")
        return keywords.entries.map { it.key + "^" + df.format(it.value) }.joinToString { it }
    }

    override fun getParams(): Params {
        return Params.of(
                "-log", verbose,
                "-i", fetchInterval,
                "-p", fetchPriority,
                "-s", score,
                "-d", depth,
                "-z", zoneId,
                "-w", formatKeywords(),
                "-idx", indexerUrl
        )
                .filter { p -> StringUtils.isNotEmpty(p.value.toString()) }
                .merge(linkOptions.params)
    }

    override fun toString(): String {
        return params.withKVDelimiter(" ").formatAsLine().replace("\\s+".toRegex(), " ")
    }

    companion object {

        @JvmField
        val DEFAULT = CrawlOptions()

        fun parse(args: String, conf: ImmutableConfig): CrawlOptions {
            if (args.isBlank()) {
                return CrawlOptions(arrayOf(), conf)
            }

            return CrawlOptions(args, conf).apply { parse() }
        }
    }
}
