package ai.platon.pulsar.crawl.common

import ai.platon.pulsar.common.config.ImmutableConfig

interface JobInitialized {
    fun setup(jobConf: ImmutableConfig)
}
