/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.platon.pulsar.persist

import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.MutableConfig
import org.slf4j.LoggerFactory

typealias HadoopConfiguration = org.apache.hadoop.conf.Configuration

object HadoopUtils {
    private val LOG = LoggerFactory.getLogger(HadoopUtils::class.java)

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

    /**
     * TODO: use a wrapper to HadoopConfiguration
     * */
    fun toMutableConfig(conf: HadoopConfiguration): MutableConfig {
        val config = MutableConfig()
        // TODO: read all resources from conf
        conf.forEach { (k, v) ->
            config[k] = v
        }
        return config
    }
}
