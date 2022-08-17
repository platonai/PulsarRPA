package ai.platon.pulsar.persist

import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.MutableConfig
import org.slf4j.LoggerFactory

typealias HadoopConfiguration = org.apache.hadoop.conf.Configuration

object HadoopUtils {

    fun toHadoopConfiguration(conf: ImmutableConfig): HadoopConfiguration {
        val hadoopConfiguration = HadoopConfiguration()
        conf.unbox().loadedResources.forEach {
            hadoopConfiguration.addResource(it)
        }
        hadoopConfiguration.reloadConfiguration()

        conf.unbox().forEach { (k, v) ->
            hadoopConfiguration[k] = v
        }

        return hadoopConfiguration
    }
}
