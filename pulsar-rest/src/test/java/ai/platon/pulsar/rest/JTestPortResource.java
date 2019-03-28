/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * http://glassfish.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package ai.platon.pulsar.rest;

import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import ai.platon.pulsar.rest.service.PortManager;
import ai.platon.pulsar.persist.rdb.model.ServerInstance;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class JTestPortResource extends JerseyTest {

  private static String FetchServerName = ServerInstance.Type.FetchService.name();

  @Override
  protected Application configure() {
    ApplicationContext applicationContext = new ClassPathXmlApplicationContext("classpath:/rest-context/rest-test-context.xml");
    MasterApplication application = applicationContext.getBean(MasterApplication.class);
    application.property("contextConfig", applicationContext);
    return application;
  }

  @Test
  public void testWsVersion() {
    String version = javax.ws.rs.core.UriBuilder.class.getPackage().toString();
    System.out.println(version);
    assertTrue(version.contains("package javax.ws.rs.core, version 2.0"));
  }

  @Test
  public void testWelcome() {
    String greeting = target("/welcome").request().get(String.class);
    Assert.assertEquals("<p>Welcome to pulsar</p>", greeting);
  }

  @Test
  public void testPortAcquire() {
    for (int i = 0; i < 50; ++i) {
      int port = target("port").path("acquire").queryParam("type", FetchServerName).request().get(Integer.class);
      Assert.assertEquals(21001 + i, port);
    }
  }

  @Test
  public void testGetManager() {
    PortManager portManager = target("port").path("get-empty-port-manager")
        .queryParam("type", FetchServerName).request().get(PortManager.class);
    System.out.println(portManager);
  }

  @Test
  @Ignore("Failed to return a list of integer, we do not it's caused by a jersey bug or a dependency issue")
  public void testListOfInteger() {
    List<Integer> listOfInteger = target("port").path("listOfInteger")
        .request(MediaType.APPLICATION_JSON).get(new GenericType<ArrayList<Integer>>() {});
    System.out.println(listOfInteger);
  }

  @Test
  public void testPortRecycle() {
    for (int i = 0; i < 50; ++i) {
      int port = target("port").path("acquire").queryParam("type", FetchServerName).request().get(Integer.class);
      Assert.assertEquals(21001 + i, port);
    }

    int port = target("port").path("acquire").queryParam("type", FetchServerName).request().get(Integer.class);
    assertTrue(port > 21000);
    target("port").path("recycle").queryParam("type", FetchServerName)
        .request().put(Entity.entity(port, MediaType.TEXT_PLAIN_TYPE));

//    List<Integer> freePorts = target("port").path("free").queryParam("type", FetchServerName)
//        .request(MediaType.APPLICATION_JSON).get(new GenericType<List<Integer>>() {});
    Type listType = new TypeToken<ArrayList<Integer>>(){}.getType();
    String result = target("port").path("legacy").path("free").queryParam("type", FetchServerName)
        .request(MediaType.TEXT_PLAIN).get(String.class);
    List<Integer> freePorts = new GsonBuilder().create().fromJson(result, listType);

//    System.out.println(port);
//    System.out.println(freePorts);
    assertTrue(freePorts.contains(port));
  }
}
