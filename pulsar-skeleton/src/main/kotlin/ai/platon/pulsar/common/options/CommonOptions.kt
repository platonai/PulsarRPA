package ai.platon.pulsar.common.options

import com.beust.jcommander.Parameter

/**
 * Created by vincent on 17-4-12.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
open class CommonOptions(argv: Array<String>) : PulsarOptions(argv) {
    @Parameter(names = ["-h", "-help", "--help"], help = true, description = "Print help text")
    override var isHelp: Boolean = false
}
