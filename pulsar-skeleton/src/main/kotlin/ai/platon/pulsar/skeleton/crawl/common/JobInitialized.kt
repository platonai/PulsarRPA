package ai.platon.pulsar.skeleton.crawl.common

import ai.platon.pulsar.common.config.ImmutableConfig

interface JobInitialized {
    fun setup(jobConf: ImmutableConfig)
}
