/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.platon.pulsar.common

import ai.platon.pulsar.common.url.*
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class TestHyperlinks {

    @Test
    fun testEquals() {
        var u1: UrlAware = Hyperlink(uc.urlString1)
        var u2: UrlAware = Hyperlink(uc.urlString1, "hello", Int.MAX_VALUE)
        assertEquals(u1, u2)

        u1 = Hyperlink(uc.urlString1)
        u2 = Hyperlink(uc.urlString1, label = "")
        assertEquals(u1, u2 as Hyperlink)

        u1 = Hyperlink(uc.urlString1)
        u2 = StatefulHyperlink(uc.urlString1)
        assertEquals(u1, u2)

        u1 = Hyperlink(uc.urlString1)
        u2 = StatefulFatLink(uc.urlString1, tailLinks = listOf())
        assertEquals(u1, u2)

        u1 = Hyperlink(uc.urlString1)
        u2 = Hyperlink(uc.urlString2)
        assertNotEquals(u1, u2)

        assertEquals(Hyperlink(uc.urlString1), Hyperlink(uc.urlString1, label = ""))
    }
}
