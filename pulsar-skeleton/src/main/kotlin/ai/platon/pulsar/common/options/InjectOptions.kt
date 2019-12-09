package ai.platon.pulsar.common.options

import ai.platon.pulsar.common.PulsarParams
import ai.platon.pulsar.common.config.Params
import com.beust.jcommander.Parameter
import java.util.*

/**
 * Command options for inject tasks.
 * Expect the list option which specify the seed or seed file, the @ sign is not supported
 */
class InjectOptions(args: Array<String>) : CommonOptions(args) {
    @Parameter(description = "<seeds> \nSeed urls. You can use {@code @FILE} syntax to read from file.")
    var seeds: List<String> = ArrayList()
    @Parameter(names = [PulsarParams.ARG_LIMIT], description = "task limit")
    var limit = -1

    init {
        // We may read seeds from a file using @ sign, the file parsing should be handled manually
        expandAtSign = false
        // if acceptUnknownOptions is true, the main parameter (seeds) is not recognized
        acceptUnknownOptions = false
    }

    override fun getParams(): Params {
        return Params.of(
                PulsarParams.ARG_CRAWL_ID, crawlId,
                PulsarParams.ARG_SEEDS, if (seeds.isEmpty()) "" else seeds[0],
                PulsarParams.ARG_LIMIT, limit
        )
    }
}
