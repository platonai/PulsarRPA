package ai.platon.pulsar.persist

import ai.platon.pulsar.common.config.ImmutableConfig
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.Environment
import java.util.*
import kotlin.collections.Map
import kotlin.collections.MutableMap
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.emptyMap
import kotlin.collections.forEach
import kotlin.collections.set

typealias HadoopConfiguration = org.apache.hadoop.conf.Configuration

object HadoopUtils {

    fun toHadoopConfiguration(conf: ImmutableConfig): HadoopConfiguration {
        val hadoopConfiguration = HadoopConfiguration()

        conf.unbox().forEach { (k, v) ->
            hadoopConfiguration[k] = v
        }
        // spring properties has higher priority
        dumpSpringProperties(conf.environment).forEach { (k, v) ->
            hadoopConfiguration[k] = v
        }

        return hadoopConfiguration
    }

    private fun dumpSpringProperties(environment: Environment?): Map<String, String> {
        if (environment == null) {
            return emptyMap()
        }

        val properties: MutableMap<String, String> = TreeMap()
        if (environment is ConfigurableEnvironment) {
            for (propertySource in environment.propertySources) {
                val mapSource = propertySource.source
                if (mapSource is Map<*, *>) {
                    mapSource.forEach { (key, value) ->
                        if (key != null && value != null) {
                            properties[key.toString()] = value.toString()
                        }
                    }
                }
            }
        }
        return properties
    }
}
