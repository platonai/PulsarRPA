package ai.platon.pulsar.common.options

import ai.platon.pulsar.common.PulsarParams
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Params
import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import com.beust.jcommander.ParameterException
import java.time.Duration
import java.util.*
import kotlin.math.abs

class GenerateOptions(argv: Array<String>, val conf: ImmutableConfig): CommonOptions(argv) {
    // TODO: crawlId is not used since AutoStorageService is started after Option parsing
    @Parameter(names = [PulsarParams.ARG_CRAWL_ID], description = "The crawl id, (default : \"storage.crawl.id\").")
    var crawlId: String = conf[CapabilityTypes.STORAGE_CRAWL_ID, ""]
    @Parameter(names = [PulsarParams.ARG_BATCH_ID], description = "The batch id")
    var batchId = generateBatchId()
    @Parameter(names = [PulsarParams.ARG_ROUND], description = "The crawl round")
    var round = 1
    @Parameter(names = ["-reGen"], description = "Re generate pages")
    var reGenerate = false
    @Parameter(names = ["-reSeeds"], description = "Re-generate all seeds")
    var reGenerateSeeds = false
    @Parameter(names = [PulsarParams.ARG_TOPN], description = "Number of top URLs to be selected")
    var topN = Int.MAX_VALUE
    @Parameter(names = [PulsarParams.ARG_NO_NORMALIZER], description = "Activate the normalizer plugin to normalize the url")
    var noNormalizer = false
    @Parameter(names = [PulsarParams.ARG_NO_FILTER], description = "Activate the filter plugin to filter the url")
    var noFilter = false
    @Parameter(names = [PulsarParams.ARG_ADDDAYS], description = "Adds numDays to the current time to facilitate crawling urls already. " +
            "Fetched sooner then default.")
    var adddays: Long = 0
    @Parameter(names = [PulsarParams.ARG_LIMIT], description = "task limit")
    var limit = Int.MAX_VALUE

    constructor(conf: ImmutableConfig): this(arrayOf(), conf)

    fun toParams(): Params {
        return Params.of(
                PulsarParams.ARG_CRAWL_ID, crawlId,
                PulsarParams.ARG_BATCH_ID, batchId,
                PulsarParams.ARG_TOPN, topN,
                PulsarParams.ARG_REGENERATE, reGenerate,
                PulsarParams.ARG_REGENERATE_SEEDS, reGenerateSeeds,
                PulsarParams.ARG_CURTIME, System.currentTimeMillis() + Duration.ofDays(adddays).toMillis(),
                PulsarParams.ARG_NO_NORMALIZER, noNormalizer,
                PulsarParams.ARG_NO_FILTER, noFilter
        )
    }

    private fun generateBatchId(): String {
        return (System.currentTimeMillis() / 1000).toString() + "-" + abs(Random().nextInt())
    }
}
