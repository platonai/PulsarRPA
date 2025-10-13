package ai.platon.pulsar.browser.driver.chrome.dom

import ai.platon.pulsar.browser.driver.chrome.RemoteDevTools
import ai.platon.pulsar.browser.driver.chrome.dom.model.DOMRect
import ai.platon.pulsar.browser.driver.chrome.dom.model.EnhancedSnapshotNode
import com.github.kklisura.cdt.protocol.v2023.commands.DOMSnapshot
import com.github.kklisura.cdt.protocol.v2023.types.domsnapshot.DocumentSnapshot
import com.github.kklisura.cdt.protocol.v2023.types.domsnapshot.LayoutTreeSnapshot
import com.github.kklisura.cdt.protocol.v2023.types.domsnapshot.NameValue
import com.github.kklisura.cdt.protocol.v2023.types.domsnapshot.NodeTreeSnapshot
import com.github.kklisura.cdt.protocol.v2023.types.domsnapshot.RareBooleanData
import com.github.kklisura.cdt.protocol.v2023.types.domsnapshot.SnapshotCapture
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DomSnapshotHandlerTests {

    private lateinit var devTools: RemoteDevTools
    private lateinit var domSnapshot: DOMSnapshot
    private lateinit var handler: DomSnapshotHandler

    @BeforeEach
    fun setup() {
        devTools = mockk(relaxed = true)
        domSnapshot = mockk(relaxed = true)

        every { devTools.domSnapshot } returns domSnapshot
        handler = DomSnapshotHandler(devTools)
    }

    @Test
    fun `captureByBackendNodeId returns correct structure with bounds and rects`() {
        // Arrange
        val mockSnapshot = createMockSnapshotWithBasicData()
        every { domSnapshot.captureSnapshot(any(), any(), any(), any(), any()) } returns mockSnapshot

        // Act
        val result = handler.captureByBackendNodeId()

        // Assert
        assertEquals(2, result.size)

        val node1 = result[101]
        assertNotNull(node1)
        assertEquals(DOMRect(10.0, 20.0, 100.0, 50.0), node1.bounds)
        assertEquals(DOMRect(10.0, 20.0, 100.0, 50.0), node1.clientRects)
        assertEquals(DOMRect(10.0, 20.0, 150.0, 80.0), node1.scrollRects)
        assertEquals(1, node1.paintOrder)
        assertTrue(node1.isClickable == true)
        assertEquals("pointer", node1.cursorStyle)

        val node2 = result[102]
        assertNotNull(node2)
        assertEquals(DOMRect(200.0, 300.0, 80.0, 40.0), node2.bounds)
        assertEquals(2, node2.paintOrder)
    }

    @Test
    fun `captureByBackendNodeId handles computed styles correctly`() {
        // Arrange
        val mockSnapshot = createMockSnapshotWithStyles()
        every { domSnapshot.captureSnapshot(any(), any(), any(), any(), any()) } returns mockSnapshot

        // Act
        val result = handler.captureByBackendNodeId(includeStyles = true)

        // Assert
        val node = result[101]
        assertNotNull(node)
        assertNotNull(node.computedStyles)
        assertEquals("block", node.computedStyles!!["display"])
        assertEquals("auto", node.computedStyles["overflow"])
        assertEquals("pointer", node.computedStyles["cursor"])
    }

    @Test
    fun `captureEnhanced includes absolute coordinates and stacking contexts`() {
        // Arrange
        val mockSnapshot = createMockEnhancedSnapshot()
        every { domSnapshot.captureSnapshot(any(), any(), any(), any(), any()) } returns mockSnapshot

        val mockRuntime = mockk<com.github.kklisura.cdt.protocol.v2023.commands.Runtime>()
        every { devTools.runtime } returns mockRuntime

        val viewportResponse = mockk<com.github.kklisura.cdt.protocol.v2023.types.runtime.EvaluateResponse>()
        val viewportResult = mockk<com.github.kklisura.cdt.protocol.v2023.types.runtime.RemoteObject>()
        every { viewportResult.value } returns "{\"width\":1920,\"height\":1080,\"x\":0,\"y\":0}"
        every { viewportResponse.result } returns viewportResult
        every { mockRuntime.evaluate(any()) } returns viewportResponse

        // Act
        val result = handler.captureEnhanced(includeAbsoluteCoords = true)

        // Assert
        val node = result[101]
        assertNotNull(node)
        assertNotNull(node.absoluteBounds)
        assertEquals(DOMRect(10.0, 20.0, 100.0, 50.0), node.absoluteBounds)
        assertEquals(1, node.stackingContexts)
    }

    @Test
    fun `captureByBackendNodeId handles missing data gracefully`() {
        // Arrange
        val mockSnapshot = createMockSnapshotWithMissingData()
        every { domSnapshot.captureSnapshot(any(), any(), any(), any(), any()) } returns mockSnapshot

        // Act
        val result = handler.captureByBackendNodeId()

        // Assert
        assertEquals(1, result.size)
        val node = result[101]
        assertNotNull(node)
        assertNull(node.bounds) // Missing bounds should be null
        assertNull(node.paintOrder) // Missing paint order should be null
        assertTrue(node.computedStyles.isNullOrEmpty()) // Missing styles should be empty
    }

    @Test
    fun `captureByBackendNodeId respects includeStyles parameter`() {
        // Arrange
        val mockSnapshot = createMockSnapshotWithStyles()
        every { domSnapshot.captureSnapshot(any(), any(), any(), any(), any()) } returns mockSnapshot

        // Act - without styles
        val resultWithoutStyles = handler.captureByBackendNodeId(includeStyles = false)

        // Assert
        val node = resultWithoutStyles[101]
        assertNotNull(node)
        assertTrue(node.computedStyles.isNullOrEmpty())
    }

    @Test
    fun `REQUIRED_COMPUTED_STYLES contains essential style properties`() {
        // Assert
        val requiredStyles = DomSnapshotHandler.REQUIRED_COMPUTED_STYLES
        assertTrue(requiredStyles.contains("display"))
        assertTrue(requiredStyles.contains("visibility"))
        assertTrue(requiredStyles.contains("overflow"))
        assertTrue(requiredStyles.contains("cursor"))
        assertTrue(requiredStyles.contains("position"))
    }

    @Test
    fun `EXTENDED_COMPUTED_STYLES includes positioning and layout properties`() {
        // Assert
        val extendedStyles = DomSnapshotHandler.EXTENDED_COMPUTED_STYLES
        assertTrue(extendedStyles.contains("transform"))
        assertTrue(extendedStyles.contains("z-index"))
        assertTrue(extendedStyles.contains("top"))
        assertTrue(extendedStyles.contains("left"))
        assertTrue(extendedStyles.contains("width"))
        assertTrue(extendedStyles.contains("height"))
        assertTrue(extendedStyles.contains("margin-top"))
        assertTrue(extendedStyles.contains("padding-top"))
    }

    // Helper functions to create mock snapshot data

    private fun createMockSnapshotWithBasicData(): SnapshotCapture {
        return SnapshotCapture().apply {
            documents = listOf(
                DocumentSnapshot().apply {
                    nodes = NodeTreeSnapshot().apply {
                        backendNodeId = listOf(101, 102)
                    }
                    layout = LayoutTreeSnapshot().apply {
                        nodeIndex = listOf(0, 1)
                        bounds = listOf(
                            listOf(10.0, 20.0, 110.0, 70.0),  // [x1, y1, x2, y2, x3, y3, x4, y4]
                            listOf(200.0, 300.0, 280.0, 340.0)
                        )
                        clientRects = listOf(
                            listOf(10.0, 20.0, 100.0, 50.0),  // [x, y, width, height]
                            listOf(200.0, 300.0, 80.0, 40.0)
                        )
                        scrollRects = listOf(
                            listOf(10.0, 20.0, 150.0, 80.0),
                            listOf(200.0, 300.0, 80.0, 40.0)
                        )
                        paintOrders = listOf(1, 2)
                        styles = listOf(
                            listOf(0, 1), // indices into strings for style name/value pairs
                            listOf(2, 3)
                        )
                    }
                }
            )
            strings = listOf("cursor", "pointer", "display", "block")
        }
    }

    private fun createMockSnapshotWithStyles(): SnapshotCapture {
        return SnapshotCapture().apply {
            documents = listOf(
                DocumentSnapshot().apply {
                    nodes = NodeTreeSnapshot().apply {
                        backendNodeId = listOf(101)
                    }
                    layout = LayoutTreeSnapshot().apply {
                        nodeIndex = listOf(0)
                        bounds = listOf(listOf(10.0, 20.0, 110.0, 70.0))
                        clientRects = listOf(listOf(10.0, 20.0, 100.0, 50.0))
                        scrollRects = listOf(listOf(10.0, 20.0, 150.0, 80.0))
                        paintOrders = listOf(1)
                        styles = listOf(
                            listOf(0, 1, 2, 3, 4, 5) // Multiple style pairs
                        )
                    }
                }
            )
            strings = listOf(
                "display", "block",
                "overflow", "auto",
                "cursor", "pointer",
                "visibility", "visible"
            )
        }
    }

    private fun createMockEnhancedSnapshot(): SnapshotCapture {
        return SnapshotCapture().apply {
            documents = listOf(
                DocumentSnapshot().apply {
                    nodes = NodeTreeSnapshot().apply {
                        backendNodeId = listOf(101)
                    }
                    layout = LayoutTreeSnapshot().apply {
                        nodeIndex = listOf(0)
                        bounds = listOf(listOf(10.0, 20.0, 110.0, 70.0))
                        clientRects = listOf(listOf(10.0, 20.0, 100.0, 50.0))
                        scrollRects = listOf(listOf(10.0, 20.0, 150.0, 80.0))
                        paintOrders = listOf(1)
                        stackingContexts = RareBooleanData().apply {
                            index = listOf(0) // First node is in stacking context
                        }
                        styles = listOf(listOf(0, 1))
                    }
                }
            )
            strings = listOf("position", "relative")
        }
    }

    private fun createMockSnapshotWithMissingData(): SnapshotCapture {
        return SnapshotCapture().apply {
            documents = listOf(
                DocumentSnapshot().apply {
                    nodes = NodeTreeSnapshot().apply {
                        backendNodeId = listOf(101)
                    }
                    layout = LayoutTreeSnapshot().apply {
                        nodeIndex = listOf(0)
                        // Missing bounds, rects, paint orders
                        bounds = emptyList()
                        clientRects = emptyList()
                        scrollRects = emptyList()
                        paintOrders = emptyList()
                        styles = emptyList()
                    }
                }
            )
            strings = emptyList()
        }
    }
}