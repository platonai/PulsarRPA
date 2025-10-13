package ai.platon.pulsar.browser.driver.chrome

import ai.platon.pulsar.browser.driver.chrome.AccessibilityHandler.AccessibilityTreeResult
import com.github.kklisura.cdt.protocol.v2023.commands.Accessibility
import com.github.kklisura.cdt.protocol.v2023.commands.Page
import com.github.kklisura.cdt.protocol.v2023.commands.Runtime
import com.github.kklisura.cdt.protocol.v2023.types.accessibility.AXNode
import com.github.kklisura.cdt.protocol.v2023.types.accessibility.AXProperty
import com.github.kklisura.cdt.protocol.v2023.types.accessibility.AXPropertyValue
import com.github.kklisura.cdt.protocol.v2023.types.page.Frame
import com.github.kklisura.cdt.protocol.v2023.types.page.FrameTree
import com.github.kklisura.cdt.protocol.v2023.types.runtime.RemoteObject
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AccessibilityHandlerTests {

    private lateinit var devTools: RemoteDevTools
    private lateinit var accessibility: Accessibility
    private lateinit var page: Page
    private lateinit var runtime: Runtime
    private lateinit var handler: AccessibilityHandler

    @BeforeEach
    fun setup() {
        devTools = mockk(relaxed = true)
        accessibility = mockk(relaxed = true)
        page = mockk(relaxed = true)
        runtime = mockk(relaxed = true)

        every { devTools.isOpen } returns true
        every { devTools.accessibility } returns accessibility
        every { devTools.page } returns page
        every { devTools.runtime } returns runtime

        handler = AccessibilityHandler(devTools)
    }

    @Test
    fun `getFullAXTree returns correct structure and fields`() {
        // Arrange
        val mockNodes = listOf(
            createMockAXNode("node1", "button", "Submit", 101),
            createMockAXNode("node2", "textbox", "Username", 102)
        )

        every { accessibility.getFullAXTree(null, null) } returns mockNodes

        // Act
        val result = handler.getFullAXTree()

        // Assert
        assertEquals(2, result.size)
        assertEquals("node1", result[0].nodeId)
        assertEquals("button", result[0].role?.value)
        assertEquals("Submit", result[0].name?.value)
        assertEquals(101, result[0].backendDOMNodeId)
    }

    @Test
    fun `getFullAXTreeRecursive returns nodes grouped by frame and backend ID`() {
        // Arrange
        val frameTree = FrameTree().apply {
            frame = Frame().apply { id = "main-frame" }
            childFrames = listOf(
                FrameTree().apply {
                    frame = Frame().apply { id = "iframe1" }
                }
            )
        }

        val mainFrameNodes = listOf(
            createMockAXNode("main1", "button", "Main Button", 201, "main-frame"),
            createMockAXNode("main2", "link", "Main Link", 202, "main-frame")
        )

        val iframeNodes = listOf(
            createMockAXNode("iframe1", "textbox", "Iframe Input", 301, "iframe1")
        )

        every { page.getFrameTree() } returns frameTree
        every { accessibility.getFullAXTree(null, "main-frame") } returns mainFrameNodes
        every { accessibility.getFullAXTree(null, "iframe1") } returns iframeNodes

        // Act
        val result = handler.getFullAXTreeRecursive()

        // Assert
        assertEquals(3, result.nodes.size)
        assertEquals(2, result.nodesByFrameId["main-frame"]?.size)
        assertEquals(1, result.nodesByFrameId["iframe1"]?.size)
        assertEquals(2, result.nodesByBackendNodeId[201]?.size) // Should include frame-stamped duplicates
    }

    @Test
    fun `getAccessibilityTree with selector filters nodes correctly`() {
        // Arrange
        val allNodes = listOf(
            createMockAXNode("node1", "button", "Submit", 101),
            createMockAXNode("node2", "textbox", "Username", 102),
            createMockAXNode("node3", "button", "Cancel", 103)
        )

        every { accessibility.getFullAXTree(null, null) } returns allNodes

        val mockEvaluationResult = mockk<com.github.kklisura.cdt.protocol.v2023.types.runtime.EvaluateResponse>()
        val mockResult = mockk<RemoteObject>()
        every { mockResult.value } returns "[101, 103]" // Backend IDs matching button elements
        every { mockEvaluationResult.result } returns mockResult
        every { runtime.evaluate(any()) } returns mockEvaluationResult

        // Act
        val result = handler.getAccessibilityTree("button")

        // Assert
        assertEquals(2, result.nodes.size)
        assertTrue(result.nodes.all { it.backendDOMNodeId in listOf(101, 103) })
    }

    @Test
    fun `findScrollableElementIds identifies scrollable elements correctly`() {
        // Arrange
        val axNodes = listOf(
            createMockAXNode("scrollable1", "div", null, 201).apply {
                properties = listOf(
                    AXProperty().apply {
                        name = "scrollable"
                        value = AXPropertyValue().apply { value = true }
                    }
                )
            },
            createMockAXNode("nonScrollable", "span", null, 202),
            createMockAXNode("scrollable2", "div", null, 203).apply {
                properties = listOf(
                    AXProperty().apply {
                        name = "scrollable"
                        value = AXPropertyValue().apply { value = true }
                    }
                )
            }
        )

        every { accessibility.getFullAXTree(null, null) } returns axNodes

        val mockEvaluationResult = mockk<com.github.kklisura.cdt.protocol.v2023.types.runtime.EvaluateResponse>()
        val mockResult = mockk<RemoteObject>()
        every { mockResult.value } returns "[203]" // Additional scrollable found via DOM
        every { mockEvaluationResult.result } returns mockResult
        every { runtime.evaluate(any()) } returns mockEvaluationResult

        // Act
        val result = handler.findScrollableElementIds()

        // Assert
        assertEquals(listOf(201, 203), result)
    }

    @Test
    fun `getFullAXTree handles empty responses gracefully`() {
        // Arrange
        every { accessibility.getFullAXTree(null, null) } returns emptyList()

        // Act
        val result = handler.getFullAXTree()

        // Assert
        assertTrue(result.isEmpty())
    }

    @Test
    fun `getFullAXTreeRecursive handles frame tree errors`() {
        // Arrange
        every { page.getFrameTree() } throws RuntimeException("Frame tree error")
        every { page.frameTree } returns null

        // Act
        val result = handler.getFullAXTreeRecursive()

        // Assert
        assertEquals(AccessibilityTreeResult.EMPTY, result)
    }

    @Test
    fun `findScrollableElementIds handles evaluation errors gracefully`() {
        // Arrange
        val axNodes = listOf(
            createMockAXNode("scrollable1", "div", null, 201).apply {
                properties = listOf(
                    AXProperty().apply {
                        name = "scrollable"
                        value = AXPropertyValue().apply { value = true }
                    }
                )
            }
        )

        every { accessibility.getFullAXTree(null, null) } returns axNodes
        every { runtime.evaluate(any()) } throws RuntimeException("Evaluation failed")

        // Act
        val result = handler.findScrollableElementIds()

        // Assert
        assertEquals(listOf(201), result) // Should fallback to AX-only detection
    }

    // Helper function to create mock AX nodes
    private fun createMockAXNode(
        nodeId: String,
        role: String? = null,
        name: String? = null,
        backendDOMNodeId: Int? = null,
        frameId: String? = null
    ): AXNode {
        return AXNode().apply {
            this.nodeId = nodeId
            this.role = role?.let {
                com.github.kklisura.cdt.protocol.v2023.types.accessibility.AXValue().apply {
                    value = it
                }
            }
            this.name = name?.let {
                com.github.kklisura.cdt.protocol.v2023.types.accessibility.AXValue().apply {
                    value = it
                }
            }
            this.backendDOMNodeId = backendDOMNodeId
            this.frameId = frameId
            this.properties = emptyList()
            this.childIds = emptyList()
            this.ignored = false
        }
    }
}