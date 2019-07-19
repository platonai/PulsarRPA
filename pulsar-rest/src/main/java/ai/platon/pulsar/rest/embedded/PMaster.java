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

import ai.platon.pulsar.rest.MasterApplication;
import ai.platon.pulsar.rest.resources.SeedResource;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.grizzly2.httpserver.internal.LocalizationMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.net.URI;
import java.util.Scanner;

public class PMaster {

  public static final Logger LOG = LoggerFactory.getLogger(PMaster.class);

  private MasterApplication masterApplication;
  private HttpServer server;

  public PMaster(MasterApplication masterApplication) {
    this.masterApplication = masterApplication;
    masterApplication.property(PMaster.class.getName(), this);
    server = GrizzlyHttpServerFactory.createHttpServer(masterApplication.getBaseUri(), masterApplication, false);
  }

  public boolean isStarted() {
    return server != null && server.isStarted();
  }

  public void start() {
    try {
      server.start();
      Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    } catch (IOException e) {
      server.shutdownNow();
      throw new ProcessingException(LocalizationMessages.FAILED_TO_START_SERVER(e.getMessage()), e);
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
    server.shutdownNow();
  }

  public void shutdownNow() {
    LOG.info("Shutdown server");
    server.shutdown();
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

    ApplicationContext applicationContext = new ClassPathXmlApplicationContext("classpath:/**/rest-context.xml");
    MasterApplication masterApplication = applicationContext.getBean(MasterApplication.class);

    if (stop) {
      LOG.debug("Try stop server via REST Api");
      callStopApi(masterApplication.getBaseUri(), AdminResource.SIMPLE_AUTH_TOKEN, true);
    }
    else {
      masterApplication.property("contextConfig", applicationContext);
      masterApplication.registerClasses(AdminResource.class);
      masterApplication.packages(false, SeedResource.class.getPackage().getName());
      masterApplication.property("log4jConfigLocation", "log4j-jetty.properties");

      PMaster pMaster = new PMaster(masterApplication);

      if (interactive) {
        pMaster.startAsDaemon();
        System.out.println("Application started.\nTry out " + masterApplication.getBaseUri());
        while (true) {
          System.out.println("Hit X to exit : ");
          String cmd = new Scanner(System.in).nextLine();
          if (cmd.equalsIgnoreCase("x")) {
            pMaster.shutdown();
            break;
          }
        }
      } else {
        pMaster.start();
      }
    }
  }
}
