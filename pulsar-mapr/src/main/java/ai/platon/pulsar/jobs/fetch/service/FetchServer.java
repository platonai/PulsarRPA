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
package ai.platon.pulsar.jobs.fetch.service;

import ai.platon.pulsar.common.NetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import java.io.IOException;

/**
 * FetchServer is responsible to schedule fetch tasks
 */
public interface FetchServer {

  String FETCH_SERVER = "FETCH_SERVER";
  Logger LOG = LoggerFactory.getLogger(FetchServer.class);
  int BASE_PORT = 21000;
  String ROOT_PATH = "/api";

  void initialize(ApplicationContext applicationContext) throws IOException;

  boolean canStart();

  /**
   * Determine whether a server is running.
   *
   * @return true if a server instance is running.
   */
  static boolean isRunning(int port) {
    return NetUtil.testNetwork("127.0.0.1", port);
  }

  boolean isRunning();

  void start();

  /**
   * Starts the fetch server.
   */
  void startAsDaemon();

  boolean shutdown();

  /**
   * Stop the fetch server.
   *
   * @return true if no server is running or if the shutdown was successful.
   *         Return false if there are running jobs and the force switch has not
   *         been activated.
   */
  boolean shutdownNow();

  void registerServiceInstance();

  void unregisterServiceInstance();
}
