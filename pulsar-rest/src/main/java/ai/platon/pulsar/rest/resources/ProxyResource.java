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

import ai.platon.pulsar.common.config.ImmutableConfig;
import ai.platon.pulsar.persist.metadata.MultiMetadata;
import ai.platon.pulsar.persist.metadata.SpellCheckedMultiMetadata;
import ai.platon.pulsar.rest.model.request.ProxyConfig;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Proxy Resource module
 * */
@RestController
@RequestMapping("/proxy")
public class ProxyResource {
  public static final Logger LOG = LoggerFactory.getLogger(ProxyResource.class);

  @Autowired
  private ImmutableConfig immutableConfig;

  private TestAndSaveThread testAndSaveThread = null;

  /**
   * List all proxy servers
   * */
  @GetMapping
  public List<String> list() {
    // return ProxyPool.getInstance(immutableConfig)();
    return new ArrayList<>();
  }

  @PutMapping("/echo")
  public byte[] echo(@RequestHeader Map<String, String> headers, byte[] content) {
    MultiMetadata customHeaders = new SpellCheckedMultiMetadata();
    for (java.util.Map.Entry<String, String> entry : headers.entrySet()) {
      customHeaders.put(entry.getKey(), entry.getValue());
    }

    LOG.info("{}", headers.entrySet());
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
  @GetMapping("/touch")
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
  @PostMapping("/report")
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
