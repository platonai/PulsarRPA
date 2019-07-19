package ai.platon.pulsar.common.options

import ai.platon.pulsar.common.config.Params
import com.beust.jcommander.Parameter


/**
 * Created by vincent on 17-7-14.
 */
class ParseOptions : CommonOptions {
    @Parameter(names = ["-ps", "--parse"], description = "Parse the page.")
    var isParse = false
    @Parameter(names = ["-rpl", "--reparse-links"], description = "Re-parse all links if the parsed.")
    var isReparseLinks = false
    @Parameter(names = ["-nlf", "--no-link-filter"], description = "No filters applied to parse links.")
    var isNoLinkFilter = false
    @Parameter(names = ["-prst", "--persist"], description = "Persist the page.")
    var isPersist = false

    constructor(): super()

    constructor(args: String) : super(args)

    override fun getParams(): Params {
        return Params.of(
                "-ps", isParse,
                "-rpl", isReparseLinks,
                "-nlf", isNoLinkFilter,
                "-prst", isPersist
        )
    }

    override fun toString(): String {
        return params.withCmdLineStyle(true).withKVDelimiter(" ")
                .formatAsLine().replace("\\s+".toRegex(), " ")
    }

    companion object {
        fun parse(args: String): ParseOptions {
            val options = ParseOptions(args)
            options.parse()
            return options
        }
    }
}
