/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package ai.platon.pulsar.rest.api.service.deprecated;

import ai.platon.pulsar.common.config.CapabilityTypes;
import ai.platon.pulsar.common.config.MutableConfig;
import ai.platon.pulsar.rest.api.model.request.FetchConfig;
import com.google.common.collect.Maps;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static ai.platon.pulsar.common.config.CapabilityTypes.PULSAR_CONFIG_ID;

public class JobConfigurations {

  public static final Logger LOG = LoggerFactory.getLogger(JobConfigurations.class);

  public static final String DEFAULT = "default";

  private Map<String, MutableConfig> configurations = Maps.newConcurrentMap();

  private AtomicInteger configSequence = new AtomicInteger();

  public JobConfigurations() {
    configurations.put(JobConfigurations.DEFAULT, new MutableConfig());
  }

  public Set<String> list() {
    return configurations.keySet();
  }

  public String create(FetchConfig pulsarConfig) {
    // The client do not specified a config id, generate one.
    if (StringUtils.isBlank(pulsarConfig.getConfigId())) {
      pulsarConfig.setConfigId(generateId(pulsarConfig));
    }

    // LOG.info("Try to create pulsar config : " + pulsarConfig.toString());

    // Duplicated pulsar config
    if (!canCreate(pulsarConfig)) {
      return null;
    }

    createHadoopConfig(pulsarConfig);

    if (LOG.isInfoEnabled()) {
      LOG.info("Created a new FetchConfig, #" + pulsarConfig.getConfigId());
    }

    return pulsarConfig.getConfigId();
  }

  public MutableConfig getDefault() { return configurations.get(JobConfigurations.DEFAULT); }

  public MutableConfig get(String confId) {
    if (confId == null) {
      return configurations.get(JobConfigurations.DEFAULT);
    }
    return configurations.get(confId);
  }

  public Map<String, String> getAsMap(String confId) {
    Configuration configuration = configurations.get(confId).unbox();
    if (configuration == null) {
      return Collections.emptyMap();
    }

    Iterator<Entry<String, String>> iterator = configuration.iterator();
    Map<String, String> configMap = Maps.newTreeMap();
    while (iterator.hasNext()) {
      Entry<String, String> entry = iterator.next();
      configMap.put(entry.getKey(), entry.getValue());
    }

    return configMap;
  }

  public void setProperty(String confId, String propName, String propValue) {
    if (!configurations.containsKey(confId)) {
      throw new IllegalArgumentException("Unknown configId <" + confId + ">");
    }

    Configuration conf = configurations.get(confId).unbox();
    conf.set(propName, propValue);
  }

  public void delete(String confId) {
    configurations.remove(confId);

    if (LOG.isInfoEnabled()) {
      LOG.info("Removed a FetchConfig, #" + confId);
    }
  }

  /**
   * Add an ui specified part to construct the id
   * */
  private String generateId(FetchConfig pulsarConfig) {
    String configId = String.valueOf(configSequence.incrementAndGet());
    String uiCrawlId = pulsarConfig.getParams().get(CapabilityTypes.STORAGE_CRAWL_ID);

    return MessageFormat.format("{0}-{1}-{2}", uiCrawlId, pulsarConfig.getPriority(), configId);
  }

  private boolean canCreate(FetchConfig pulsarConfig) {
    if (pulsarConfig.isForce()) {
      return true;
    }

    return !configurations.containsKey(pulsarConfig.getConfigId());
  }

  private void createHadoopConfig(FetchConfig pulsarConfig) {
    MutableConfig conf = new MutableConfig();

    conf.set(PULSAR_CONFIG_ID, pulsarConfig.getConfigId());

    configurations.put(pulsarConfig.getConfigId(), conf);

    if (MapUtils.isEmpty(pulsarConfig.getParams())) {
      return;
    }

    for (Entry<String, String> e : pulsarConfig.getParams().entrySet()) {
      if (!StringUtils.isEmpty(e.getKey()) && !StringUtils.isEmpty(e.getValue())) {
        conf.set(e.getKey(), e.getValue());
      }
      else {
        LOG.warn("Received invalid pulsar config params : " + pulsarConfig.toString());
      }
    }
  }
}
