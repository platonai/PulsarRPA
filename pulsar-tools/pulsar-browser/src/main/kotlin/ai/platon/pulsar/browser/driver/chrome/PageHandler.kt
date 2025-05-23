package ai.platon.pulsar.browser.driver.chrome

import ai.platon.pulsar.browser.common.ScriptConfuser
import ai.platon.pulsar.browser.driver.chrome.util.ChromeDriverException
import ai.platon.pulsar.browser.driver.chrome.util.ChromeRPCException
import ai.platon.pulsar.common.AppContext
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.math.geometric.OffsetD
import com.github.kklisura.cdt.protocol.v2023.support.annotations.Experimental
import com.github.kklisura.cdt.protocol.v2023.types.dom.Rect
import com.github.kklisura.cdt.protocol.v2023.types.page.Navigate
import com.github.kklisura.cdt.protocol.v2023.types.page.ReferrerPolicy
import com.github.kklisura.cdt.protocol.v2023.types.page.TransitionType
import com.github.kklisura.cdt.protocol.v2023.types.runtime.Evaluate
import com.github.kklisura.cdt.protocol.v2023.types.runtime.SerializationOptions
import kotlinx.coroutines.delay
import kotlin.random.Random

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
    private val runtime get() = devTools.runtime.takeIf { isActive }

    val mouse = Mouse(devTools)
    val keyboard = Keyboard(devTools)

    @Throws(ChromeDriverException::class)
    fun navigate(url: String): Navigate? {
        return pageAPI?.navigate(url)
    }

    @Throws(ChromeDriverException::class)
    fun navigate(
        url: String,
        referrer: String? = null,
        transitionType: TransitionType? = null,
        frameId: String? = null,
        referrerPolicy: ReferrerPolicy? = null
    ): Navigate? {
        return pageAPI?.navigate(url, referrer, transitionType, frameId, referrerPolicy)
    }

    /**
     * TODO: make sure the meaning of 0 node id
     * */
    @Throws(ChromeDriverException::class)
    fun querySelector(selector: String): Int? {
        return querySelectorOrNull(selector)
    }

    @Throws(ChromeDriverException::class)
    fun querySelectorAll(selector: String): List<Int> {
        return invokeOnElement(selector) { nodeId ->
            domAPI?.querySelectorAll(nodeId, selector)
        } ?: listOf()
    }

    @Throws(ChromeDriverException::class)
    fun getAttributes(selector: String): Map<String, String> {
        return invokeOnElement(selector) { nodeId ->
            domAPI?.getAttributes(nodeId)?.zipWithNext()?.toMap()
        } ?: emptyMap()
    }

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
     * */
    @Throws(ChromeDriverException::class)
    fun getAttributeAll(selector: String, attrName: String, start: Int, limit: Int): List<String> {
        return querySelectorAll(selector).asSequence().drop(start).take(limit)
            .mapNotNull { getAttribute(it, attrName) }
            .toList()
    }

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
     * @param selector - A
     * {@link https://developer.mozilla.org/en-US/docs/Web/CSS/CSS_Selectors | selector }
     * of an element to focus. If there are multiple elements satisfying the
     * selector, the first will be focused.
     * @returns  NodeId which resolves when the element matching selector is
     * successfully focused. returns 0 if there is no element
     * matching selector.
     */
    @Throws(ChromeDriverException::class)
    fun focusOnSelector(selector: String): Int {
        val rootId = domAPI?.document?.nodeId ?: return 0

        val nodeId = domAPI?.querySelector(rootId, selector)
        if (nodeId == 0) {
            return 0
        }

        domAPI?.focus(nodeId, rootId, null)

        return nodeId ?: 0
    }

    @Throws(ChromeDriverException::class)
    fun scrollIntoViewIfNeeded(selector: String, rect: Rect? = null): Int? {
        val nodeId = querySelector(selector)
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
            logger.debug(
                "DOM.scrollIntoViewIfNeeded is not supported, fallback to Element.scrollIntoView | {} | {} | {}",
                nodeId, e.message, selector
            )
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
        val evaluate = runtime?.evaluate(confuser.confuse(expression))

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
            logger.info(exception.description + "\n>>>$expression<<<")
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

    suspend fun click(nodeId: Int) {
        click(nodeId, 1)
    }

    suspend fun click(nodeId: Int, count: Int, position: String = "center") {
        click0(nodeId, count, position)
    }

    suspend fun press(nodeId: Int, key: String, delay: Long) {
        click(nodeId, 1)
        keyboard.press(key, delay)
    }

    suspend fun type(nodeId: Int, text: String, delay: Long) {
        click(nodeId, 1)
        keyboard.type(text, delay)
        delay(200)
    }

    suspend fun fill(nodeId: Int, text: String, delay: Long) {
        val value = getAttribute(nodeId, "value")
        if (value != null) {
            // it's an input element, we should click on the right side of the element,
            // so the cursor appears at the tail of the text
            click(nodeId, 1, "right")
            keyboard.delete(value.length, delay)
            // ensure the input is empty
            // page.setAttribute(nodeId, "value", "")
        }

        click(nodeId, 1)
        // For fill, there is no delay between key presses
        keyboard.type(text, 0)
    }

    @Throws(ChromeDriverException::class)
    private fun querySelectorOrNull(selector: String): Int? {
        val rootId = domAPI?.document?.nodeId
        return if (rootId != null && rootId > 0) {
            domAPI?.querySelector(rootId, selector)
        } else null
    }

    private suspend fun click0(nodeId: Int, count: Int, position: String = "center") {
        val deltaX = 4.0 + Random.nextInt(4)
        val deltaY = 4.0
        val offset = OffsetD(deltaX, deltaY)
        val minDeltaX = 2.0

        val p = pageAPI
        val d = domAPI
        if (p == null || d == null) {
            return
        }

        val clickableDOM = ClickableDOM(p, d, nodeId, offset)
        val point = clickableDOM.clickablePoint().value ?: return
        val box = clickableDOM.boundingBox()
        val width = box?.width ?: 0.0
        // if it's an input element, we should click on the right side of the element,
        // so the cursor is at the tail of the text
        var offsetX = when (position) {
            "left" -> 0.0 + deltaX
            "right" -> width - deltaX
            else -> width / 2 + deltaX
        }
        offsetX = offsetX.coerceAtMost(width - minDeltaX).coerceAtLeast(minDeltaX)

        point.x += offsetX

        mouse.click(point.x, point.y, count, 200)
    }

    @Throws(ChromeDriverException::class)
    private fun <T> invokeOnElement(selector: String, action: (Int) -> T): T? {
        val nodeId = querySelectorOrNull(selector)
        if (nodeId != null && nodeId > 0) {
            return action(nodeId)
        }

        return null
    }

    @Throws(ChromeDriverException::class)
    private fun predicateOnElement(selector: String, action: (Int) -> Boolean): Boolean {
        val nodeId = querySelectorOrNull(selector)
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
        return runtime?.evaluate(
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
