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
import ai.platon.pulsar.common.config.CapabilityTypes
import org.apache.commons.lang3.SystemUtils
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.random.Random
import kotlin.test.*

class TestAppPaths {
    private val tmpDirStr get() = AppPaths.TMP_DIR.toString()
    private val homeDirStr get() = AppPaths.DATA_DIR.toString()

    val home = SystemUtils.USER_HOME
    val tmp = SystemUtils.JAVA_IO_TMPDIR
    val appName = AppContext.APP_NAME
    val ident = AppContext.APP_IDENT
    val sep = File.separatorChar

    @BeforeTest
    fun setup() {
        System.clearProperty(CapabilityTypes.APP_TMP_DIR_KEY)
        System.clearProperty(CapabilityTypes.APP_NAME_KEY)
        System.clearProperty(CapabilityTypes.APP_ID_KEY)
    }

    @AfterTest
    fun tearDown() {
        Files.deleteIfExists(Paths.get("$home/prometheus"))
    }

    @Test
    fun testAppContextDirs() {
        assertEquals("pulsar", appName)

        if (SystemUtils.IS_OS_WINDOWS) {
            assertEquals(AppContext.APP_TMP_DIR.toString(), "$tmp$appName")
            assertEquals(AppContext.APP_PROC_TMP_DIR.toString(), "$tmp$appName-$ident")
        }
        
        if (SystemUtils.IS_OS_LINUX) {
            assertEquals(AppContext.APP_TMP_DIR.toString(), "$tmp$sep$appName")
            assertEquals(AppContext.APP_PROC_TMP_DIR.toString(), "$tmp$sep$appName-$ident")
        }
        
        System.setProperty(CapabilityTypes.APP_TMP_DIR_KEY, "$home${sep}prometheus")
        assertEquals("$home${sep}prometheus$sep$appName", AppContext.APP_TMP_DIR_RT.toString())
        assertEquals("$home${sep}prometheus$sep$appName-$ident", AppContext.APP_PROC_TMP_DIR_RT.toString())
    }

    @Test
    fun testCustomAppContextDirs() {
        System.setProperty(CapabilityTypes.APP_TMP_DIR_KEY, "$home${sep}prometheus")
        System.setProperty(CapabilityTypes.APP_NAME_KEY, "amazon")
        System.setProperty(CapabilityTypes.APP_ID_KEY, "bs")

        assertEquals("$home${sep}prometheus${sep}amazon", AppContext.APP_TMP_DIR_RT.toString())
        assertEquals("$home${sep}prometheus${sep}amazon-bs", AppContext.APP_PROC_TMP_DIR_RT.toString())
    }

    @Test
    fun testPathStartWith() {
        assertTrue { AppPaths.CHROME_DATA_DIR_PROTOTYPE.startsWith(AppPaths.BROWSER_DATA_DIR) }
    }

    @Test
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
     * @see <a href='https://github.com/platonai/PulsarRPA/issues/20'>
     *     #20 Failed to create symbolic link when export webpage on Windows 11</a>
     * */
    @Test
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
                assertTrue { Files.exists(path) }
                assertFalse { Files.exists(link) }
                AppFiles.createSymbolicLink(link, path)
                
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
    fun testFromDomain() {
        assertTrue { Strings.isIpLike("8.8.8.8") }
        assertTrue { Strings.isIpLike("127.0.0.1") }
        assertEquals("127-0-0-1", AppPaths.fromDomain("https://127.0.0.1/a/b/c?t=1&k=2"))
        assertEquals("localhost", AppPaths.fromDomain("https://localhost/a/b/c?t=1&k=2#domain"))
        assertEquals("baidu-com", AppPaths.fromDomain("https://baidu.com/a/b/c?t=1&k=2#domain"))
    }
}
