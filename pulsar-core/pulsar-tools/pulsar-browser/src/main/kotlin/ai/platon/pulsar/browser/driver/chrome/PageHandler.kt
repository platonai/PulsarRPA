package ai.platon.pulsar.browser.driver.chrome

import ai.platon.pulsar.browser.common.ScriptConfuser
import ai.platon.pulsar.browser.driver.chrome.dom.Locator
import ai.platon.pulsar.browser.driver.chrome.util.ChromeDriverException
import ai.platon.pulsar.browser.driver.chrome.util.ChromeRPCException
import ai.platon.pulsar.common.AppContext
import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.printlnPro
import com.github.kklisura.cdt.protocol.v2023.support.annotations.Experimental
import com.github.kklisura.cdt.protocol.v2023.support.annotations.Optional
import com.github.kklisura.cdt.protocol.v2023.support.annotations.ParamName
import com.github.kklisura.cdt.protocol.v2023.types.dom.Rect
import com.github.kklisura.cdt.protocol.v2023.types.page.Navigate
import com.github.kklisura.cdt.protocol.v2023.types.page.ReferrerPolicy
import com.github.kklisura.cdt.protocol.v2023.types.page.TransitionType
import com.github.kklisura.cdt.protocol.v2023.types.runtime.Evaluate
import com.github.kklisura.cdt.protocol.v2023.types.runtime.SerializationOptions

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
    fun navigate(@ParamName("url") url: String): Navigate? {
        return pageAPI?.navigate(url)
    }

    @Throws(ChromeDriverException::class)
    fun navigate(
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
    fun querySelector(selector: String): Int? {
        return resolveSelector(selector)
    }

    /**
     * Queries for all elements matching the selector.
     *
     * Note: Backend node ID format ("backend:123") will return a single-element list
     * containing that node, as backend node IDs reference a specific node.
     *
     * @param selector CSS selector or "backend:nodeId" format
     * @return List of nodeIds matching the selector
     */
    @Throws(ChromeDriverException::class)
    fun querySelectorAll(selector: String): List<Int> {
        // For backend node ID, return a single-element list
        if (selector.startsWith(BACKEND_NODE_PREFIX)) {
            val nodeId = resolveSelector(selector)
            return if (nodeId != null && nodeId > 0) listOf(nodeId) else listOf()
        } else if (selector.startsWith(FBN_PREFIX)) {

        }

        // For regular selectors, use querySelectorAll
        return invokeOnElement(selector) { nodeId ->
            domAPI?.querySelectorAll(nodeId, selector)
        } ?: listOf()
    }

    /**
     * Gets all attributes for the element matching the selector.
     *
     * @param selector CSS selector or "backend:nodeId" format
     * @return Map of attribute name to value
     */
    @Throws(ChromeDriverException::class)
    fun getAttributes(selector: String): Map<String, String> {
        return invokeOnElement(selector) { nodeId ->
            domAPI?.getAttributes(nodeId)?.zipWithNext()?.toMap()
        } ?: emptyMap()
    }

    /**
     * Gets a specific attribute value for the element matching the selector.
     *
     * @param selector CSS selector or "backend:nodeId" format
     * @param attrName Attribute name to retrieve
     * @return Attribute value or null if not found
     */
    @Throws(ChromeDriverException::class)
    fun getAttribute(selector: String, attrName: String) = invokeOnElement(selector) { getAttribute(it, attrName) }

    @Throws(ChromeDriverException::class)
    fun getAttribute(nodeId: Int, attrName: String): String? {
        // `attributes`: n1, v1, n2, v2, n3, v3, ...
        val attributes = domAPI?.getAttributes(nodeId) ?: return null
        val nameIndex = attributes.indexOf(attrName)
        if (nameIndex < 0) {
            return null
        }
        val valueIndex = nameIndex + 1
        return attributes.getOrNull(valueIndex)
    }

    @Throws(ChromeDriverException::class)
    fun setAttribute(nodeId: Int, attrName: String, attrValue: String) {
        domAPI?.setAttributeValue(nodeId, attrName, attrValue)
    }

    /**
     * TODO: too many requests, need to optimize
     * RobustRPC - Too many RPC failures: selectAttributeAll (6/5) | DOM Error while querying
     *
     * getAttributeAll performs a query + RPC per node (N+1). Batched attribute fetch via DOM.getAttributes
     * or DOM.collectClassNamesFromSubtree-style calls would dramatically cut traffic.
     *
     * @param selector CSS selector or "backend:nodeId" format
     * @param attrName Attribute name to retrieve
     * @param start Starting index (0-based)
     * @param limit Maximum number of results
     * @return List of attribute values
     */
    @Throws(ChromeDriverException::class)
    fun getAttributeAll(selector: String, attrName: String, start: Int, limit: Int): List<String> {
        return querySelectorAll(selector).asSequence().drop(start).take(limit)
            .mapNotNull { getAttribute(it, attrName) }
            .toList()
    }

    /**
     * Checks if the element matching the selector is visible.
     *
     * @param selector CSS selector or "backend:nodeId" format
     * @return true if visible, false otherwise
     */
    @Throws(ChromeDriverException::class)
    fun visible(selector: String) = predicateOnElement(selector) { visible(it) }

    @Throws(ChromeDriverException::class)
    fun visible(nodeId: Int): Boolean {
        if (nodeId <= 0) {
            return false
        }

        var isVisible = true

        val properties = cssAPI?.getComputedStyleForNode(nodeId)
        properties?.forEach { prop ->
            when {
                prop.name == "display" && prop.value == "none" -> isVisible = false
                prop.name == "visibility" && prop.value == "hidden" -> isVisible = false
                prop.name == "opacity" && prop.value == "0" -> isVisible = false
            }
        }

        if (isVisible) {
            isVisible = ClickableDOM.create(pageAPI, domAPI, nodeId)?.isVisible() ?: false
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
    fun focusOnSelector(selector: String): Int {
        val nodeId = resolveSelector(selector)
        if (nodeId == null || nodeId == 0) {
            return 0
        }

        // Fix: Only use nodeId parameter, others should be null
        domAPI?.focus(nodeId, null, null)

        return nodeId
    }

    /**
     * Scrolls the element into view if needed.
     *
     * @param selector CSS selector or "backend:nodeId" format
     * @param rect Optional rectangle to scroll into view
     * @return nodeId of the element, or null if not found
     */
    @Throws(ChromeDriverException::class)
    fun scrollIntoViewIfNeeded(selector: String, rect: Rect? = null): Int? {
        val nodeId = resolveSelector(selector)
        if (nodeId == null || nodeId == 0) {
            logger.info("No node found for selector: $selector")
            return null
        }

        return scrollIntoViewIfNeeded(nodeId, selector, rect)
    }

    @Throws(ChromeDriverException::class)
    fun scrollIntoViewIfNeeded(nodeId: Int, selector: String? = null, rect: Rect? = null): Int? {
        try {
            val node = domAPI?.describeNode(nodeId, null, null, null, false)
            if (node?.nodeType != ELEMENT_NODE) {
                logger.info("Node is not of type HTMLElement | {}", selector ?: nodeId)
                return null
            }

            domAPI?.scrollIntoViewIfNeeded(nodeId, node.backendNodeId, null, rect)
        } catch (e: ChromeRPCException) {
            logger.debug("DOM.scrollIntoViewIfNeeded is not supported, fallback to Element.scrollIntoView | {} | {} | {}",
                nodeId, e.message, selector)
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
    @Throws(ChromeDriverException::class)
    fun evaluateDetail(expression: String): Evaluate? {
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
    fun evaluate(expression: String): Any? {
        val evaluate = evaluateDetail(expression)

        val exception = evaluate?.exceptionDetails?.exception
        if (exception != null) {
            logger.warn(exception.description + "\n>>>$expression<<<")
        }

        val result = evaluate?.result
        return result?.value
    }

    @Throws(ChromeDriverException::class)
    fun evaluateValueDetail(expression: String): Evaluate? {
//        val iife = JsUtils.toIIFE(confuser.confuse(expression))
//        return evaluate(iife, returnByValue = true)
        val expression2 = confuser.confuse(expression)
        return cdpEvaluate(expression2, returnByValue = true)
    }

    /**
     * Evaluates expression on global object.
     *
     * @param expression Javascript expression to evaluate
     * @return Remote object value in case of primitive values or JSON values (if it was requested).
     * */
    @Throws(ChromeDriverException::class)
    fun evaluateValue(expression: String): Any? {
        val evaluate = evaluateValueDetail(expression)

        val exception = evaluate?.exceptionDetails?.exception
        if (exception != null) {
            logger.info(exception.description + "\n>>>$expression<<<")
        }

        return evaluate?.result?.value
    }

    @Throws(ChromeDriverException::class)
    private fun querySelectorOrNull(selector: String): Int? {
        val rootId = domAPI?.document?.nodeId
        return if (rootId != null && rootId > 0) {
            domAPI?.querySelector(rootId, selector)
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
    private fun resolveSelector(selector: String): Int? {
        val locator = Locator.parse(selector) ?: return null

        return when (locator.type) {
            Locator.Type.CSS_PATH -> querySelectorOrNull(selector)
            Locator.Type.BACKEND_NODE_ID -> {
                val backendNodeId = locator.selector.toIntOrNull()
                if (backendNodeId == null) {
                    logger.warn("Invalid backend node ID format: '{}'", selector)
                    return null
                }
                resolveBackendNodeId(backendNodeId)
            }
            Locator.Type.FRAME_BACKEND_NODE_ID -> {
                val backendNodeId = selector.substringAfterLast(",").toIntOrNull()
                resolveBackendNodeId(backendNodeId)
            }
            else -> throw UnsupportedOperationException("Unsupported selector $selector")
        }
    }

    /**
     * Resolves a backend node ID to a regular node ID.
     *
     * @param backendNodeId The backend node ID
     * @return nodeId or null if resolution fails
     */
    @Throws(ChromeDriverException::class)
    private fun resolveBackendNodeId(backendNodeId: Int?): Int? {
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
    private fun <T> invokeOnElement(selector: String, action: (Int) -> T): T? {
        val nodeId = resolveSelector(selector)
        if (nodeId != null && nodeId > 0) {
            return action(nodeId)
        }

        return null
    }

    @Throws(ChromeDriverException::class)
    private fun predicateOnElement(selector: String, action: (Int) -> Boolean): Boolean {
        val nodeId = resolveSelector(selector)
        if (nodeId != null && nodeId > 0) {
            return action(nodeId)
        }

        return false
    }

    private fun cdpEvaluate(
        expression: String,
        objectGroup: String? = null,
        includeCommandLineAPI: Boolean? = null,
        silent: Boolean? = null,
        contextId: Int? = null,
        returnByValue: Boolean? = null,
        @Experimental generatePreview: Boolean? = null,
        userGesture: Boolean? = null,
        awaitPromise: Boolean? = null,
        @Experimental throwOnSideEffect: Boolean? = null,
        @Experimental timeout: Double? = null,
        @Experimental disableBreaks: Boolean? = null,
        @Experimental replMode: Boolean? = null,
        @Experimental allowUnsafeEvalBlockedByCSP: Boolean? = null,
        @Experimental uniqueContextId: String? = null,
        @Experimental serializationOptions: SerializationOptions? = null,
    ): Evaluate? {
        return runtimeAPI?.evaluate(
            expression,
            objectGroup,
            includeCommandLineAPI,
            silent,
            contextId,
            returnByValue,
            generatePreview,
            userGesture,
            awaitPromise,
            throwOnSideEffect,
            timeout,
            disableBreaks,
            replMode,
            allowUnsafeEvalBlockedByCSP,
            uniqueContextId,
            null,
            serializationOptions
        )
    }
}
