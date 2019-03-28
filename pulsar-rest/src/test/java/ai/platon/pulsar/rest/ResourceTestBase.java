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

import ai.platon.pulsar.rest.rpc.PageResourceReference;
import ai.platon.pulsar.common.config.ImmutableConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import javax.ws.rs.core.Application;

public class ResourceTestBase extends JerseyTest {
  public static final Logger LOG = LoggerFactory.getLogger(ResourceTestBase.class);

  protected static final String seedUrl = "http://news.cqnews.net/html/2017-06/08/content_41874417.htm";
  protected static final String detailUrl = "http://news.163.com/17/0607/21/CMC14QCD000189FH.html";
  protected ImmutableConfig conf;

  /**
   * TODO : create a mock site
   */
  protected PageResourceReference pageResourceReference;

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();

    pageResourceReference = new PageResourceReference(getBaseUri(), client());
    pageResourceReference.fetch(seedUrl);
    pageResourceReference.fetch(detailUrl);
  }

  @Override
  protected Application configure() {
    ApplicationContext applicationContext = new ClassPathXmlApplicationContext("classpath:/rest-context/rest-test-context.xml");
    MasterApplication application = applicationContext.getBean(MasterApplication.class);
    application.property("contextConfig", applicationContext);

    this.conf = applicationContext.getBean(ImmutableConfig.class);

    return application;
  }

  @After
  @Override
  public void tearDown() throws Exception {
    pageResourceReference.delete(seedUrl);
    pageResourceReference.delete(detailUrl);
    super.tearDown();
  }
}
