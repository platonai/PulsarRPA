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

import ai.platon.pulsar.rest.model.response.LinkDatum;
import ai.platon.pulsar.rest.rpc.MasterReference;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static ai.platon.pulsar.common.config.PulsarConstants.YES_STRING;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Ignore("Test can not pass because api changes")
public class JTestSeedResource extends ResourceTestBase {

  private String seedUrl = "http://news.china.com/zh_cn/social/index.html";
  private String seedUrlToTestMultiInject = "http://news.cqnews.net/rollnews/index_6.htm";

  private String[] allUrls = {
      seedUrl,
      seedUrlToTestMultiInject
  };

  protected MasterReference masterReference;

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();

    masterReference = new MasterReference(getBaseUri(), client());
    masterReference.inject(seedUrl);
  }

  @Test
  public void testList() {
    String result = target("seeds")
        .request()
        .accept(MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON)
        .get(String.class);

    Gson gson = new GsonBuilder().create();
    Type listType = new TypeToken<ArrayList<LinkDatum>>() {}.getType();
    ArrayList<LinkDatum> ols = gson.fromJson(result, listType);

    // LOG.debug(ols.stream().map(MyOutlink::getUrl).collect(Collectors.joining("\n")));
    assertTrue(!ols.isEmpty());
  }

  @Test
  public void testHome() {
    List<LinkDatum> links = target("seeds")
        .path("home")
        .request()
        .accept(MediaType.APPLICATION_JSON)
        .get(new GenericType<List<LinkDatum>>() {});

    LOG.debug(links.toString());

    // LOG.debug(ols.stream().map(MyOutlink::getUrl).collect(Collectors.joining("\n")));
    assertTrue(!links.isEmpty());
  }

  @Test
  public void testInject() {
    Map<String, String> statusFields = masterReference.inject(seedUrl, "");
    LOG.debug(statusFields.toString());
    assertEquals(YES_STRING, statusFields.get("metadata I_S"));
  }

  @Test
  public void testInjectOutgoingPages() {
    String result = target("seeds")
        .path("inject-out-pages")
        .queryParam("url", seedUrl)
        .queryParam("filter", "-umin 50")
        .request()
        .get(String.class);
    LOG.debug(result);
  }

  @Test
  public void testUnInject() {
    masterReference.inject(seedUrl);
    Map<String, String> statusFields = masterReference.unInject(seedUrl);
    LOG.debug(statusFields.toString());
    assertTrue(!statusFields.isEmpty());
    assertTrue(!statusFields.containsKey("metadata I_S"));
  }

  @Test
  public void testUnInjectOutgoingPages() {
    masterReference.inject(seedUrl);
    String result = target("seeds")
        .path("uninject-out-pages")
        .queryParam("url", seedUrl)
        .queryParam("filter", "-umin 50")
        .request()
        .get(String.class);
    LOG.debug(result);
  }

  @Test
  public void testSeedHome() throws InterruptedException {
    masterReference.unInject(seedUrl);

    masterReference.inject(seedUrlToTestMultiInject);
    masterReference.inject(seedUrlToTestMultiInject);
    masterReference.inject(seedUrlToTestMultiInject);
    masterReference.inject(seedUrlToTestMultiInject);

    Thread.sleep(1000);

    List<LinkDatum> seedUrls = masterReference.home();
    assertTrue(!seedUrls.isEmpty());
//    assertEquals(1, seedUrls.size());
  }

  @After
  @Override
  public void tearDown() throws Exception {
    Stream.of(allUrls).forEach(pageResourceReference::delete);
    super.tearDown();
  }
}
