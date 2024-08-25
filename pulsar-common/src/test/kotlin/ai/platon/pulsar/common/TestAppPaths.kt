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
import org.junit.jupiter.api.BeforeEach
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
        System.clearProperty(CapabilityTypes.APP_TMP_BASE_DIR_KEY)
        System.clearProperty(CapabilityTypes.APP_NAME_KEY)
        System.clearProperty(CapabilityTypes.APP_ID_KEY)
    }
    
    @AfterTest
    fun tearDown() {
        Files.deleteIfExists(Paths.get("$home/prometheus"))
        
        System.clearProperty(CapabilityTypes.APP_TMP_DIR_KEY)
        System.clearProperty(CapabilityTypes.APP_TMP_BASE_DIR_KEY)
        System.clearProperty(CapabilityTypes.APP_NAME_KEY)
        System.clearProperty(CapabilityTypes.APP_ID_KEY)
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
        
        val tmpBaseDir = Paths.get(tmp, "prometheus").toString()
        System.setProperty(CapabilityTypes.APP_TMP_BASE_DIR_KEY, tmpBaseDir)
        assertEquals("$tmpBaseDir$sep$appName", AppContext.APP_TMP_DIR_RT.toString())
        assertEquals("$tmpBaseDir$sep$appName-$ident", AppContext.APP_PROC_TMP_DIR_RT.toString())
        
        val expectedDataDir = Paths.get(home, "prometheus", ".pulsar").toString()
        System.setProperty(CapabilityTypes.APP_DATA_DIR_KEY, expectedDataDir)
        assertEquals(expectedDataDir, AppContext.APP_DATA_DIR_RT.toString())
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
    fun testGetPaths() {
        val filename = "finish_job-1217.20347.sh"
        
        var path = AppPaths.getTmp("scripts", filename)
        var path2 = path
        assertTrue(path.startsWith(AppPaths.TMP_DIR), "$path -> $path2")
        
        path = AppPaths.get("scripts", filename)
        path2 = AppPaths.get("scripts", filename)
        assertTrue(path2.startsWith(AppPaths.DATA_DIR), "$path -> $path2")
        
        path = AppPaths.SCRIPT_DIR
        
        path2 = AppPaths.getTmp(path.toString(), filename)
        // TODO: fix me: assertTrue(path2.startsWith(AppPaths.TMP_DIR))
        // assertTrue(path2.startsWith(AppPaths.TMP_DIR), "$path -> $path2")
        
        assertTrue(path2.startsWith(AppPaths.PROC_TMP_DIR), "$path -> $path2")
        
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
    
    
    
    
    @BeforeEach
    fun setUp() {
        // Any setup can be placed here if needed in future
    }
    
    @Test
    fun testResolve() {
        val base = Paths.get("base")
        val resolved = AppPaths.resolve(base, "first", "second", "third")
        assertEquals(Paths.get("base/first/second/third"), resolved)
    }
    
    @Test
    fun testGet() {
        val path = AppPaths.get("first", "second", "third")
        assertEquals(Paths.get(AppPaths.DATA_DIR.toString(), "first/second/third"), path)
    }
    
    @Test
    fun testGetTmp() {
        val path = AppPaths.getTmp("first", "second", "third")
        assertEquals(Paths.get(AppPaths.TMP_DIR.toString(), "first/second/third"), path)
    }
    
    @Test
    fun testGetRandomTmp() {
        val path = AppPaths.getRandomTmp("prefix-", ".suffix")
        assertTrue(path.startsWith(AppPaths.TMP_DIR))
        assertTrue(path.toString().endsWith(".suffix"))
    }
    
    @Test
    fun testGetProcTmp() {
        val path = AppPaths.getProcTmp("first", "second", "third")
        assertEquals(Paths.get(AppPaths.PROC_TMP_DIR.toString(), "first/second/third"), path)
    }
    
    @Test
    fun testGetProcTmpTmp() {
        val path = AppPaths.getProcTmpTmp("first", "second", "third")
        assertEquals(Paths.get(AppPaths.PROC_TMP_DIR.resolve("tmp").toString(), "first/second/third"), path)
    }
    
    @Test
    fun testGetRandomProcTmpTmp() {
        val path = AppPaths.getRandomProcTmpTmp("prefix-", ".suffix")
        assertTrue(path.startsWith(AppPaths.PROC_TMP_DIR.resolve("tmp")))
        assertTrue(path.toString().endsWith(".suffix"))
    }
    
    @Test
    fun testRandom() {
        val randomStr = AppPaths.random("prefix-", ".suffix")
        assertTrue(randomStr.startsWith("prefix-"))
        assertTrue(randomStr.endsWith(".suffix"))
    }
    
    @Test
    fun testHex() {
        val uri = "http://example.com/test"
        val hexStr = AppPaths.hex(uri, "prefix-", ".suffix")
        assertEquals("prefix-44739ab8292f3d29beb6975ac3207e46.suffix", hexStr)
    }
    
    @Test
    fun testFileId() {
        val uri = "http://example.com/test"
        val fileId = AppPaths.fileId(uri)
        assertEquals("44739ab8292f3d29beb6975ac3207e46", fileId)
    }
    
    @Test
    fun testMockPagePath() {
        val uri = "http://example.com/test"
        val path = AppPaths.mockPagePath(uri)
        assertEquals(Paths.get(AppPaths.LOCAL_TEST_WEB_PAGE_DIR.toString(), "example-com-44739ab8292f3d29beb6975ac3207e46.htm"), path)
    }
    
    @Test
    fun testFromDomain_String() {
        val domain = AppPaths.fromDomain("http://example.com/test")
        assertEquals("example-com", domain)
    }
    
    @Test
    fun testFromUri() {
        val uri = "http://example.com/test"
        val filename = AppPaths.fromUri(uri)
        assertEquals("example-com-44739ab8292f3d29beb6975ac3207e46", filename)
    }
    
    @Test
    fun testUniqueSymbolicLinkForUri() {
        val uri = "http://example.com/test"
        val link = AppPaths.uniqueSymbolicLinkForUri(uri)
        assertEquals(Paths.get(AppPaths.SYS_TMP_LINKS_DIR.toString(), "44739ab8292f3d29beb6975ac3207e46.htm"), link)
    }
}
