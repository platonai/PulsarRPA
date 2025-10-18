package ai.platon.pulsar.browser.driver.chrome.dom

import ai.platon.pulsar.browser.driver.chrome.dom.model.DOMRect
import ai.platon.pulsar.browser.driver.chrome.dom.model.DOMTreeNodeEx
import ai.platon.pulsar.browser.driver.chrome.dom.model.SnapshotNodeEx
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ClickableElementDetectorTest {

    private val detector = ClickableElementDetector()

    @Test
    fun `button tag is interactive`() {
        val node = DOMTreeNodeEx(nodeName = "button")
        assertTrue(detector.isInteractive(node))
    }

    @Test
    fun `div with onclick is interactive`() {
        val node = DOMTreeNodeEx(
            nodeName = "div",
            attributes = mapOf("onclick" to "doIt()")
        )
        assertTrue(detector.isInteractive(node))
    }

    @Test
    fun `div with role button is interactive`() {
        val node = DOMTreeNodeEx(
            nodeName = "div",
            attributes = mapOf("role" to "button")
        )
        assertTrue(detector.isInteractive(node))
    }

    @Test
    fun `small iframe is not interactive, large iframe is`() {
        val small = DOMTreeNodeEx(
            nodeName = "iframe",
            snapshotNode = SnapshotNodeEx(bounds = DOMRect(0.0, 0.0, 80.0, 80.0))
        )
        val large = DOMTreeNodeEx(
            nodeName = "iframe",
            snapshotNode = SnapshotNodeEx(bounds = DOMRect(0.0, 0.0, 200.0, 200.0))
        )
        assertFalse(detector.isInteractive(small))
        assertTrue(detector.isInteractive(large))
    }

    @Test
    fun `html and body are not interactive`() {
        assertFalse(detector.isInteractive(DOMTreeNodeEx(nodeName = "html")))
        assertFalse(detector.isInteractive(DOMTreeNodeEx(nodeName = "body")))
    }

    @Test
    fun `cursor pointer implies interactive`() {
        val node = DOMTreeNodeEx(
            nodeName = "span",
            snapshotNode = SnapshotNodeEx(cursorStyle = "pointer")
        )
        assertTrue(detector.isInteractive(node))
    }

    @Test
    fun `icon sized element with aria-label is interactive`() {
        val node = DOMTreeNodeEx(
            nodeName = "span",
            attributes = mapOf("aria-label" to "open"),
            snapshotNode = SnapshotNodeEx(bounds = DOMRect(0.0, 0.0, 20.0, 20.0))
        )
        assertTrue(detector.isInteractive(node))
    }
}

