package ai.platon.pulsar.common

import ai.platon.pulsar.common.AppPaths.fromHost
import ai.platon.pulsar.common.AppPaths.fromUri
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.net.URL

class AppPathsTest {
    
    @Test
    fun `test fromHost with URL`() {
        // 正常情况
        assertEquals("baidu-com", fromHost(URL("http://www.baidu.com")))
        assertEquals("taobao-com", fromHost(URL("https://www.taobao.com")))
        
        // IP 地址
        assertEquals("127-0-0-1", fromHost(URL("http://127.0.0.1")))
        
        // IP 地址带端口
        assertEquals("192-168-1-1", fromHost(URL("http://192.168.1.1:8080")))
        
        // 本地主机
        assertEquals("localhost", fromHost(URL("http://localhost")))
        
        // 无效 URL
        assertEquals("unknown", fromHost(URL("http://invalid.url")))
    }
    
    @Test
    fun `test fromHost with String`() {
        // 正常情况
        assertEquals("baidu-com", fromHost("http://www.baidu.com"))
        assertEquals("taobao-com", fromHost("https://www.taobao.com"))
        assertEquals("ebay-com", fromHost("https://www.ebay.com"))
        
        // 无效 URL
        assertEquals("unknown", fromHost("invalid-url"))
    }
    
    @Test
    fun `test fromUri`() {
        // 正常情况
        assertEquals("pre-baidu-com-ddcd696103c7865a3301ac293b27c55c-post", fromUri("http://www.baidu.com/some/path?query=param", "pre-", "-post"))
        
        // 无效 URL
        assertEquals("pre-unknown-post", fromUri("invalid-url", "pre-", "-post"))
        
        // 带前缀和后缀
        assertEquals(
            "pre-baidu-com-ddcd696103c7865a3301ac293b27c55c-post",
            fromUri("http://www.baidu.com/some/path?query=param", "pre-", "-post")
        )
        
        // 只有前缀
        assertEquals("pre-baidu-com-ddcd696103c7865a3301ac293b27c55c", fromUri("http://www.baidu.com/some/path?query=param", "pre-"))
        
        // 只有后缀
        assertEquals("baidu-com-ddcd696103c7865a3301ac293b27c55c-post", fromUri("http://www.baidu.com/some/path?query=param", "", "-post"))
    }
}
