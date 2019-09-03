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

import ai.platon.pulsar.common.NetUtil;
import ai.platon.pulsar.persist.rdb.model.BrowserInstance;
import ai.platon.pulsar.persist.rdb.model.ServerInstance;
import ai.platon.pulsar.rest.api.service.BrowserInstanceService;
import ai.platon.pulsar.rest.api.service.ServerInstanceService;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static ai.platon.pulsar.persist.rdb.model.ServerInstance.Type.FetchService;

/**
 * Fetcher Server Resource
 * */
@RestController
@RequestMapping("/service")
public class ServiceResource {

  public static final Logger LOG = LoggerFactory.getLogger(ServiceResource.class);

  private ServerInstanceService serverInstanceService;

  private BrowserInstanceService browserInstanceService;

  private static long lastCheckAvailableTime = System.currentTimeMillis();

  // @Inject
  @Autowired
  public ServiceResource(
          ServerInstanceService serverInstanceService,
          BrowserInstanceService browserInstanceService) {
    this.serverInstanceService = serverInstanceService;
    this.browserInstanceService = browserInstanceService;
  }

  /**
   * For test
   * Register pulsar relative server
   * */
  @PostMapping("/echo")
  public ServerInstance echo(ServerInstance serverInstance) {
    return serverInstance;
  }

  /**
   * For test
   * Return a list of integer
   * NOTE: return of a list of integers is not supported by jersey-2.26-b03, use string instead
   * */
  @GetMapping("/listOfInteger")
  public List<Integer> getListOfInteger() {
    return IntStream.range(0, 10).boxed().collect(Collectors.toList());
  }

  /**
   * List all servers instances
   * */
  @GetMapping
  public List<ServerInstance> list() {
    return serverInstanceService.list();
  }

  /**
   * List all servers instances
   * */
  @PutMapping("/{browserId}")
  public List<ServerInstance> getLiveServerInstances(@PathVariable("browserId") long browserId, String password) {
    if (browserInstanceService.authorize(browserId, password)) {
      return getLiveServerInstances(FetchService);
    }

    return Lists.newArrayList();
  }

  @PostMapping("/login")
  public BrowserInstance report(HttpServletRequest request, BrowserInstance browserInstance) {
    if (browserInstance.getId() == null) {
      browserInstance.setCreated(Instant.now());
    }

    if (request != null) {
      browserInstance.setIp(request.getRemoteAddr());
    }

    // browserInstance.setUserAgent();
    browserInstance.setSesssion("session");
    browserInstance.setModified(Instant.now());
    browserInstanceService.save(browserInstance);

    return browserInstance;
  }

  /**
   * Register pulsar relative server
   *
   * From jersey document:
   * When deploying a JAX-RS application using servlet then
   * ServletConfig, ServletContext, HttpServletRequest and HttpServletResponse
   * are available using @Context.
   *
   * But it seems there is nothing to inject when we run tests using provider
   * jersey-test-framework-provider-inmemory
   * */
  @PostMapping("/register")
  public ServerInstance register(HttpServletRequest request, ServerInstance serverInstance) {
    // In test mode,
    if (request != null) {
      serverInstance.setIp(request.getRemoteAddr());
      LOG.debug(request.getRemoteAddr());
    }

    return serverInstanceService.register(serverInstance);
  }

  /**
   * Unregister pulsar relative server
   * */
  @DeleteMapping("/unregister/{id}")
  public ServerInstance unregister(@PathVariable("id") Long id) {
    ServerInstance instance = serverInstanceService.get(id);
    if (instance != null) {
      serverInstanceService.unregister(id);
    }
    return instance;
  }

  private List<ServerInstance> getLiveServerInstances(ServerInstance.Type type) {
    List<ServerInstance> serverInstances = serverInstanceService.list(type);

    long now = System.currentTimeMillis();
    int checkPeriod = 15;
    if (now - lastCheckAvailableTime > checkPeriod * 1000) {
      List<ServerInstance> serverInstances2 = Lists.newArrayList();

      // LOG.debug("Service resource check time : " + Request.getCurrent().getDate());

      for (ServerInstance serverInstance : serverInstances) {
        if (!NetUtil.testNetwork(serverInstance.getIp(), serverInstance.getPort())) {
          serverInstanceService.unregister(serverInstance.getId());
        }
        else {
          serverInstances2.add(serverInstance);
        }
      }

      lastCheckAvailableTime = now;

      return serverInstances2;
    }

    return serverInstances;
  }
}
