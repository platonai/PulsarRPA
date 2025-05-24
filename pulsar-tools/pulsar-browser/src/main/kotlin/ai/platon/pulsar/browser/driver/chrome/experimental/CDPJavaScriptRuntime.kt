package ai.platon.pulsar.browser.driver.chrome.experimental

import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.browser.driver.chrome.RemoteDevTools
import com.github.kklisura.cdt.protocol.v2023.types.runtime.CallArgument
import com.github.kklisura.cdt.protocol.v2023.types.runtime.CallFunctionOn
import com.github.kklisura.cdt.protocol.v2023.types.runtime.SerializationOptions

class CDPJavaScriptRuntime(
    private val devTools: RemoteDevTools,
    private val settings: BrowserSettings,
) {
    private val runtime get() = devTools.runtime
    private val domAPI get() = devTools.dom

    /**
     * Demonstrates how to resolve a DOM node to a remote object and then call a function on it
     * to extract text content
     */
    fun extractTextFromElement(nodeId: Int): String? {
        // Step 1: Resolve the DOM node to a JavaScript object reference
        val resolveResult = domAPI?.resolveNode(nodeId, null, null, null)
        val objectId = resolveResult?.objectId ?: return null

        // Step 2: Define the function to extract text content
        val functionDeclaration = """
        function() {
            // Extract text content from this node
            return {
                textContent: this.textContent,
                innerText: this.innerText,
                value: this.value
            };
        }
    """.trimIndent()

        // Step 3: Call the function on the resolved object
        val callResult = cdpCallFunctionOn(
            functionDeclaration,
            objectId = objectId,
            returnByValue = true,
            awaitPromise = true
        )

        // Step 4: Release the object to avoid memory leaks
        devTools.runtime.releaseObject(objectId)

        // Step 5: Extract and return the text content from the result
        val result = callResult?.result?.value ?: return null

        // Handle the result based on its type
//        return when {
//            result?.asMap()?.get("textContent") != null -> result.asMap()["textContent"].toString()
//            result?.asMap()?.get("innerText") != null -> result.asMap()["innerText"].toString()
//            result?.asMap()?.get("value") != null -> result.asMap()["value"].toString()
//            else -> null
//        }

        return result.toString()
    }

    private fun cdpCallFunctionOn(
        functionDeclaration: String,
        objectId: String? = null,
        arguments: List<CallArgument?>? = null,
        silent: Boolean? = null,
        returnByValue: Boolean? = null,
        generatePreview: Boolean? = null,
        userGesture: Boolean? = null,
        awaitPromise: Boolean? = null,
        executionContextId: Int? = null,
        objectGroup: String? = null,
        throwOnSideEffect: Boolean? = null,
        uniqueContextId: String? = null,
        generateWebDriverValue: Boolean? = null,
        serializationOptions: SerializationOptions? = null
    ): CallFunctionOn? {
        return runtime?.callFunctionOn(
            functionDeclaration,
            objectId,
            arguments,
            silent,
            returnByValue,
            generatePreview,
            userGesture,
            awaitPromise,
            executionContextId,
            objectGroup,
            throwOnSideEffect,
            uniqueContextId,
            generateWebDriverValue,
            serializationOptions
        )
    }
}
