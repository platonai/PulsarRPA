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
    private val css get() = devTools.css.takeIf { isActive }
    private val runtime get() = devTools.runtime.takeIf { isActive }

    val mouse = Mouse(devTools)
    val keyboard = Keyboard(devTools)

    fun querySelector(selector: String): Int? {
        val rootId = dom?.document?.nodeId
        return if (rootId != null && rootId > 0) {
            dom?.querySelector(rootId, selector)
        } else null
    }

    fun visible(selector: String): Boolean {
        val nodeId = querySelector(selector)
        if (nodeId == null || nodeId <= 0) {
            return false
        }

        return visible(nodeId)
    }

    fun visible(nodeId: Int): Boolean {
        if (nodeId <= 0) {
            return false
        }

        var isVisible = true

        val properties = css?.getComputedStyleForNode(nodeId)
        properties?.forEach { prop ->
            when {
                prop.name == "display" && prop.value == "none" -> isVisible = false
                prop.name == "visibility" && prop.value == "hidden" -> isVisible = false
                prop.name == "opacity" && prop.value == "0" -> isVisible = false
            }
        }

        if (isVisible) {
            isVisible = ClickableDOM.create(page, dom, nodeId)?.isVisible() ?: false
        }

        return isVisible
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
                logger.info("Node is not of type HTMLElement | {}", selector ?: nodeId)
                return null
            }

            dom?.scrollIntoViewIfNeeded(nodeId, node.backendNodeId, null, rect)
        } catch (e: ChromeRPCException) {
            // logger.warn("Can to scroll into {} | {} | {}", nodeId, e.message, selector)
            // Fallback to Element.scrollIntoView if DOM.scrollIntoViewIfNeeded is not supported
            evaluate("__pulsar_utils__.scrollIntoView('$selector')")
        }

        return nodeId
    }

    /**
     * Evaluates expression on global object.
     *
     * @param expression Javascript expression to evaluate
     * @return Remote object value in case of primitive values or JSON values (if it was requested).
     * */
    fun evaluate(expression: String): Any? {
        val evaluate = runtime?.evaluate(browserSettings.confuser.confuse(expression))

        val exception = evaluate?.exceptionDetails?.exception
        if (exception != null) {
//            logger.warn(exception.value?.toString())
//            logger.warn(exception.unserializableValue)
            logger.info(exception.description + "\n>>>$expression<<<")
        }

//        println(Gson().toJson(evaluate))

        val result = evaluate?.result
        return result?.value
    }
}
