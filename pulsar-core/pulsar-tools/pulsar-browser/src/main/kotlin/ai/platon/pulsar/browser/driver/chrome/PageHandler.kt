package ai.platon.pulsar.browser.driver.chrome

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import ai.platon.cdt.kt.protocol.support.annotations.ParamName
import ai.platon.cdt.kt.protocol.types.dom.Rect
import ai.platon.cdt.kt.protocol.types.page.Navigate
import ai.platon.cdt.kt.protocol.types.page.ReferrerPolicy
import ai.platon.cdt.kt.protocol.types.page.TransitionType
import ai.platon.cdt.kt.protocol.types.runtime.CallFunctionOn
import ai.platon.cdt.kt.protocol.types.runtime.Evaluate
import ai.platon.pulsar.browser.common.ScriptConfuser
import ai.platon.pulsar.browser.driver.chrome.dom.Locator
import ai.platon.pulsar.browser.driver.chrome.util.ChromeDriverException
import ai.platon.pulsar.browser.driver.chrome.util.ChromeRPCException
import ai.platon.pulsar.common.AppContext
import ai.platon.pulsar.common.brief
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.js.JsUtils

/**
 * NodeId does not explicitly prohibit 0, but as seen in the internal implementation (Chromium source code):
 * - All valid nodes are assigned NodeIds starting from 1
 * - `0` is reserved as an "invalid / null node"
 *
 * DOM.NodeId #
 * Unique DOM node identifier.
 * Type: integer
 *
 * DOM.BackendNodeId #
 * Unique DOM node identifier used to reference a node that may not have been pushed to the front-end.
 * Type: integer
 *
 * References:
 * - [NodeId](https://chromedevtools.github.io/devtools-protocol/tot/DOM/#type-NodeId)
 * */
data class NodeRef constructor(
    val nodeId: Int = 0,
    // backend node id is more stable
    val backendNodeId: Int = 0,
    val objectId: String? = null
) {
    /**
     * Check if the node may exist.
     *
     * At least one of nodeId and backendNodeId is positive.
     * */
    fun mayExist(): Boolean {
        return nodeId > 0 || backendNodeId > 0
    }

    fun isNull(): Boolean {
        return nodeId == 0 && backendNodeId == 0
    }
}

class PageHandler(
    private val devTools: RemoteDevTools,
    private val confuser: ScriptConfuser,
) {
    companion object {
        // see org.w3c.dom.Node.ELEMENT_NODE
        const val ELEMENT_NODE = 1
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

    suspend fun exists(selector: String): Boolean {
        val rootId = domAPI?.getDocument()?.nodeId ?: return false
        val nodeId = try {
            // Executes `querySelector` on a given node.
            domAPI?.querySelector(rootId, selector)
        } catch (e: Exception) {
            logger.warn("Exception executing `querySelector` on node $rootId.", e)
            null
        }
        return nodeId != null && nodeId > 0
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
    @Deprecated("Use resolveSelector instead", ReplaceWith("resolveSelector(selector)"))
    @Throws(ChromeDriverException::class)
    suspend fun querySelector(selector: String): NodeRef? {
        return resolveSelector(selector)
    }

    /**
     * Resolves a selector to a `NodeRef` object, which contains information about the DOM node.
     * This method supports two types of selectors:
     * 1. Regular CSS selector: Resolves to a `NodeRef` using `querySelectorOrNull`.
     * 2. Backend node ID selector: Resolves to a `NodeRef` using `resolveByBackendNodeId`.
     *
     * @param selector A string representing the selector. It can be:
     * - A CSS selector (e.g., "div.class", "#id").
     * - A backend node ID in the format "backend:123".
     * - A frame-backendNode int the format "fbn:FRAMExID,123"
     *
     * @return A `NodeRef` object if the selector resolves successfully, or `null` if not found.
     *
     * @throws ChromeDriverException If an error occurs during the resolution process.
     */
    @Throws(ChromeDriverException::class)
    suspend fun resolveSelector(selector: String): NodeRef? {
        // Parse the selector into a Locator object. If parsing fails, return null.
        val locator = Locator.parse(selector) ?: return null

        require(Locator.Type.CSS_PATH.text.isEmpty())

        // Determine the type of the locator and resolve accordingly.
        val nodeRef = when (locator.type) {
            // For CSS_PATH type, use querySelectorOrNull to resolve the selector.
            Locator.Type.CSS_PATH -> resolveCSSSelector0(selector)

            // For BACKEND_NODE_ID type, parse the backend node ID and resolve it.
            Locator.Type.BACKEND_NODE_ID -> {
                val backendNodeId = locator.selector.toIntOrNull()
                if (backendNodeId == null) {
                    logger.warn("Invalid backend node ID format: '{}'", selector)
                    return null
                }
                resolveByBackendNodeId(backendNodeId)
            }

            // For FRAME_BACKEND_NODE_ID type, extract the backend node ID and resolve it.
            Locator.Type.FRAME_BACKEND_NODE_ID -> {
                val backendNodeId = selector.substringAfterLast(",").toIntOrNull()
                resolveByBackendNodeId(backendNodeId)
            }

            else -> throw UnsupportedOperationException("Unsupported selector $selector")
        }

        // Return the resolved NodeRef or null if resolution failed.
        return nodeRef
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
        if (node.isNull()) {
            return null
        }

        // `attributes`: n1, v1, n2, v2, n3, v3, ...
        val attributes = domAPI?.getAttributes(node.nodeId) ?: return null
        val nameIndex = attributes.indexOf(attrName)
        if (nameIndex < 0) {
            return null
        }
        val valueIndex = nameIndex + 1
        return attributes.getOrNull(valueIndex)
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
        if (node.isNull()) {
            return false
        }

        var isVisible = true

        val properties = cssAPI?.getComputedStyleForNode(node.nodeId)
        properties?.forEach { prop ->
            when (prop.name) {
                "display" if prop.value == "none" -> isVisible = false
                "visibility" if prop.value == "hidden" -> isVisible = false
                "opacity" if prop.value == "0" -> isVisible = false
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

        val confusedExpr = confuser.confuse(expression)

        return try {
            runtimeAPI?.evaluate(confusedExpr)
        } catch (e: Exception) {
            logger.warn("Failed to evaluate $expression", e)
            null
        }
    }

    /**
     * Evaluates expression on global object.
     *
     * @param expression Javascript expression to evaluate
     * @return Remote object value in case of primitive values or JSON values (if it was requested).
     * */
    @Throws(ChromeDriverException::class)
    suspend fun evaluate(script: String): Any? {
        val evaluate = evaluateDetail(script)

        val exception = evaluate?.exceptionDetails?.exception
        if (exception != null) {
            logger.warn(exception.description + "\n>>>$script<<<")
        }

        val result = evaluate?.result
        return result?.value
    }

    @Throws(ChromeDriverException::class)
    suspend fun evaluateValueDetail(script: String): Evaluate? {
        val expression: String
        val lines = script.split('\n').map { it.trim() }.filter { it.isNotBlank() }
        // Check if this script is a IIFE
        if (lines.size > 1) {
            val firstLine = lines[0]
            if (!firstLine.startsWith("(")) {
                expression = JsUtils.toIIFE(confuser.confuse(script))
            } else {
                expression = script
            }
        } else {
            expression = script
        }
//        val iife = JsUtils.toIIFE(confuser.confuse(expression))

        val confusedExpr = confuser.confuse(expression)

        return try {
            // returnByValue: Whether the result is expected to be a JSON object that should be sent by value.
            runtimeAPI?.evaluate(confusedExpr, returnByValue = true)
        } catch (e: Exception) {
            logger.warn("Failed to evaluate $expression", e)
            null
        }
    }

    /**
     * Evaluates expression on global object.
     *
     * @param expression Javascript expression to evaluate
     * @return Remote object value in case of primitive values or JSON values (if it was requested).
     * */
    @Throws(ChromeDriverException::class)
    suspend fun evaluateValue(script: String): Any? {
        val evaluate = evaluateValueDetail(script)

        val exception = evaluate?.exceptionDetails?.exception
        if (exception != null) {
            logger.info(exception.description + "\n>>>$script<<<")
        }

        return evaluate?.result?.value
    }

    @Throws(ChromeDriverException::class)
    suspend fun evaluateValueDetail(selector: String, functionDeclaration: String): CallFunctionOn? {
        val node = resolveSelector(selector)
        return runtimeAPI?.callFunctionOn(functionDeclaration, objectId = node?.objectId)
    }

    @Throws(ChromeDriverException::class)
    suspend fun evaluateValue(selector: String, functionDeclaration: String): Any? {
        val reslut = evaluateValueDetail(selector, functionDeclaration)

        val exception = reslut?.exceptionDetails?.exception
        if (exception != null) {
            logger.info(exception.description + "\n>>>$functionDeclaration<<<")
        }

        return reslut?.result?.value
    }

    @Throws(ChromeDriverException::class)
    private suspend fun resolveCSSSelector0(selector: String): NodeRef? {
        val rootId = domAPI?.getDocument()?.nodeId ?: return null

        val nodeId = try {
            domAPI?.querySelector(rootId, selector)
        } catch (e: ChromeRPCException) {
            // code: -3200 message: "Could not find node with given id"
            // This exception is expected, will change this log to debug
            val message = e.message
            if (message == null || !message.contains("Could not find node with given id")) {
                logger.warn("Exception from domAPI?.querySelector | {}", e.brief())
            }
            null
        } catch (e: Exception) {
            logger.warn("Unexpected exception from domAPI?.querySelector ", e)
            null
        }

        if (nodeId == null || nodeId == 0) {
            return null
        }

        val node = try {
            domAPI?.describeNode(nodeId, null, null, null, null)
        } catch (e: Exception) {
            logger.warn("Exception from domAPI?.describeNode ", e)
            null
        }

        node ?: return null

        if (node.nodeId == 0 || node.backendNodeId == 0) {
            logger.info("Both nodeId and backendNodeId are not found (value: 0)")
            return null
        }

        return NodeRef(node.nodeId, node.backendNodeId)
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
                logger.warn("Failed to resolve node: {}, {}", nodeId, backendNodeId)
                return null
            }

            val objectId = remoteObject.objectId
            // Use DOM.requestNode to get the nodeId from the runtime object
            val nodeId = domAPI?.requestNode(objectId) ?: 0
            // Release the remote object to avoid memory leaks
            runtimeAPI?.releaseObject(objectId)

            NodeRef(nodeId, backendNodeId ?: 0, objectId)
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

        if (node.nodeId > 0) {
            return action(node)
        }

        return false
    }
}
