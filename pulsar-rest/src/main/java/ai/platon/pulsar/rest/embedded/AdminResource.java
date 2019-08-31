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
package ai.platon.pulsar.rest.embedded;

import ai.platon.pulsar.rest.model.response.PulsarStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping(value = "/admin")
public class AdminResource {

  private static final Logger LOG = LoggerFactory.getLogger(AdminResource.class);
  private static final Duration DELAY_SEC = Duration.ofSeconds(10);

  public static String SIMPLE_AUTH_TOKEN = "admin@localhost:iwqxi8iloqpol";

//  @Autowired
//  MasterResourceConfig masterResourceConfig;
//
//  @Autowired
//  private JobConfigurations jobConfigurations;

  @GetMapping
  public PulsarStatus getPulsarStatus() {
    PulsarStatus status = new PulsarStatus();

//    status.setConfiguration(jobConfigurations.list());

    return status;
  }

  @PutMapping("/stop")
  public int stop(@RequestParam("authToken") String authToken, @RequestParam("force") boolean force) {
    if (!SIMPLE_AUTH_TOKEN.equals(authToken)) {
      return -1;
    }

    scheduleServerStop(force);

    return 0;
  }

  private void scheduleServerStop(boolean force) {
//    LOG.info("Server shutdown scheduled in {} seconds", DELAY_SEC.getSeconds());
//    Thread thread = new Thread() {
//      public void run() {
//        PMaster pMaster = (PMaster) masterResourceConfig.getProperty(PMaster.class.getName());
//        if (!pMaster.isStarted()) {
//          return;
//        }
//
//        try {
//          Thread.sleep(DELAY_SEC.getSeconds());
//        } catch (InterruptedException e) {
//          Thread.currentThread().interrupt();
//        }
//
//        pMaster.shutdownNow();
//      }
//    };
//    thread.setDaemon(true);
//    thread.start();
//    LOG.info("Service shutting down...");
  }
}
