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
package ai.platon.pulsar.parse.tika

import ai.platon.pulsar.crawl.parse.ParseException
import ai.platon.pulsar.crawl.protocol.ProtocolException
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test
import java.io.IOException

@Ignore("Image parser is not required currently")
class TestImageMetadata : TikaTestBase() {
    // Make sure sample files are copied to "test.data" as specified in
    private val sampleFiles = arrayOf("pulsar_logo_tm.gif")

    @Test
    @Throws(ProtocolException::class, ParseException::class, IOException::class)
    fun testIt() {
        for (sampleFile in sampleFiles) {
            val page = parse(sampleFile)
            Assert.assertEquals("121", page.metadata["width"])
            Assert.assertEquals("48", page.metadata["height"])
        }
    }
}