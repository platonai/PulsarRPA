package `fun`.platonic.pulsar.common

import `fun`.platonic.pulsar.common.config.MutableConfig
import org.apache.hadoop.conf.Configuration

/**
 * Utility to create Hadoop [Configuration]s that include scent specific
 * resources.
 *
 * A simple scent configuration loader used where ScentContext is not available
 */
object SimpleScentConfig: MutableConfig() {
    const val DEFAULT_RESOURCE = "scent-default.xml"
    const val SPECIFIED_RESOURCE = "scent-site.xml"

    /**
     * Create a configuration for scent context. This will load the standard scent project resources,
     * `scent-default.xml` and `scent-site.xml`.
     */
    init {
        unbox().addResource(DEFAULT_RESOURCE)
        unbox().addResource(SPECIFIED_RESOURCE)
    }
}
