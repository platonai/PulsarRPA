package ai.platon.pulsar.browser.driver.chrome

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import ai.platon.cdt.kt.protocol.support.annotations.ParamName
import ai.platon.cdt.kt.protocol.types.dom.Rect
import ai.platon.cdt.kt.protocol.types.page.Navigate
import ai.platon.cdt.kt.protocol.types.page.ReferrerPolicy
import ai.platon.cdt.kt.protocol.types.page.TransitionType
import ai.platon.cdt.kt.protocol.types.runtime.Evaluate
import ai.platon.pulsar.browser.common.ScriptConfuser
import ai.platon.pulsar.browser.driver.chrome.dom.Locator
import ai.platon.pulsar.browser.driver.chrome.util.ChromeDriverException
import ai.platon.pulsar.browser.driver.chrome.util.ChromeRPCException
import ai.platon.pulsar.common.AppContext
import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.getLogger

data class NodeRef constructor(
    val nodeId: Int? = null,
    // backend node id is more stable
    val backendNodeId: Int? = null,
    val objectId: String? = null
)

class PageHandler(
    private val devTools: RemoteDevTools,
    private val confuser: ScriptConfuser,
) {
    companion object {
        // see org.w3c.dom.Node.ELEMENT_NODE
        const val ELEMENT_NODE = 1

        // Backend node ID selector prefix
        private val BACKEND_NODE_PREFIX = Locator.Type.BACKEND_NODE_ID.text
        private val FBN_PREFIX = Locator.Type.FRAME_BACKEND_NODE_ID.text
    }

    private val logger = getLogger(this)

    private val isActive get() = AppContext.isActive && devTools.isOpen
    private val pageAPI get() = devTools.page.takeIf { isActive }
    private val domAPI get() = devTools.dom.takeIf { isActive }
    private val cssAPI get() = devTools.css.takeIf { isActive }
    private val runtimeAPI get() = devTools.runtime.takeIf { isActive }

    val mouse = Mouse(devTools)
    val keyboard = Keyboard(devTools)

    @Throws(ChromeDriverException::class)
    suspend fun navigate(@ParamName("url") url: String): Navigate? {
        return pageAPI?.navigate(url)
    }

    @Throws(ChromeDriverException::class)
    suspend fun navigate(
        @ParamName("url") url: String,
        @Optional @ParamName("referrer") referrer: String? = null,
        @Optional @ParamName("transitionType") transitionType: TransitionType? = null,
        @Optional @ParamName("frameId") frameId: String? = null,
        @Experimental @Optional @ParamName("referrerPolicy") referrerPolicy: ReferrerPolicy? = null
    ): Navigate? {
        return pageAPI?.navigate(url, referrer, transitionType, frameId, referrerPolicy)
    }

    /**
     * Queries for an element using a CSS selector or backend node ID.
     *
     * Supports two selector formats:
     * 1. CSS selector: "div.class", "#id", etc.
     * 2. Backend node ID: "backend:123"
     *
     * @param selector CSS selector or "backend:nodeId" format
     * @return nodeId or null if not found
     */
    @Throws(ChromeDriverException::class)
    suspend fun querySelector(selector: String): NodeRef? {
        return resolveSelector(selector)
    }

    /**
     * Gets a specific attribute value for the element matching the selector.
     *
     * @param selector CSS selector or "backend:nodeId" format
     * @param attrName Attribute name to retrieve
     * @return Attribute value or null if not found
     */
    @Throws(ChromeDriverException::class)
    suspend fun getAttribute(selector: String, attrName: String) = invokeOnElement(selector) { getAttribute(it, attrName) }

    @Throws(ChromeDriverException::class)
    suspend fun getAttribute(node: NodeRef, attrName: String): String? {
        node.nodeId ?: return null

        // `attributes`: n1, v1, n2, v2, n3, v3, ...
        val attributes = domAPI?.getAttributes(node.nodeId) ?: return null
        val nameIndex = attributes.indexOf(attrName)
        if (nameIndex < 0) {
            return null
        }
        val valueIndex = nameIndex + 1
        return attributes.getOrNull(valueIndex)
    }

    @Throws(ChromeDriverException::class)
    suspend fun setAttribute(nodeId: Int, attrName: String, attrValue: String) {
        domAPI?.setAttributeValue(nodeId, attrName, attrValue)
    }

    /**
     * Checks if the element matching the selector is visible.
     *
     * @param selector CSS selector or "backend:nodeId" format
     * @return true if visible, false otherwise
     */
    @Throws(ChromeDriverException::class)
    suspend fun visible(selector: String) = predicateOnElement(selector) { visible(it) }

    @Throws(ChromeDriverException::class)
    suspend fun visible(node: NodeRef): Boolean {
        node.nodeId ?: return false

        var isVisible = true

        val properties = cssAPI?.getComputedStyleForNode(node.nodeId)
        properties?.forEach { prop ->
            when {
                prop.name == "display" && prop.value == "none" -> isVisible = false
                prop.name == "visibility" && prop.value == "hidden" -> isVisible = false
                prop.name == "opacity" && prop.value == "0" -> isVisible = false
            }
        }

        if (isVisible) {
            isVisible = ClickableDOM.create(pageAPI, domAPI, node)?.isVisible() ?: false
        }

        return isVisible
    }

    /**
     * This method fetches an element with `selector` and focuses it. If there's no
     * element matching `selector`, the method returns 0.
     *
     * Supports two selector formats:
     * 1. CSS selector: "input#username"
     * 2. Backend node ID: "backend:123"
     *
     * @param selector - A CSS selector or "backend:nodeId" format of an element to focus.
     * If there are multiple elements satisfying the selector, the first will be focused.
     * @returns NodeId which resolves when the element matching selector is
     * successfully focused. Returns 0 if there is no element matching selector.
     */
    @Throws(ChromeDriverException::class)
    suspend fun focusOnSelector(selector: String): NodeRef? {
        val nodeRef = resolveSelector(selector) ?: return null

        // Fix: Only use nodeId parameter, others should be null
        domAPI?.focus(nodeRef.nodeId, nodeRef.backendNodeId, nodeRef.objectId)

        return nodeRef
    }

    /**
     * Scrolls the element into view if needed.
     *
     * @param selector CSS selector or "backend:nodeId" format
     * @param rect Optional rectangle to scroll into view
     * @return nodeId of the element, or null if not found
     */
    @Throws(ChromeDriverException::class)
    suspend fun scrollIntoViewIfNeeded(selector: String, rect: Rect? = null): NodeRef? {
        val node = resolveSelector(selector) ?: return null
        if (node.nodeId == null) {
            logger.info("No node found for selector: $selector")
            return null
        }

        return scrollIntoViewIfNeeded(node, selector, rect)
    }

    @Throws(ChromeDriverException::class)
    suspend fun scrollIntoViewIfNeeded(nodeRef: NodeRef, selector: String? = null, rect: Rect? = null): NodeRef? {
        try {
            val node = domAPI?.describeNode(nodeRef.nodeId, nodeRef.backendNodeId, nodeRef.objectId, null, false)
            if (node?.nodeType != ELEMENT_NODE) {
                logger.info("Node is not of type HTMLElement | {}", selector ?: node)
                return null
            }

            domAPI?.scrollIntoViewIfNeeded(node.nodeId, node.backendNodeId, nodeRef.objectId, rect)

            return nodeRef
        } catch (e: ChromeRPCException) {
            logger.debug("DOM.scrollIntoViewIfNeeded is not supported, fallback to Element.scrollIntoView | {} | {} | {}",
                nodeRef, e.message, selector)
            // Fallback to Element.scrollIntoView if DOM.scrollIntoViewIfNeeded is not supported
            evaluate("__pulsar_utils__.scrollIntoView('$selector')")
        }

        return null
    }

    /**
     * Evaluates expression on global object.
     *
     * @param expression Javascript expression to evaluate
     * @return Remote object value in case of primitive values or JSON values (if it was requested).
     * */
    @Throws(ChromeDriverException::class)
    suspend fun evaluateDetail(expression: String): Evaluate? {
//        val iife = JsUtils.toIIFE(confuser.confuse(expression))
//        return runtime?.evaluate(iife)
        val evaluate = runtimeAPI?.evaluate(confuser.confuse(expression))

        return evaluate
    }

    /**
     * Evaluates expression on global object.
     *
     * @param expression Javascript expression to evaluate
     * @return Remote object value in case of primitive values or JSON values (if it was requested).
     * */
    @Throws(ChromeDriverException::class)
    suspend fun evaluate(expression: String): Any? {
        val evaluate = evaluateDetail(expression)

        val exception = evaluate?.exceptionDetails?.exception
        if (exception != null) {
            logger.warn(exception.description + "\n>>>$expression<<<")
        }

        val result = evaluate?.result
        return result?.value
    }

    @Throws(ChromeDriverException::class)
    suspend fun evaluateValueDetail(expression: String): Evaluate? {
//        val iife = JsUtils.toIIFE(confuser.confuse(expression))
//        return evaluate(iife, returnByValue = true)
        val expression2 = confuser.confuse(expression)
        // return cdpEvaluate(expression2, returnByValue = true)

        // returnByValue: Whether the result is expected to be a JSON object that should be sent by value.
        return runtimeAPI?.evaluate(expression2, returnByValue = true)
    }

    /**
     * Evaluates expression on global object.
     *
     * @param expression Javascript expression to evaluate
     * @return Remote object value in case of primitive values or JSON values (if it was requested).
     * */
    @Throws(ChromeDriverException::class)
    suspend fun evaluateValue(expression: String): Any? {
        val evaluate = evaluateValueDetail(expression)

        val exception = evaluate?.exceptionDetails?.exception
        if (exception != null) {
            logger.info(exception.description + "\n>>>$expression<<<")
        }

        return evaluate?.result?.value
    }

    @Throws(ChromeDriverException::class)
    private suspend fun querySelectorOrNull(selector: String): NodeRef? {
        val rootId = domAPI?.getDocument()?.nodeId
        return if (rootId != null && rootId > 0) {
            val nodeId = domAPI?.querySelector(rootId, selector)
            val node = domAPI?.describeNode(nodeId, null, null, null, null) ?: return null
            NodeRef(node.nodeId, node.backendNodeId)
        } else null
    }

    /**
     * Parses the selector and returns the node ID.
     * Supports two formats:
     * 1. Regular CSS selector: returns nodeId via querySelector
     * 2. Backend node ID selector: "backend:123" returns nodeId via resolveNode
     *
     * @param selector CSS selector or "backend:nodeId" format
     * @return nodeId or null if not found
     */
    @Throws(ChromeDriverException::class)
    private suspend fun resolveSelector(selector: String): NodeRef? {
        val locator = Locator.parse(selector) ?: return null

        val nodeRef = when (locator.type) {
            Locator.Type.CSS_PATH -> querySelectorOrNull(selector)
            Locator.Type.BACKEND_NODE_ID -> {
                val backendNodeId = locator.selector.toIntOrNull()
                if (backendNodeId == null) {
                    logger.warn("Invalid backend node ID format: '{}'", selector)
                    return null
                }
                resolveByBackendNodeId(backendNodeId)
            }
            Locator.Type.FRAME_BACKEND_NODE_ID -> {
                val backendNodeId = selector.substringAfterLast(",").toIntOrNull()
                resolveByBackendNodeId(backendNodeId)
            }
            else -> throw UnsupportedOperationException("Unsupported selector $selector")
        }

        return nodeRef
    }

    @Throws(ChromeDriverException::class)
    private suspend fun resolveByBackendNodeId(backendNodeId: Int?): NodeRef? = resolve(null, backendNodeId)

    /**
     * Resolves a backend node ID to a regular node ID.
     *
     * @param backendNodeId The backend node ID
     * @return nodeId or null if resolution fails
     */
    @Throws(ChromeDriverException::class)
    private suspend fun resolve(nodeId: Int?, backendNodeId: Int?): NodeRef? {
        return try {
            // Use DOM.resolveNode to convert backendNodeId to a runtime object
            val remoteObject = domAPI?.resolveNode(nodeId, backendNodeId, null, null)

            if (remoteObject?.objectId == null) {
                logger.warn("Failed to resolve backend node ID: {}", backendNodeId)
                return null
            }

            val objectId = remoteObject.objectId
            // Use DOM.requestNode to get the nodeId from the runtime object
            val nodeId = domAPI?.requestNode(objectId)
            // Release the remote object to avoid memory leaks
            runtimeAPI?.releaseObject(objectId)

            NodeRef(nodeId, backendNodeId, objectId)
        } catch (e: Exception) {
            logger.warn("Exception resolving backend node ID {}: {}", backendNodeId, e.message)
            null
        }
    }

    /**
     * Resolves a backend node ID to a regular node ID.
     *
     * @param backendNodeId The backend node ID
     * @return nodeId or null if resolution fails
     */
    @Throws(ChromeDriverException::class)
    private suspend fun resolveBackendNodeId(backendNodeId: Int?): Int? {
        backendNodeId ?: return null

        return try {
            // Use DOM.resolveNode to convert backendNodeId to a runtime object
            val remoteObject = domAPI?.resolveNode(null, backendNodeId, null, null)

            if (remoteObject?.objectId == null) {
                logger.warn("Failed to resolve backend node ID: {}", backendNodeId)
                return null
            }

            // Use DOM.requestNode to get the nodeId from the runtime object
            val nodeId = domAPI?.requestNode(remoteObject.objectId)

            // Release the remote object to avoid memory leaks
            runtimeAPI?.releaseObject(remoteObject.objectId)

            nodeId
        } catch (e: Exception) {
            logger.warn("Exception resolving backend node ID {}: {}", backendNodeId, e.message)
            null
        }
    }

    @Throws(ChromeDriverException::class)
    private suspend fun <T> invokeOnElement(selector: String, action: suspend (NodeRef) -> T): T? {
        val node = resolveSelector(selector) ?: return null

        return action(node)
    }

    @Throws(ChromeDriverException::class)
    private suspend fun predicateOnElement(selector: String, action: suspend (NodeRef) -> Boolean): Boolean {
        val node = resolveSelector(selector) ?: return false

        if (node.nodeId != null && node.nodeId > 0) {
            return action(node)
        }

        return false
    }
}
