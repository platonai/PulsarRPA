package ai.platon.pulsar.common.options

import ai.platon.pulsar.common.AppFiles
import ai.platon.pulsar.common.PulsarParams
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Params
import com.beust.jcommander.Parameter
import java.util.*

/**
 * Command options for [WebGraphUpdateJob].
 */
class UpdateOptions(args: Array<String>, conf: ImmutableConfig) : CommonOptions(args) {
    @Parameter(description = "[batchId], If not specified, use last generated batch id.")
    var batchId: MutableList<String> = ArrayList()
    @Parameter(names = [PulsarParams.ARG_ROUND], description = "Crawl round")
    var round = conf.getInt(CapabilityTypes.CRAWL_ROUND, 1)
    @Parameter(names = [PulsarParams.ARG_LIMIT], description = "Update job limit")
    var limit = conf.getInt(CapabilityTypes.LIMIT, Int.MAX_VALUE)

    init {
        crawlId = conf.get(CapabilityTypes.STORAGE_CRAWL_ID)
        val bid = conf.get(CapabilityTypes.BATCH_ID, AppFiles.readBatchIdOrDefault("all"))
        batchId.add(bid)
    }

    override fun getParams(): Params {
        return Params.of(
                "crawlId", crawlId,
                "batchId", batchId[0],
                "round", round,
                "limit", limit
        )
    }
}
