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

import ai.platon.pulsar.persist.rdb.model.ServerInstance;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import java.util.List;

import static ai.platon.pulsar.persist.rdb.model.ServerInstance.Type.FetchService;
import static org.junit.Assert.*;

public class JTestServiceResource extends ResourceTestBase {

  private static String FetchServerName = FetchService.name();

  @Test
  public void testRegister() {
    ServerInstance serverInstance = new ServerInstance("", 19888, FetchService);
    serverInstance = target("service").path("register").request(MediaType.APPLICATION_JSON)
        .post(Entity.json(serverInstance), ServerInstance.class);
    assertEquals(19888, serverInstance.getPort());
    assertEquals("127.0.0.1", serverInstance.getIp());
  }

  @Test
  public void testListOfInteger() {
    List<Integer> listOfInteger = target("service").path("listOfInteger").request().get(new GenericType<List<Integer>>() {});
    System.out.println(listOfInteger);
  }

  @Test
  public void testServiceResource() {
    // Register
    ServerInstance serverInstance = null;
    for (int i = 0; i < 1; ++i) {
      serverInstance = target("service").path("register").request(MediaType.APPLICATION_JSON)
          .post(Entity.json(new ServerInstance("127.0.0." + i, 19888 + i, FetchService)), ServerInstance.class);
    }

    // List
    List<ServerInstance> serverInstances = target("service").request(MediaType.APPLICATION_JSON)
        .get(new GenericType<List<ServerInstance>>() {});
    // System.out.println(serverInstances);
    assertTrue(serverInstances.contains(serverInstance));

    // Unregister
    serverInstance = target("service").path("unregister").path(String.valueOf(serverInstance.getId()))
        .request(MediaType.APPLICATION_JSON).delete(ServerInstance.class);
    serverInstances = target("service").request(MediaType.APPLICATION_JSON)
        .get(new GenericType<List<ServerInstance>>() {});
    // System.out.println(serverInstances);
    assertFalse(serverInstances.contains(serverInstance));
  }
}
