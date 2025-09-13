
package ai.platon.pulsar.skeleton.crawl.schedule

import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.skeleton.common.message.MiscMessageWriter
import ai.platon.pulsar.skeleton.common.persist.ext.options
import ai.platon.pulsar.skeleton.crawl.component.FetchComponent
import ai.platon.pulsar.persist.WebPage
import java.time.Duration
import java.time.Instant

/**
 * This class implements the default re-fetch schedule. That is, no matter if
 * the page was changed or not, the `fetchInterval` remains
 * unchanged, and the updated page fetchTime will always be set to
 * `fetchTime + fetchInterval * 1000`.
 *
 * @author Andrzej Bialecki
 */
class DefaultFetchSchedule(
        conf: ImmutableConfig,
        messageWriter: MiscMessageWriter? = null
) : AbstractFetchSchedule(conf, messageWriter)
