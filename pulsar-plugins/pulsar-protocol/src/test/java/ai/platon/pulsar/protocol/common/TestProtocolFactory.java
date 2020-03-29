/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.platon.pulsar.protocol.common;

import ai.platon.pulsar.common.config.ImmutableConfig;
import ai.platon.pulsar.crawl.protocol.ProtocolFactory;
import ai.platon.pulsar.protocol.browser.BrowserEmulatorProtocol;
import ai.platon.pulsar.protocol.crowd.ForwardingProtocol;
import ai.platon.pulsar.protocol.http.Http;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Unit test for new protocol plugin.
 */
public class TestProtocolFactory {

    private ImmutableConfig conf;
    private ProtocolFactory protocolFactory;

    /**
     * Inits the Test Case with the test parse-plugin file
     */
    @Before
    public void setUp() throws Exception {
        conf = new ImmutableConfig();
        protocolFactory = new ProtocolFactory(conf);
    }

    @Test
    public void testGetProtocol() throws Exception {
        assertEquals(Http.class.getName(),
                protocolFactory.getProtocol("http://example.com").getClass().getName());
        assertEquals(Http.class.getName(),
                protocolFactory.getProtocol("https://example.com").getClass().getName());
        assertEquals(ForwardingProtocol.class.getName(),
                protocolFactory.getProtocol("crowd:http://example.com").getClass().getName());
        assertEquals(BrowserEmulatorProtocol.class.getName(),
                protocolFactory.getProtocol("selenium:http://example.com").getClass().getName());
    }
}
