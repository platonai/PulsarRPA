package ai.platon.pulsar.browser.driver.chrome

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.mockito.Mockito.*
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import com.github.kklisura.cdt.protocol.v2023.commands.Page
import com.github.kklisura.cdt.protocol.v2023.commands.DOM
import com.github.kklisura.cdt.protocol.v2023.commands.Runtime
import com.github.kklisura.cdt.protocol.v2023.types.dom.Node
import com.github.kklisura.cdt.protocol.v2023.types.runtime.RemoteObject
import ai.platon.pulsar.browser.common.ScriptConfuser

/**
 * Tests for backend node ID support in PageHandler selectors.
 * 
 * This test validates the new feature that allows selectors in the format:
 * "backend:123" where 123 is a backend node ID.
 */
class PageHandlerBackendNodeIdTests {
    
    @Test
    fun `test backend node ID selector format parsing`() {
        val devTools = mock(RemoteDevTools::class.java)
        val confuser = mock(ScriptConfuser::class.java)
        val domAPI = mock(DOM::class.java)
        val runtimeAPI = mock(Runtime::class.java)
        
        whenever(devTools.isOpen).thenReturn(true)
        whenever(devTools.dom).thenReturn(domAPI)
        whenever(devTools.runtime).thenReturn(runtimeAPI)
        
        val pageHandler = PageHandler(devTools, confuser)
        
        // Test valid backend node ID format
        val backendNodeId = 123
        val selector = "backend:$backendNodeId"
        
        // Mock the resolution process
        val remoteObject = mock(RemoteObject::class.java)
        whenever(remoteObject.objectId).thenReturn("object-id-123")
        whenever(domAPI.resolveNode(null, backendNodeId, null, null)).thenReturn(remoteObject)
        whenever(domAPI.requestNode("object-id-123")).thenReturn(456)
        
        val nodeId = pageHandler.querySelector(selector)
        
        assertEquals(456, nodeId)
        verify(domAPI).resolveNode(null, backendNodeId, null, null)
        verify(domAPI).requestNode("object-id-123")
        verify(runtimeAPI).releaseObject("object-id-123")
    }
    
    @Test
    fun `test regular CSS selector still works`() {
        val devTools = mock(RemoteDevTools::class.java)
        val confuser = mock(ScriptConfuser::class.java)
        val domAPI = mock(DOM::class.java)
        val document = mock(Node::class.java)
        
        whenever(devTools.isOpen).thenReturn(true)
        whenever(devTools.dom).thenReturn(domAPI)
        whenever(domAPI.document).thenReturn(document)
        whenever(document.nodeId).thenReturn(1)
        whenever(domAPI.querySelector(1, "div.test")).thenReturn(789)
        
        val pageHandler = PageHandler(devTools, confuser)
        
        val nodeId = pageHandler.querySelector("div.test")
        
        assertEquals(789, nodeId)
        verify(domAPI).querySelector(1, "div.test")
    }
    
    @Test
    fun `test invalid backend node ID format returns null`() {
        val devTools = mock(RemoteDevTools::class.java)
        val confuser = mock(ScriptConfuser::class.java)
        
        whenever(devTools.isOpen).thenReturn(true)
        
        val pageHandler = PageHandler(devTools, confuser)
        
        // Test invalid formats
        val invalidSelectors = listOf(
            "backend:",           // missing ID
            "backend:abc",        // non-numeric ID
            "backend:12.34",      // decimal ID
            "backend:-1"          // negative ID (might be valid, but good to test)
        )
        
        invalidSelectors.forEach { selector ->
            val nodeId = pageHandler.querySelector(selector)
            assertNull(nodeId, "Selector '$selector' should return null")
        }
    }
    
    @Test
    fun `test focusOnSelector with backend node ID`() {
        val devTools = mock(RemoteDevTools::class.java)
        val confuser = mock(ScriptConfuser::class.java)
        val domAPI = mock(DOM::class.java)
        val runtimeAPI = mock(Runtime::class.java)
        
        whenever(devTools.isOpen).thenReturn(true)
        whenever(devTools.dom).thenReturn(domAPI)
        whenever(devTools.runtime).thenReturn(runtimeAPI)
        
        val pageHandler = PageHandler(devTools, confuser)
        
        val backendNodeId = 999
        val selector = "backend:$backendNodeId"
        
        // Mock the resolution
        val remoteObject = mock(RemoteObject::class.java)
        whenever(remoteObject.objectId).thenReturn("obj-999")
        whenever(domAPI.resolveNode(null, backendNodeId, null, null)).thenReturn(remoteObject)
        whenever(domAPI.requestNode("obj-999")).thenReturn(555)
        
        val resultNodeId = pageHandler.focusOnSelector(selector)
        
        assertEquals(555, resultNodeId)
        verify(domAPI).focus(555, null, null)
        verify(runtimeAPI).releaseObject("obj-999")
    }
    
    @Test
    fun `test querySelectorAll with backend node ID returns single element list`() {
        val devTools = mock(RemoteDevTools::class.java)
        val confuser = mock(ScriptConfuser::class.java)
        val domAPI = mock(DOM::class.java)
        val runtimeAPI = mock(Runtime::class.java)
        
        whenever(devTools.isOpen).thenReturn(true)
        whenever(devTools.dom).thenReturn(domAPI)
        whenever(devTools.runtime).thenReturn(runtimeAPI)
        
        val pageHandler = PageHandler(devTools, confuser)
        
        val backendNodeId = 777
        val selector = "backend:$backendNodeId"
        
        // Mock the resolution
        val remoteObject = mock(RemoteObject::class.java)
        whenever(remoteObject.objectId).thenReturn("obj-777")
        whenever(domAPI.resolveNode(null, backendNodeId, null, null)).thenReturn(remoteObject)
        whenever(domAPI.requestNode("obj-777")).thenReturn(888)
        
        val nodeIds = pageHandler.querySelectorAll(selector)
        
        assertEquals(1, nodeIds.size)
        assertEquals(888, nodeIds[0])
    }
}
