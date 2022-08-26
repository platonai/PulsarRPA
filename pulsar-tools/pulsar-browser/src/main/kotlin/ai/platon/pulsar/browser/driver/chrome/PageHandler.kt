package ai.platon.pulsar.browser.driver.chrome

import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.browser.driver.chrome.util.ChromeRPCException
import ai.platon.pulsar.common.AppContext
import ai.platon.pulsar.common.getLogger
import com.github.kklisura.cdt.protocol.types.dom.Rect

class PageHandler(
    private val devTools: RemoteDevTools,
    private val browserSettings: BrowserSettings
) {
    companion object {
        // see org.w3c.dom.Node.ELEMENT_NODE
        const val ELEMENT_NODE = 1
    }

    private val logger = getLogger(this)

    private val isActive get() = AppContext.isActive && devTools.isOpen
    private val page get() = devTools.page.takeIf { isActive }
    private val dom get() = devTools.dom.takeIf { isActive }
    private val runtime get() = devTools.runtime.takeIf { isActive }

    val mouse = Mouse(devTools)
    val keyboard = Keyboard(devTools)

    fun querySelector(selector: String): Int? {
        val rootId = dom?.document?.nodeId
        return if (rootId != null && rootId != 0) {
            dom?.querySelector(rootId, selector)
        } else null
    }

    fun scrollIntoViewIfNeeded(selector: String, rect: Rect? = null): Int? {
        val nodeId = querySelector(selector)
        if (nodeId == null || nodeId == 0) {
            logger.info("No node found for selector: $selector")
            return null
        }

        return scrollIntoViewIfNeeded(nodeId, selector, rect)
    }

    fun scrollIntoViewIfNeeded(nodeId: Int, selector: String? = null, rect: Rect? = null): Int? {
        try {
            val node = dom?.describeNode(nodeId, null, null, null, false)
            if (node?.nodeType != ELEMENT_NODE) {
                logger.info("Node is not an element: {}", selector ?: nodeId)
                return null
            }

            dom?.scrollIntoViewIfNeeded(nodeId, node.backendNodeId, null, rect)
        } catch (e: ChromeRPCException) {
            logger.warn("Failed to scroll into {}/{} | {}", nodeId, selector, e.message)
        }

        return nodeId
    }

    fun evaluate(expression: String): Any? {
        val evaluate = runtime?.evaluate(browserSettings.nameMangling(expression))

        val exception = evaluate?.exceptionDetails?.exception
        if (exception != null) {
//                logger.warn(exception.value?.toString())
//                logger.warn(exception.unserializableValue)
            logger.info(exception.description + "\n>>>$expression<<<")
        }

        val result = evaluate?.result
        return result?.value
    }
}
