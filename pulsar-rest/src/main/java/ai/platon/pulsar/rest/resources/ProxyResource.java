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

import com.google.gson.Gson;
import ai.platon.pulsar.rest.model.request.ProxyConfig;
import ai.platon.pulsar.common.config.ImmutableConfig;
import ai.platon.pulsar.persist.metadata.MultiMetadata;
import ai.platon.pulsar.persist.metadata.SpellCheckedMultiMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;

/**
 * Proxy Resource module
 * */
@Component
@Path("/proxy")
@Produces(MediaType.APPLICATION_JSON)
public class ProxyResource {
  public static final Logger LOG = LoggerFactory.getLogger(ProxyResource.class);

  @Autowired
  private ImmutableConfig immutableConfig;

  private TestAndSaveThread testAndSaveThread = null;

  /**
   * List all proxy servers
   * */
  @GET
  public List<String> list() {
    // return ProxyPool.getInstance(immutableConfig)();
    return new ArrayList<>();
  }

  @PUT
  @Path("/echo")
  @Consumes("text/html; charset='UTF-8'")
  @Produces("text/html; charset='UTF-8'")
  public byte[] echo(@Context HttpHeaders httpHeaders, byte[] content) {
    MultiMetadata customHeaders = new SpellCheckedMultiMetadata();
    for (java.util.Map.Entry<String, List<String>> entry : httpHeaders.getRequestHeaders().entrySet()) {
      for (String value : entry.getValue()) {
        customHeaders.put(entry.getKey(), value);
      }
    }

    LOG.info("{}", httpHeaders.getRequestHeaders().entrySet());
    LOG.info("{}", customHeaders);

    return content;
  }

  /**
   * download proxy list file as a text file
   * */
//  @GET
//  @Path("/download")
//  public Representation download() {
//    return new FileRepresentation(ProxyPool.ProxyListFile, org.restlet.data.MediaType.TEXT_PLAIN);
//  }

  /**
   * refresh proxy list file
   * */
  @GET
  @Path("/touch")
  public String touch() {
    // return "last modified : " + ProxyPool.getInstance();
    return "not implemented";
  }

  /**
   * manage reports from satellite
   * 
   * satellite reports itself with it's configuration file, 
   * the server side add the satellite into proxy list
   * 
   * */
  @POST
  @Path("/report")
  @Consumes(javax.ws.rs.core.MediaType.APPLICATION_JSON)
  public ArrayList<String> report(String content) {
//    String host = Request.getCurrent().getClientInfo().getAddress();
    String host = "";

        // logger.info("received report from " + host);
    // logger.debug("received content : {} ", content);

    Gson gson = new Gson();
    ProxyConfig proxyConfig = gson.fromJson(content, ProxyConfig.class);

    ArrayList<String> proxyList = new ArrayList<String>();
    if (proxyConfig != null) {
      for (int i = 0; i < proxyConfig.coordinator.proxyProcessCount; ++i) {
        int port = proxyConfig.coordinator.serverPortBase + i;
        proxyList.add(host + ":" + port);
      }
    }

    // logger.debug("received proxy server list : {} ", proxyList);

    testAndSaveThread = new TestAndSaveThread(proxyList);
    testAndSaveThread.start();

    return proxyList;
  }

  private class TestAndSaveThread extends Thread {
    private final List<String> proxyList;

    TestAndSaveThread(List<String> proxyList) {
      this.proxyList = proxyList;
    }

    @Override
    public void start() {
      setName(getClass().getSimpleName());
      setDaemon(true);
      super.start();
    }

    @Override
    public void run() {
      if (proxyList != null && !proxyList.isEmpty()) {
//        try {
//          ProxyPool.testAndSave(proxyList);
//        } catch (IOException e) {
//          LOG.error(e.toString());
//        }
      }
    }
  }
}
