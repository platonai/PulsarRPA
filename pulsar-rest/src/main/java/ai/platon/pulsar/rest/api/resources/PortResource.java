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
package ai.platon.pulsar.rest.api.resources;

import ai.platon.pulsar.persist.rdb.model.ServerInstance;
import ai.platon.pulsar.rest.api.service.PortManager;
import com.google.common.collect.Maps;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@RestController
@RequestMapping("/port")
public class PortResource {

  private final Map<String, PortManager> portManagers = Maps.newConcurrentMap();

  public PortResource() {
    PortManager portManager = new PortManager(ServerInstance.Type.FetchService.name());
    portManagers.put(portManager.getType(), portManager);

    portManager = new PortManager(ServerInstance.Type.PulsarMaster.name());
    portManagers.put(portManager.getType(), portManager);
  }

  @GetMapping("/get-empty-port-manager")
  public PortManager getManager(@PathVariable("type") String type) {
    return new PortManager(type, 0, 0);
  }

  // TODO : Failed to return a list of integer, we do not if it's a jersey bug or dependency issue
  @GetMapping("/listOfInteger")
  public List<Integer> getListOfInteger(@PathVariable("type") String type) {
    return IntStream.range(0, 10).mapToObj(Integer::new).collect(Collectors.toList());
  }

  @GetMapping("/report")
  public String report() {
    String report = portManagers.values().stream()
        .map(p -> "\tPorts for " + p.getType() + " :\n" + p.toString())
        .collect(Collectors.joining("\n"));
    return "PortResource #" + hashCode() + report;
  }

  // TODO : Failed to return a list of integer, we do not if it's a jersey bug or dependency issue
  @GetMapping("/active")
  public List<Integer> activePorts(@PathVariable("type") String type) {
    return portManagers.get(type).getActivePorts();
  }

  @GetMapping("/legacy/active")
  public String getActivePortsLegacy(@PathVariable("type") String type) {
    Type listType = new TypeToken<ArrayList<Integer>>(){}.getType();
    return new GsonBuilder().create().toJson(portManagers.get(type).getActivePorts(), listType);
  }

  @GetMapping("/free")
  public List<Integer> getFreePorts(@PathVariable("type") String type) {
    return portManagers.get(type).getFreePorts();
  }

  // Jersey 1 does not support list of primary types
  @GetMapping("/legacy/free")
  public String getFreePortsLegacy(@PathVariable("type") String type) {
    Type listType = new TypeToken<ArrayList<Integer>>(){}.getType();
    return new GsonBuilder().create().toJson(portManagers.get(type).getFreePorts(), listType);
  }

  @GetMapping("/acquire")
  public Integer acquire(@PathVariable("type") String type) {
    return portManagers.get(type).acquire();
  }

  @GetMapping("/legacy/acquire")
  public String acquireLegacy(@PathVariable("type") String type) {
    return String.valueOf(portManagers.get(type).acquire());
  }

  @PutMapping("/recycle")
  public void recycle(@PathVariable("type") String type, @PathVariable("port") Integer port) {
    portManagers.get(type).recycle(port);
  }
}
