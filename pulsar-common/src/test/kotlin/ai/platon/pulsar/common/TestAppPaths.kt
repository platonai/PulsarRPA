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

import ai.platon.pulsar.common.config.AppConstants
import org.apache.commons.lang3.RandomStringUtils
import org.junit.Test
import java.io.IOException
import java.nio.file.Files
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestAppPaths {
    private val tmpDirStr get() = AppPaths.TMP_DIR.toString()
    private val homeDirStr get() = AppPaths.DATA_DIR.toString()

    @Test
    @Throws(Exception::class)
    fun testGet() {
        val filename = "finish_job-1217.20347.sh"

        var path = AppPaths.getTmp("scripts", filename)
        var path2 = path
        assertTrue(path.startsWith(AppPaths.TMP_DIR), "$path -> $path2")

        path = AppPaths.get("scripts", filename)
        path2 = AppPaths.get("scripts", filename)
        assertTrue(path2.startsWith(AppPaths.DATA_DIR), "$path -> $path2")

        path = AppPaths.SCRIPT_DIR
        path2 = AppPaths.getTmp(path.toString(), filename)
        assertTrue(path2.startsWith(AppPaths.TMP_DIR), "$path -> $path2")

        path = AppPaths.DATA_DIR.resolve("scripts")
        path2 = AppPaths.get(path.toString(), filename)
        assertTrue(path2.startsWith(AppPaths.DATA_DIR), "$path -> $path2")
    }

    /**
     * Ensure a symbolic link can be created.
     *
     * @see <a href='https://github.com/platonai/pulsarr/issues/20'>
     *     #20 Failed to create symbolic link when export webpage on Windows 11</a>
     * */
    @Test
    @Throws(Exception::class)
    fun testCreateSymbolicLink() {
        var i = 0
        val n = 50

        while(i++ < n) {
            val filename = "" + Random.nextInt(10) + ".htm"
            val url = AppConstants.EXAMPLE_URL + "/$filename"
            val path = AppPaths.getTmp(AppPaths.fromUri(url))
            Files.writeString(path, "test")

            val link = AppPaths.uniqueSymbolicLinkForUri(url)
            try {
                Files.deleteIfExists(link)
                Files.createSymbolicLink(link, path)
                assertTrue { Files.exists(link) }
            } finally {
                if (i % 2 == 0) {
                    Files.deleteIfExists(link)
                    Files.deleteIfExists(path)
                }
            }
        }
    }

    @Test
    @Throws(Exception::class)
    fun testFromDomain() {
        assertTrue { Strings.isIpLike("8.8.8.8") }
        assertTrue { Strings.isIpLike("127.0.0.1") }
        assertEquals("127-0-0-1", AppPaths.fromDomain("https://127.0.0.1/a/b/c?t=1&k=2"))
        assertEquals("localhost", AppPaths.fromDomain("https://localhost/a/b/c?t=1&k=2#domain"))
        assertEquals("baidu-com", AppPaths.fromDomain("https://baidu.com/a/b/c?t=1&k=2#domain"))
    }
}
