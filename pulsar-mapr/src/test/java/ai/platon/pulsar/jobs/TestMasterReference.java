package ai.platon.pulsar.jobs;

import ai.platon.pulsar.common.config.ImmutableConfig;
import ai.platon.pulsar.jobs.fetch.service.jersey1.MasterReference;
import ai.platon.pulsar.jobs.fetch.service.jersey1.ServerInstance;
import com.beust.jcommander.internal.Lists;
import com.sun.jersey.api.client.filter.LoggingFilter;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by vincent on 17-5-2.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
public class TestMasterReference {
  private final MasterReference masterReference;

  public TestMasterReference() {
    masterReference = new MasterReference(new ImmutableConfig());
    masterReference.addFilter(new LoggingFilter(System.out));
  }

  /**
   * Check PMaster is available.
   * NOTICE : MasterReference is compatible with PMaster in class api level, but is compatible in REST api level
   * */
  public boolean checkPMaster() {
    if (!masterReference.test()) {
      System.out.println("PMaster is not available");
      return false;
    }
    return true;
  }

  @Test
  public void testPortResource() {
    if (!checkPMaster()) {
      return;
    }

    ServerInstance.Type type = ServerInstance.Type.FetchService;

    List<Integer> ports = Lists.newArrayList();
    for (int i = 0; i < 20; ++i) {
      int port = masterReference.acquirePort(type);
      // assertEquals(21000 + i, port);
      ports.add(port);
    }

    List<Integer> freePorts = Lists.newArrayList();
    for (int i = 19; i >= 10; --i) {
      int port = ports.get(i);
      freePorts.add(port);
      masterReference.recyclePort(type, port);
      assertTrue(port > 21000);
    }

    List<Integer> freePorts2 = masterReference.getFreePorts(type);
    assertTrue(CollectionUtils.containsAll(freePorts2, freePorts));

//    System.out.println(freePorts);
//    System.out.println(freePorts2);
  }

  @Test
  public void testServerInsanceResourceEcho() {
    if (!checkPMaster()) {
      return;
    }

    ServerInstance serverInstance = new ServerInstance("126.1.1.7", 21000, ServerInstance.Type.FetchService.name());
    serverInstance = masterReference.echo(serverInstance);
    assertEquals(21000, serverInstance.getPort());
    assertEquals("126.1.1.7", serverInstance.getIp());
  }

  @Test
  public void testRegisterServerInsance() {
    if (!checkPMaster()) {
      return;
    }

    for (int i = 0; i < 10; ++i) {
      int port = 21000 + i;

      ServerInstance serverInstance = new ServerInstance("", port, ServerInstance.Type.FetchService.name());
      serverInstance = masterReference.register(serverInstance);
      // System.out.println(serverInstance);
      assertEquals(port, serverInstance.getPort());
//      assertEquals("127.0.0.1", serverInstance.getIp());

      serverInstance = masterReference.unregister(serverInstance.getId());
      assertEquals(port, serverInstance.getPort());
//      assertEquals("127.0.0.1", serverInstance.getIp());

      serverInstance = masterReference.unregister(100000);
    }
  }
}
