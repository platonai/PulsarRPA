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
package ai.platon.pulsar.common

import java.net.InetAddress
import kotlin.test.*

/** Unit tests for StringUtil methods.  */
class TestNetUtils {
    @Test
    fun testUriBuilder() {
//    URI uri = UriBuilder.fromUri("http://www.163.com").path("q").queryParam("a", 1).queryParam("utf8", "✓").build();
//    assertEquals("http://www.163.com/q?a=1&utf8=%E2%9C%93", uri.toASCIIString());
    }

    @Test
    fun testLocalAddress() {
        val localHost = InetAddress.getLocalHost()
        println(localHost)
    }
}
