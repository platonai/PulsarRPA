package ai.platon.pulsar.common.options

import ai.platon.pulsar.common.PulsarParams
import com.beust.jcommander.Parameter

/**
 * Created by vincent on 17-4-12.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
open class CommonOptions : PulsarOptions {
    @Parameter(names = [PulsarParams.ARG_CRAWL_ID], description = "crawl id, (default : \"storage.crawl.id\")")
    var crawlId = ""
    @Parameter(names = ["-h", "-help", "--help"], help = true, description = "Print help text")
    override var isHelp: Boolean = false

    constructor() {
        addObjects(this)
    }

    constructor(argv: Array<String>) : super(argv) {
        addObjects(this)
    }

    constructor(args: String) : super(args) {
        addObjects(this)
    }

    constructor(argv: Map<String, String>) : super(argv) {
        addObjects(this)
    }
}
