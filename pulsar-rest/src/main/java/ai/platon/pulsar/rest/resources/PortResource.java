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

import ai.platon.pulsar.persist.rdb.model.ServerInstance;
import ai.platon.pulsar.rest.service.PortManager;
import com.google.common.collect.Maps;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.springframework.stereotype.Component;

import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Singleton
@Component
@Path("/port")
public class PortResource {

  private final Map<String, PortManager> portManagers = Maps.newConcurrentMap();

  public PortResource() {
    PortManager portManager = new PortManager(ServerInstance.Type.FetchService.name());
    portManagers.put(portManager.getType(), portManager);

    portManager = new PortManager(ServerInstance.Type.PulsarMaster.name());
    portManagers.put(portManager.getType(), portManager);
  }

  @GET
  @Path("/get-empty-port-manager")
  // @Consumes({ MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON })
  @Produces({ MediaType.APPLICATION_JSON })
  public PortManager getManager(@QueryParam("type") String type) {
    return new PortManager(type, 0, 0);
  }

  // TODO : Failed to return a list of integer, we do not if it's a jersey bug or dependency issue
  @GET
  @Path("/listOfInteger")
  @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
  public List<Integer> getListOfInteger(@QueryParam("type") String type) {
    return IntStream.range(0, 10).mapToObj(Integer::new).collect(Collectors.toList());
  }

  @GET
  @Path("/report")
  @Produces({ MediaType.TEXT_PLAIN })
  public String report() {
    String report = portManagers.values().stream()
        .map(p -> "\tPorts for " + p.getType() + " :\n" + p.toString())
        .collect(Collectors.joining("\n"));
    return "PortResource #" + hashCode() + report;
  }

  // TODO : Failed to return a list of integer, we do not if it's a jersey bug or dependency issue
  @GET
  @Path("/active")
  @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
  public List<Integer> activePorts(@QueryParam("type") String type) {
    return portManagers.get(type).getActivePorts();
  }

  // Jersey 1 does not support list of primary types
  @GET
  @Path("/legacy/active")
  @Produces({ MediaType.TEXT_PLAIN })
  public String getActivePortsLegacy(@QueryParam("type") String type) {
    Type listType = new TypeToken<ArrayList<Integer>>(){}.getType();
    return new GsonBuilder().create().toJson(portManagers.get(type).getActivePorts(), listType);
  }

  @GET
  @Path("/free")
  @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
  public List<Integer> getFreePorts(@QueryParam("type") String type) {
    return portManagers.get(type).getFreePorts();
  }

  // Jersey 1 does not support list of primary types
  @GET
  @Path("/legacy/free")
  @Produces({ MediaType.TEXT_PLAIN })
  public String getFreePortsLegacy(@QueryParam("type") String type) {
    Type listType = new TypeToken<ArrayList<Integer>>(){}.getType();
    return new GsonBuilder().create().toJson(portManagers.get(type).getFreePorts(), listType);
  }

  @GET
  @Path("/acquire")
  @Produces({ MediaType.TEXT_PLAIN })
  public Integer acquire(@QueryParam("type") String type) {
    return portManagers.get(type).acquire();
  }

  // Jersey 1 does not support primary types
  @GET
  @Path("/legacy/acquire")
  @Produces({ MediaType.TEXT_PLAIN })
  public String acquireLegacy(@QueryParam("type") String type) {
    return String.valueOf(portManagers.get(type).acquire());
  }

  @PUT
  @Path("/recycle")
  public void recycle(@QueryParam("type") String type, @QueryParam("port") Integer port) {
    portManagers.get(type).recycle(port);
  }
}
