package ai.platon.pulsar.common.proxy

import ai.platon.pulsar.common.config.ImmutableConfig
import org.apache.commons.io.FileUtils
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.nio.file.Files
import java.time.Duration

class FileProxyLoaderTest {
    
    private lateinit var tempDir: java.nio.file.Path
    private lateinit var fileProxyLoader: FileProxyLoader
    private lateinit var conf: ImmutableConfig
    
    @BeforeEach
    fun setUp() {
        conf = Mockito.mock(ImmutableConfig::class.java)
        fileProxyLoader = FileProxyLoader(conf)
        // 创建一个临时目录并添加有效的代理文件
        tempDir = Files.createTempDirectory("proxy")
        fileProxyLoader.proxyDir = tempDir
    }
    
    @AfterEach
    fun tearDown() {
        // 清理临时目录
        FileUtils.deleteDirectory(tempDir.toFile())
    }
    
    @Test
    fun testUpdateProxiesWithValidFiles() {
        // 创建一个临时目录并添加有效的代理文件
        val validProxyFile = Files.createFile(tempDir.resolve("valid_proxy.txt"))
        Files.write(validProxyFile, listOf("127.0.0.1:8080"))
        
        val proxies = fileProxyLoader.updateProxies(Duration.ZERO)
        assertEquals(1, proxies.size)
        assertEquals("127.0.0.1:8080", proxies[0].hostPort)
    }
    
    @Test
    fun testUpdateProxiesWithInvalidFiles() {
        // 创建一个临时目录并添加无效的代理文件
        val invalidProxyFile = Files.createFile(tempDir.resolve("invalid_proxy.txt"))
        Files.write(invalidProxyFile, listOf("invalid_proxy"))
        
        val proxies = fileProxyLoader.updateProxies(Duration.ZERO)
        assertTrue(proxies.isEmpty())
    }
    
    @Test
    fun testUpdateProxiesWithNoFiles() {
        // 创建一个空的临时目录
        val proxies = fileProxyLoader.updateProxies(Duration.ZERO)
        assertTrue(proxies.isEmpty())
    }
    
    @Test
    fun testLoadProxiesWithValidFiles() {
        val validProxyFile = Files.createFile(tempDir.resolve("valid_proxy.txt"))
        Files.write(validProxyFile, listOf("127.0.0.1:8080"))
        
        val proxies = fileProxyLoader.loadProxies(Duration.ZERO)
        assertEquals(1, proxies.size)
        assertEquals("127.0.0.1:8080", proxies[0].hostPort)
    }
    
    @Test
    fun testLoadProxiesWithInvalidFiles() {
        // 创建一个临时目录并添加无效的代理文件
        val invalidProxyFile = Files.createFile(tempDir.resolve("invalid_proxy.txt"))
        Files.write(invalidProxyFile, listOf("invalid_proxy"))
        
        val proxies = fileProxyLoader.loadProxies(Duration.ofMinutes(1))
        assertTrue(proxies.isEmpty())
    }
}
