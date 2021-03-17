package ai.platon.pulsar.common.options

import ai.platon.pulsar.common.AppFiles
import ai.platon.pulsar.common.PulsarParams
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Params
import com.beust.jcommander.Parameter

/**
 * Command options for [WebGraphUpdateJob].
 */
class UpdateOptions(argv: Array<String>, conf: ImmutableConfig) : CommonOptions(argv) {
    // TODO: crawlId is not used since AutoStorageService is started after Option parsing
    @Parameter(names = [PulsarParams.ARG_CRAWL_ID], description = "The crawl id, (default : \"storage.crawl.id\").")
    var crawlId: String = conf[CapabilityTypes.STORAGE_CRAWL_ID, ""]
    @Parameter(names = [PulsarParams.ARG_BATCH_ID], description = "If not specified, use last generated batch id.")
    var batchId: String = conf.get(CapabilityTypes.BATCH_ID, FetchOptions.defaultBatchId)
    @Parameter(names = [PulsarParams.ARG_ROUND], description = "Crawl round")
    var round = conf.getInt(CapabilityTypes.CRAWL_ROUND, 1)
    @Parameter(names = [PulsarParams.ARG_LIMIT], description = "Update job limit")
    var limit = conf.getInt(CapabilityTypes.LIMIT, Int.MAX_VALUE)

    constructor(conf: ImmutableConfig): this(arrayOf(), conf)

    override fun getParams(): Params {
        return Params.of(
                "crawlId", crawlId,
                "batchId", batchId,
                "round", round,
                "limit", limit
        )
    }

    companion object {
        val defaultBatchId = AppFiles.readBatchIdOrDefault(AppConstants.ALL_BATCHES)
    }
}
