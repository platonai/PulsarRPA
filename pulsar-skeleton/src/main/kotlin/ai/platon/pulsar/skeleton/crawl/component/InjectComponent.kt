package ai.platon.pulsar.skeleton.crawl.component

import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Parameterized
import ai.platon.pulsar.persist.WebDb

/**
 * Created by vincent on 17-5-14.
 * Copyright @ 2013-2023 Platon AI. All rights reserved
 */
@Deprecated("deprecated for removal")
class InjectComponent(
    val webDb: WebDb,
    val conf: ImmutableConfig
)
