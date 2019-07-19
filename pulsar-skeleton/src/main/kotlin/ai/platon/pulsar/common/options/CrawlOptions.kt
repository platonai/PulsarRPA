package ai.platon.pulsar.common.options

import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.common.config.PulsarConstants
import com.beust.jcommander.Parameter
import org.apache.commons.lang3.StringUtils
import java.text.DecimalFormat
import java.time.Duration
import java.time.ZoneId
import java.util.*

/**
 * Created by vincent on 17-3-18.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
class CrawlOptions : CommonOptions {

    @Parameter(names = ["-log", "-verbose"], description = "Log level for this crawl task")
    val verbose = 0
    @Parameter(names = ["-i", "--fetch-interval"], converter = DurationConverter::class, description = "Fetch interval")
    var fetchInterval = Duration.ofHours(1)
    @Parameter(names = ["-p", "--fetch-priority"], description = "Fetch priority")
    val fetchPriority = PulsarConstants.FETCH_PRIORITY_DEFAULT
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

    constructor(): super() {
        addObjects(this, linkOptions)
    }

    constructor(args: String): super(args.replace("=".toRegex(), " ")) {
        addObjects(this, linkOptions)
    }

    constructor(args: String, conf: ImmutableConfig): super(args.replace("=".toRegex(), " ")) {
        this.init(conf)
        addObjects(this, linkOptions)
    }

    constructor(argv: Array<String>) : super(argv) {
        addObjects(this, linkOptions)
    }

    constructor(argv: Array<String>, conf: ImmutableConfig): super(argv) {
        this.init(conf)
        addObjects(this, linkOptions)
    }

    constructor(argv: Map<String, String>) : super(argv) {
        addObjects(this, linkOptions)
    }

    constructor(argv: Map<String, String>, conf: ImmutableConfig) : super(argv) {
        this.init(conf)
        addObjects(this, linkOptions)
    }

    private fun init(conf: ImmutableConfig) {
        this.fetchInterval = conf.getDuration(CapabilityTypes.FETCH_INTERVAL, fetchInterval)
        this.score = conf.getInt(CapabilityTypes.INJECT_SCORE, score)
        this.depth = conf.getUint(CapabilityTypes.CRAWL_MAX_DISTANCE, depth)!!
        this.linkOptions = LinkOptions("", conf)
    }

    fun formatKeywords(): String {
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
            Objects.requireNonNull(args)
            Objects.requireNonNull(conf)

            if (StringUtils.isBlank(args)) {
                return CrawlOptions(arrayOf(), conf)
            }

            val options = CrawlOptions(args, conf)
            options.parse()

            return options
        }
    }
}
