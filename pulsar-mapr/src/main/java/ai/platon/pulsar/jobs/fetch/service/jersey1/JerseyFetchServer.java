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
package ai.platon.pulsar.jobs.fetch.service.jersey1;

import ai.platon.pulsar.common.StringUtil;
import ai.platon.pulsar.common.config.ImmutableConfig;
import ai.platon.pulsar.jobs.fetch.service.FetchResource;
import ai.platon.pulsar.jobs.fetch.service.FetchServer;
import ai.platon.pulsar.persist.rdb.model.ServerInstance;
import com.sun.jersey.api.container.grizzly2.GrizzlyServerFactory;
import com.sun.jersey.api.core.ClassNamesResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;
import org.apache.hadoop.classification.InterfaceStability;
import org.glassfish.grizzly.http.server.HttpServer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.IOException;
import java.net.URI;
import java.util.Scanner;

import static ai.platon.pulsar.common.config.PulsarConstants.JOB_CONTEXT_CONFIG_LOCATION;

/**
 * JerseyFetchServer is responsible to schedule fetch tasks
 */
@InterfaceStability.Unstable
public class JerseyFetchServer implements FetchServer {

  private ImmutableConfig conf;
  private URI baseUri;
  private ResourceConfig resourceConfig;
  private boolean isActive = false;
  private HttpServer server;
  private ServerInstance serverInstance;
  private MasterReference masterReference;

  public JerseyFetchServer(ImmutableConfig conf) {
    this.conf = conf;
  }

  @Override
  public void initialize(ApplicationContext applicationContext) throws IOException {
    this.masterReference = new MasterReference(conf);
    if (!masterReference.test()) {
      LOG.warn("Failed to create fetch server : PMaster is not available");
      return;
    }

    int port = masterReference.acquirePort(ServerInstance.Type.FetchService);
    if (port < BASE_PORT) {
      LOG.warn("Failed to create fetch server : can not acquire a valid port");
      return;
    }

    this.baseUri = URI.create(String.format("http://%s:%d%s", "127.0.0.1", port, ROOT_PATH));
    this.resourceConfig = new ClassNamesResourceConfig(FetchResource.class);
  }

  public URI getBaseUri() { return baseUri; }

  public boolean canStart() {
    if (isRunning()) {
      LOG.warn("Fetch server is already running");
      return false;
    }

    return resourceConfig != null && server == null;
  }

  public void start() {
    if (!canStart()) {
      LOG.warn("FetchServer is not initialized properly, will not start");
      return;
    }

    LOG.info("Starting fetch server on port: {}", baseUri.getPort());

    try {
      this.server = GrizzlyServerFactory.createHttpServer(baseUri, resourceConfig);
      Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
      registerServiceInstance();
      isActive = true;
    } catch (IOException e) {
      LOG.error(e.toString());
    }
  }

  /**
   * Starts the fetch server.
   */
  public void startAsDaemon() {
    Thread t = new Thread(this::start);
    t.setDaemon(true);
    t.start();
  }

  public boolean shutdown() {
    return shutdownNow();
  }

  /**
   * Stop the fetch server.
   *
   * @return true if no server is running or if the shutdown was successful.
   *         Return false if there are running jobs and the force switch has not
   *         been activated.
   */
  public boolean shutdownNow() {
    if (!isActive) {
      return true;
    }

    if (server == null) {
      LOG.warn("FetchServer is not initialized");
      return false;
    }

    try {
      unregisterServiceInstance();
    }
    catch (Throwable e) {
      LOG.error(StringUtil.stringifyException(e));
    }
    finally {
      server.stop();
      isActive = false;
    }

    LOG.info("FetchServer is stopped. Port : {}", baseUri.getPort());
    return true;
  }

  public boolean isRunning() {
    return server != null && server.isStarted() && FetchServer.isRunning(baseUri.getPort());
  }

  public void registerServiceInstance() {
    // We use an Internet ip rather than an Intranet ip
    serverInstance = new ServerInstance(null, baseUri.getPort(), ServerInstance.Type.FetchService);
    serverInstance = masterReference.register(serverInstance);
    LOG.info("Registered ServerInstance " + serverInstance);
  }

  public void unregisterServiceInstance() {
    masterReference.recyclePort(ServerInstance.Type.FetchService, baseUri.getPort());

    if (serverInstance != null) {
      serverInstance = masterReference.unregister(serverInstance.getId());
      LOG.info("UnRegistered ServerInstance " + serverInstance);
    }
  }

  public static void main(String[] args) throws Exception {
    ApplicationContext context = new ClassPathXmlApplicationContext(JOB_CONTEXT_CONFIG_LOCATION);
    JerseyFetchServer fetchServer = context.getBean(JerseyFetchServer.class);
    // JerseyFetchServer fetchServer1 = new JerseyFetchServer(context.getBean(ImmutableConfig.class));
    fetchServer.initialize(context);

    if (!fetchServer.canStart()) {
      System.out.println("Can not start FetchServer");
    }

    fetchServer.startAsDaemon();
    System.out.println("Application started.\nTry out " + fetchServer.getBaseUri());
    while (true) {
      System.out.println("Hit X to exit : ");
      String cmd = new Scanner(System.in).nextLine();
      if (cmd.equalsIgnoreCase("x")) {
        fetchServer.shutdown();
        break;
      }
    }
  }
}
