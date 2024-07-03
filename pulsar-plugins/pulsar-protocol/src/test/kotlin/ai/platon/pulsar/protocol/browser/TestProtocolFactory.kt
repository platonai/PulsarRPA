/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.platon.pulsar.protocol.browser

import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.skeleton.crawl.protocol.ProtocolFactory
import ai.platon.pulsar.protocol.crowd.ForwardingProtocol
import kotlin.test.*
import kotlin.test.assertEquals

/**
 * Unit test for new protocol plugin.
 */
class TestProtocolFactory {
    private var conf = ImmutableConfig()
    private var protocolFactory = ProtocolFactory(conf)
    /**
     * Inits the Test Case with the test parse-plugin file
     */
    @BeforeTest
    @Throws(Exception::class)
    fun setUp() {
    }

    @Test
    @Throws(Exception::class)
    fun testGetProtocol() {
        //        assertEquals(Http.class.getName(),
//                protocolFactory.getProtocol("http://example.com").getClass().getName());
//        assertEquals(Http.class.getName(),
//                protocolFactory.getProtocol("https://example.com").getClass().getName());
        assertEquals(ForwardingProtocol::class.java.name,
                protocolFactory.getProtocol("crowd:http://example.com")?.javaClass?.name)
        assertEquals(BrowserEmulatorProtocol::class.java.name,
                protocolFactory.getProtocol("browser:http://example.com")?.javaClass?.name)
    }
}
