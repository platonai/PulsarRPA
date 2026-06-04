
package ai.platon.pulsar.skeleton.crawl.schedule

import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.skeleton.common.message.MiscMessageMessageWriter

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
        messageWriter: MiscMessageMessageWriter? = null
) : AbstractFetchSchedule(conf, messageWriter)
