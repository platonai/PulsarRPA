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
package ai.platon.pulsar.rest.resources;

import ai.platon.pulsar.persist.WebDb;
import ai.platon.pulsar.persist.WebPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Collectors;

import static ai.platon.pulsar.common.config.PulsarConstants.METRICS_HOME_URL;

@RestController
@RequestMapping("/metrics")
public class MetricsResource {
  public static final Logger LOG = LoggerFactory.getLogger(MetricsResource.class);

  private final WebDb webDb;

  @Autowired
  public MetricsResource(WebDb webDb) {
    this.webDb = webDb;
  }

  @GetMapping
  public String list(
          @PathVariable("limit") int limit) {
    WebPage page = webDb.getOrNil(METRICS_HOME_URL);
    if (page.isNil() || page.getLiveLinks().isEmpty()) {
      return "[]";
    }

    return "[\n" + page.getLiveLinks().values().stream()
            .limit(limit)
            .map(Object::toString)
            .collect(Collectors.joining(",\n")) + "\n]";
  }
}
