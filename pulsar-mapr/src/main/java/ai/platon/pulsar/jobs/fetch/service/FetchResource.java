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

import ai.platon.pulsar.crawl.fetch.FetchJobForwardingResponse;
import ai.platon.pulsar.crawl.fetch.FetchTask;
import ai.platon.pulsar.crawl.fetch.TaskScheduler;
import ai.platon.pulsar.crawl.fetch.TaskSchedulers;
import ai.platon.pulsar.persist.metadata.MultiMetadata;
import ai.platon.pulsar.persist.metadata.SpellCheckedMultiMetadata;
import com.google.common.collect.Lists;
import com.sun.jersey.spi.resource.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

@Component
@Singleton
@Path(value = "/fetch")
@Produces({ MediaType.APPLICATION_JSON })
public class FetchResource {

  public static final Logger LOG = LoggerFactory.getLogger(FetchServer.class);

  public final static int MAX_TASKS_PER_SCHEDULE = 100;

  @Autowired
  private TaskSchedulers taskSchedulers;

  public FetchResource() {
    // taskSchedulers =
  }

  @GET
  @Path("/schedule/{count}")
  public List<FetchTask.Key> getFetchItems(@PathParam("count") int count) {
    List<FetchTask.Key> keys = Lists.newArrayList();

    if (count < 0) {
      LOG.debug("Invalid count " + count);
      return keys;
    }

    if (count > MAX_TASKS_PER_SCHEDULE) {
      count = MAX_TASKS_PER_SCHEDULE;
    }

    return taskSchedulers.randomFetchItems(count);
  }

  /**
   * Jersey1 may not support return a list of integer
   * */
  @GET
  @Path("/scheduler/list")
  public List<Integer> listScheduers() {
    return taskSchedulers.schedulerIds();
  }

  @GET
  @Path("/scheduler/listCommands")
  public List<String> listCommands() {
    return taskSchedulers.schedulerIds().stream()
        .map(id -> "curl http://localhost:8182/fetch/stop/" + id)
        .collect(Collectors.toList());
  }

  @PUT
  @Path("/stop/{schedulerId}")
  public void stop(@PathParam("schedulerId") int schedulerId) {
    TaskScheduler taskScheduler = taskSchedulers.get(schedulerId);
    if (taskScheduler != null) {
      // taskScheduler.stop();
    }
  }

  /**
   * Accept page content from satellite(crowdsourcing web fetcher),
   * the content should be put with media getType "text/html; charset='UTF-8'"
   * 
   * TODO : does the framework make a translation between string and byte array?
   * which might affect the performance
   * */
  @PUT
  @Path("/submit")
  @Consumes("text/html; charset='UTF-8'")
  @Produces("text/html; charset='UTF-8'")
  public String finishFetchItem(@javax.ws.rs.core.Context HttpHeaders httpHeaders, byte[] content) {
    MultiMetadata customHeaders = new SpellCheckedMultiMetadata();
    for (Entry<String, List<String>> entry : httpHeaders.getRequestHeaders().entrySet()) {
      String name = entry.getKey().toLowerCase();

      // Q- means meta-data from satellite
      // F- means forwarded headers by satellite
      // ant other headers are information between satellite and this server
      if (name.startsWith("f-") || name.startsWith("q-")) {
        if (name.startsWith("f-")) {
          name = name.substring("f-".length());
        }

        for (String value : entry.getValue()) {
          customHeaders.put(name, value);
        }
      }
    }

//    log.debug("headers-1 : {}", httpHeaders.getRequestHeaders().entrySet());
//    log.debug("headers-2 : {}", customHeaders);

    FetchJobForwardingResponse fetchResult = new FetchJobForwardingResponse(customHeaders, content);
    TaskScheduler taskScheduler = taskSchedulers.get(fetchResult.getJobId());
    taskScheduler.produce(fetchResult);

    return "success";
  }
}
