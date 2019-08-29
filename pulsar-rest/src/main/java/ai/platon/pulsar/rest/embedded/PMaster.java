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

import ai.platon.pulsar.PulsarEnv;
import ai.platon.pulsar.rest.MasterResourceConfig;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.ContextLoaderListener;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import java.net.URI;
import java.util.Scanner;

public class PMaster {

  public static final Logger LOG = LoggerFactory.getLogger(PMaster.class);

  private static final String CONTEXT_PATH = "/api";
  private static final String MAPPING_URL = "/api";
  private static final String WEBAPP_DIRECTORY = "webapp";

  private Server server;

  public PMaster(MasterResourceConfig config) {
    config.property(PMaster.class.getName(), this);
    ServletHolder servletHolder = new ServletHolder(new ServletContainer(config));

    server = new Server(config.getPort());
    ServletContextHandler context = new ServletContextHandler(server, "/*");
    context.addEventListener(new ContextLoaderListener()); // spring integration

    context.addServlet(servletHolder, "/api/*");
    context.setInitParameter("contextConfigLocation", PulsarEnv.Companion.getContextConfigLocation());
  }

  public boolean isStarted() {
    return server != null && server.isStarted();
  }

  public void start() {
    try {
      server.start();
      server.join();
      Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    } catch (Exception e) {
      LOG.error("Failed to start PMaster", e);
    } finally {
      server.destroy();
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

  public void shutdown() {
    LOG.info("Shutdown server now");
    server.destroy();
  }

  public void shutdownNow() {
    LOG.info("Shutdown server");
    server.destroy();
  }

  public static int callStopApi(URI baseUri, String authToken, boolean force) {
    WebTarget target = ClientBuilder.newClient().target(baseUri).path("/");

    Form form = new Form().param("authToken", authToken).param("force", String.valueOf(force));
    return target.path("admin").path("stop").request()
        .put(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE), Integer.class);
  }

  public static void printUsage() {
    System.out.println("Usage : PMaster [-i] [stop]");
  }

  public static void main(String[] args) {
    printUsage();

    boolean interactive = false;
    boolean stop = false;
    for (String arg : args) {
      if ("stop".equals(arg)) {
          stop = true;
      } else if ("-i".equals(arg)) {
          interactive = true;
      }
    }

    PulsarEnv env = PulsarEnv.Companion.getOrCreate();
    ApplicationContext applicationContext = PulsarEnv.Companion.getApplicationContext();
    MasterResourceConfig config = applicationContext.getBean(MasterResourceConfig.class);

    if (stop) {
      LOG.debug("Try stop server via REST Api");
      callStopApi(config.getBaseUri(), AdminResource.SIMPLE_AUTH_TOKEN, true);
    } else {
      config.registerClasses(AdminResource.class);

      PMaster master = new PMaster(config);

      if (interactive) {
        master.startAsDaemon();
        System.out.println("Application started.\nTry out " + config.getBaseUri());
        while (true) {
          System.out.println("Hit X to exit : ");
          String cmd = new Scanner(System.in).nextLine();
          if (cmd.equalsIgnoreCase("x")) {
            master.shutdown();
            break;
          }
        }
      } else {
        master.start();
      }
    }
  }
}
