package ai.platon.pulsar.skeleton.crawl.component

import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Parameterized
import ai.platon.pulsar.persist.WebDb
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.skeleton.common.message.MiscMessageWriter
import ai.platon.pulsar.skeleton.crawl.schedule.DefaultFetchSchedule
import ai.platon.pulsar.skeleton.crawl.schedule.FetchSchedule
import ai.platon.pulsar.skeleton.crawl.scoring.ScoringFilters

/**
 * The update component.
 */
class UpdateComponent(
    val webDb: WebDb,
    val fetchSchedule: FetchSchedule,
    val scoringFilters: ScoringFilters? = null,
    val messageWriter: MiscMessageWriter? = null,
    val conf: ImmutableConfig,
) : Parameterized {
    constructor(webDb: WebDb, conf: ImmutableConfig) : this(webDb, DefaultFetchSchedule(conf), null, null, conf)

    fun updateFetchSchedule(page: WebPage) {
    }
}
