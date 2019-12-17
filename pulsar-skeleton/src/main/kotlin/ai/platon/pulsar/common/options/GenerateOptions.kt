package ai.platon.pulsar.common.options

import ai.platon.pulsar.common.AppFiles
import ai.platon.pulsar.common.PulsarParams
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Params
import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import com.beust.jcommander.ParameterException
import java.time.Duration
import java.util.*
import kotlin.math.abs

class GenerateOptions(val conf: ImmutableConfig) {
    @Parameter(names = [PulsarParams.ARG_CRAWL_ID], description = "The crawl id, (default : \"storage.crawl.id\").")
    var crawlId: String = conf[CapabilityTypes.STORAGE_CRAWL_ID, ""]
    @Parameter(names = [PulsarParams.ARG_BATCH_ID], description = "The batch id")
    var batchId = generateBatchId()
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
    @Parameter(names = ["-help", "-h"], help = true, description = "print the help information")
    var isHelp = false

    fun parse(args: Array<String>) {
        crawlId = conf[CapabilityTypes.STORAGE_CRAWL_ID]
        val jc = JCommander(this)
        try {
            jc.parse(*args)
        } catch (e: ParameterException) {
            println(e.toString())
            println("Try '-h' or '-help' for more information.")
            System.exit(0)
        }
        if (isHelp) {
            jc.usage()
        }
    }

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
