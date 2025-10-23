@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.commands

import ai.platon.cdt.kt.protocol.events.runtime.BindingCalled
import ai.platon.cdt.kt.protocol.events.runtime.ConsoleAPICalled
import ai.platon.cdt.kt.protocol.events.runtime.ExceptionRevoked
import ai.platon.cdt.kt.protocol.events.runtime.ExceptionThrown
import ai.platon.cdt.kt.protocol.events.runtime.ExecutionContextCreated
import ai.platon.cdt.kt.protocol.events.runtime.ExecutionContextDestroyed
import ai.platon.cdt.kt.protocol.events.runtime.ExecutionContextsCleared
import ai.platon.cdt.kt.protocol.events.runtime.InspectRequested
import ai.platon.cdt.kt.protocol.support.annotations.EventName
import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import ai.platon.cdt.kt.protocol.support.annotations.ParamName
import ai.platon.cdt.kt.protocol.support.annotations.ReturnTypeParameter
import ai.platon.cdt.kt.protocol.support.annotations.Returns
import ai.platon.cdt.kt.protocol.support.types.EventHandler
import ai.platon.cdt.kt.protocol.support.types.EventListener
import ai.platon.cdt.kt.protocol.types.runtime.AwaitPromise
import ai.platon.cdt.kt.protocol.types.runtime.CallArgument
import ai.platon.cdt.kt.protocol.types.runtime.CallFunctionOn
import ai.platon.cdt.kt.protocol.types.runtime.CompileScript
import ai.platon.cdt.kt.protocol.types.runtime.Evaluate
import ai.platon.cdt.kt.protocol.types.runtime.HeapUsage
import ai.platon.cdt.kt.protocol.types.runtime.Properties
import ai.platon.cdt.kt.protocol.types.runtime.RemoteObject
import ai.platon.cdt.kt.protocol.types.runtime.RunScript
import kotlin.Boolean
import kotlin.Double
import kotlin.Int
import kotlin.String
import kotlin.Unit
import kotlin.collections.List

/**
 * Runtime domain exposes JavaScript runtime by means of remote evaluation and mirror objects.
 * Evaluation results are returned as mirror object that expose object type, string representation
 * and unique identifier that can be used for further object reference. Original objects are
 * maintained in memory unless they are either explicitly released or are released along with the
 * other objects in their object group.
 */
interface Runtime {
  /**
   * Add handler to promise with given promise object id.
   * @param promiseObjectId Identifier of the promise.
   * @param returnByValue Whether the result is expected to be a JSON object that should be sent by value.
   * @param generatePreview Whether preview should be generated for the result.
   */
  suspend fun awaitPromise(
    @ParamName("promiseObjectId") promiseObjectId: String,
    @ParamName("returnByValue") @Optional returnByValue: Boolean? = null,
    @ParamName("generatePreview") @Optional generatePreview: Boolean? = null,
  ): AwaitPromise

  suspend fun awaitPromise(@ParamName("promiseObjectId") promiseObjectId: String): AwaitPromise {
    return awaitPromise(promiseObjectId, null, null)
  }

  /**
   * Calls function with given declaration on the given object. Object group of the result is
   * inherited from the target object.
   * @param functionDeclaration Declaration of the function to call.
   * @param objectId Identifier of the object to call function on. Either objectId or executionContextId should
   * be specified.
   * @param arguments Call arguments. All call arguments must belong to the same JavaScript world as the target
   * object.
   * @param silent In silent mode exceptions thrown during evaluation are not reported and do not pause
   * execution. Overrides `setPauseOnException` state.
   * @param returnByValue Whether the result is expected to be a JSON object which should be sent by value.
   * @param generatePreview Whether preview should be generated for the result.
   * @param userGesture Whether execution should be treated as initiated by user in the UI.
   * @param awaitPromise Whether execution should `await` for resulting value and return once awaited promise is
   * resolved.
   * @param executionContextId Specifies execution context which global object will be used to call function on. Either
   * executionContextId or objectId should be specified.
   * @param objectGroup Symbolic group name that can be used to release multiple objects. If objectGroup is not
   * specified and objectId is, objectGroup will be inherited from object.
   */
  suspend fun callFunctionOn(
    @ParamName("functionDeclaration") functionDeclaration: String,
    @ParamName("objectId") @Optional objectId: String? = null,
    @ParamName("arguments") @Optional arguments: List<CallArgument>? = null,
    @ParamName("silent") @Optional silent: Boolean? = null,
    @ParamName("returnByValue") @Optional returnByValue: Boolean? = null,
    @ParamName("generatePreview") @Optional @Experimental generatePreview: Boolean? = null,
    @ParamName("userGesture") @Optional userGesture: Boolean? = null,
    @ParamName("awaitPromise") @Optional awaitPromise: Boolean? = null,
    @ParamName("executionContextId") @Optional executionContextId: Int? = null,
    @ParamName("objectGroup") @Optional objectGroup: String? = null,
  ): CallFunctionOn

  suspend fun callFunctionOn(@ParamName("functionDeclaration") functionDeclaration: String): CallFunctionOn {
    return callFunctionOn(functionDeclaration, null, null, null, null, null, null, null, null, null)
  }

  /**
   * Compiles expression.
   * @param expression Expression to compile.
   * @param sourceURL Source url to be set for the script.
   * @param persistScript Specifies whether the compiled script should be persisted.
   * @param executionContextId Specifies in which execution context to perform script run. If the parameter is omitted the
   * evaluation will be performed in the context of the inspected page.
   */
  suspend fun compileScript(
    @ParamName("expression") expression: String,
    @ParamName("sourceURL") sourceURL: String,
    @ParamName("persistScript") persistScript: Boolean,
    @ParamName("executionContextId") @Optional executionContextId: Int? = null,
  ): CompileScript

  suspend fun compileScript(
    @ParamName("expression") expression: String,
    @ParamName("sourceURL") sourceURL: String,
    @ParamName("persistScript") persistScript: Boolean,
  ): CompileScript {
    return compileScript(expression, sourceURL, persistScript, null)
  }

  /**
   * Disables reporting of execution contexts creation.
   */
  suspend fun disable()

  /**
   * Discards collected exceptions and console API calls.
   */
  suspend fun discardConsoleEntries()

  /**
   * Enables reporting of execution contexts creation by means of `executionContextCreated` event.
   * When the reporting gets enabled the event will be sent immediately for each existing execution
   * context.
   */
  suspend fun enable()

  /**
   * Evaluates expression on global object.
   * @param expression Expression to evaluate.
   * @param objectGroup Symbolic group name that can be used to release multiple objects.
   * @param includeCommandLineAPI Determines whether Command Line API should be available during the evaluation.
   * @param silent In silent mode exceptions thrown during evaluation are not reported and do not pause
   * execution. Overrides `setPauseOnException` state.
   * @param contextId Specifies in which execution context to perform evaluation. If the parameter is omitted the
   * evaluation will be performed in the context of the inspected page.
   * This is mutually exclusive with `uniqueContextId`, which offers an
   * alternative way to identify the execution context that is more reliable
   * in a multi-process environment.
   * @param returnByValue Whether the result is expected to be a JSON object that should be sent by value.
   * @param generatePreview Whether preview should be generated for the result.
   * @param userGesture Whether execution should be treated as initiated by user in the UI.
   * @param awaitPromise Whether execution should `await` for resulting value and return once awaited promise is
   * resolved.
   * @param throwOnSideEffect Whether to throw an exception if side effect cannot be ruled out during evaluation.
   * This implies `disableBreaks` below.
   * @param timeout Terminate execution after timing out (number of milliseconds).
   * @param disableBreaks Disable breakpoints during execution.
   * @param replMode Setting this flag to true enables `let` re-declaration and top-level `await`.
   * Note that `let` variables can only be re-declared if they originate from
   * `replMode` themselves.
   * @param allowUnsafeEvalBlockedByCSP The Content Security Policy (CSP) for the target might block 'unsafe-eval'
   * which includes eval(), Function(), setTimeout() and setInterval()
   * when called with non-callable arguments. This flag bypasses CSP for this
   * evaluation and allows unsafe-eval. Defaults to true.
   * @param uniqueContextId An alternative way to specify the execution context to evaluate in.
   * Compared to contextId that may be reused accross processes, this is guaranteed to be
   * system-unique, so it can be used to prevent accidental evaluation of the expression
   * in context different than intended (e.g. as a result of navigation accross process
   * boundaries).
   * This is mutually exclusive with `contextId`.
   */
  suspend fun evaluate(
    @ParamName("expression") expression: String,
    @ParamName("objectGroup") @Optional objectGroup: String? = null,
    @ParamName("includeCommandLineAPI") @Optional includeCommandLineAPI: Boolean? = null,
    @ParamName("silent") @Optional silent: Boolean? = null,
    @ParamName("contextId") @Optional contextId: Int? = null,
    @ParamName("returnByValue") @Optional returnByValue: Boolean? = null,
    @ParamName("generatePreview") @Optional @Experimental generatePreview: Boolean? = null,
    @ParamName("userGesture") @Optional userGesture: Boolean? = null,
    @ParamName("awaitPromise") @Optional awaitPromise: Boolean? = null,
    @ParamName("throwOnSideEffect") @Optional @Experimental throwOnSideEffect: Boolean? = null,
    @ParamName("timeout") @Optional @Experimental timeout: Double? = null,
    @ParamName("disableBreaks") @Optional @Experimental disableBreaks: Boolean? = null,
    @ParamName("replMode") @Optional @Experimental replMode: Boolean? = null,
    @ParamName("allowUnsafeEvalBlockedByCSP") @Optional @Experimental allowUnsafeEvalBlockedByCSP: Boolean? = null,
    @ParamName("uniqueContextId") @Optional @Experimental uniqueContextId: String? = null,
  ): Evaluate

  suspend fun evaluate(@ParamName("expression") expression: String): Evaluate {
    return evaluate(expression, null, null, null, null, null, null, null, null, null, null, null, null, null, null)
  }

  /**
   * Returns the isolate id.
   */
  @Experimental
  @Returns("id")
  suspend fun getIsolateId(): String

  /**
   * Returns the JavaScript heap usage.
   * It is the total usage of the corresponding isolate not scoped to a particular Runtime.
   */
  @Experimental
  suspend fun getHeapUsage(): HeapUsage

  /**
   * Returns properties of a given object. Object group of the result is inherited from the target
   * object.
   * @param objectId Identifier of the object to return properties for.
   * @param ownProperties If true, returns properties belonging only to the element itself, not to its prototype
   * chain.
   * @param accessorPropertiesOnly If true, returns accessor properties (with getter/setter) only; internal properties are not
   * returned either.
   * @param generatePreview Whether preview should be generated for the results.
   */
  suspend fun getProperties(
    @ParamName("objectId") objectId: String,
    @ParamName("ownProperties") @Optional ownProperties: Boolean? = null,
    @ParamName("accessorPropertiesOnly") @Optional @Experimental accessorPropertiesOnly: Boolean? = null,
    @ParamName("generatePreview") @Optional @Experimental generatePreview: Boolean? = null,
  ): Properties

  suspend fun getProperties(@ParamName("objectId") objectId: String): Properties {
    return getProperties(objectId, null, null, null)
  }

  /**
   * Returns all let, const and class variables from global scope.
   * @param executionContextId Specifies in which execution context to lookup global scope variables.
   */
  @Returns("names")
  @ReturnTypeParameter(String::class)
  suspend fun globalLexicalScopeNames(@ParamName("executionContextId") @Optional executionContextId: Int? = null): List<String>

  @Returns("names")
  @ReturnTypeParameter(String::class)
  suspend fun globalLexicalScopeNames(): List<String> {
    return globalLexicalScopeNames(null)
  }

  /**
   * @param prototypeObjectId Identifier of the prototype to return objects for.
   * @param objectGroup Symbolic group name that can be used to release the results.
   */
  @Returns("objects")
  suspend fun queryObjects(@ParamName("prototypeObjectId") prototypeObjectId: String, @ParamName("objectGroup") @Optional objectGroup: String? = null): RemoteObject

  @Returns("objects")
  suspend fun queryObjects(@ParamName("prototypeObjectId") prototypeObjectId: String): RemoteObject {
    return queryObjects(prototypeObjectId, null)
  }

  /**
   * Releases remote object with given id.
   * @param objectId Identifier of the object to release.
   */
  suspend fun releaseObject(@ParamName("objectId") objectId: String)

  /**
   * Releases all remote objects that belong to a given group.
   * @param objectGroup Symbolic object group name.
   */
  suspend fun releaseObjectGroup(@ParamName("objectGroup") objectGroup: String)

  /**
   * Tells inspected instance to run if it was waiting for debugger to attach.
   */
  suspend fun runIfWaitingForDebugger()

  /**
   * Runs script with given id in a given context.
   * @param scriptId Id of the script to run.
   * @param executionContextId Specifies in which execution context to perform script run. If the parameter is omitted the
   * evaluation will be performed in the context of the inspected page.
   * @param objectGroup Symbolic group name that can be used to release multiple objects.
   * @param silent In silent mode exceptions thrown during evaluation are not reported and do not pause
   * execution. Overrides `setPauseOnException` state.
   * @param includeCommandLineAPI Determines whether Command Line API should be available during the evaluation.
   * @param returnByValue Whether the result is expected to be a JSON object which should be sent by value.
   * @param generatePreview Whether preview should be generated for the result.
   * @param awaitPromise Whether execution should `await` for resulting value and return once awaited promise is
   * resolved.
   */
  suspend fun runScript(
    @ParamName("scriptId") scriptId: String,
    @ParamName("executionContextId") @Optional executionContextId: Int? = null,
    @ParamName("objectGroup") @Optional objectGroup: String? = null,
    @ParamName("silent") @Optional silent: Boolean? = null,
    @ParamName("includeCommandLineAPI") @Optional includeCommandLineAPI: Boolean? = null,
    @ParamName("returnByValue") @Optional returnByValue: Boolean? = null,
    @ParamName("generatePreview") @Optional generatePreview: Boolean? = null,
    @ParamName("awaitPromise") @Optional awaitPromise: Boolean? = null,
  ): RunScript

  suspend fun runScript(@ParamName("scriptId") scriptId: String): RunScript {
    return runScript(scriptId, null, null, null, null, null, null, null)
  }

  /**
   * @param enabled
   */
  @Experimental
  suspend fun setCustomObjectFormatterEnabled(@ParamName("enabled") enabled: Boolean)

  /**
   * @param size
   */
  @Experimental
  suspend fun setMaxCallStackSizeToCapture(@ParamName("size") size: Int)

  /**
   * Terminate current or next JavaScript execution.
   * Will cancel the termination when the outer-most script execution ends.
   */
  @Experimental
  suspend fun terminateExecution()

  /**
   * If executionContextId is empty, adds binding with the given name on the
   * global objects of all inspected contexts, including those created later,
   * bindings survive reloads.
   * Binding function takes exactly one argument, this argument should be string,
   * in case of any other input, function throws an exception.
   * Each binding function call produces Runtime.bindingCalled notification.
   * @param name
   * @param executionContextId If specified, the binding would only be exposed to the specified
   * execution context. If omitted and `executionContextName` is not set,
   * the binding is exposed to all execution contexts of the target.
   * This parameter is mutually exclusive with `executionContextName`.
   * @param executionContextName If specified, the binding is exposed to the executionContext with
   * matching name, even for contexts created after the binding is added.
   * See also `ExecutionContext.name` and `worldName` parameter to
   * `Page.addScriptToEvaluateOnNewDocument`.
   * This parameter is mutually exclusive with `executionContextId`.
   */
  @Experimental
  suspend fun addBinding(
    @ParamName("name") name: String,
    @ParamName("executionContextId") @Optional executionContextId: Int? = null,
    @ParamName("executionContextName") @Optional @Experimental executionContextName: String? = null,
  )

  @Experimental
  suspend fun addBinding(@ParamName("name") name: String) {
    return addBinding(name, null, null)
  }

  /**
   * This method does not remove binding function from global object but
   * unsubscribes current runtime agent from Runtime.bindingCalled notifications.
   * @param name
   */
  @Experimental
  suspend fun removeBinding(@ParamName("name") name: String)

  @EventName("bindingCalled")
  @Experimental
  fun onBindingCalled(eventListener: EventHandler<BindingCalled>): EventListener

  @EventName("bindingCalled")
  @Experimental
  fun onBindingCalled(eventListener: suspend (BindingCalled) -> Unit): EventListener

  @EventName("consoleAPICalled")
  fun onConsoleAPICalled(eventListener: EventHandler<ConsoleAPICalled>): EventListener

  @EventName("consoleAPICalled")
  fun onConsoleAPICalled(eventListener: suspend (ConsoleAPICalled) -> Unit): EventListener

  @EventName("exceptionRevoked")
  fun onExceptionRevoked(eventListener: EventHandler<ExceptionRevoked>): EventListener

  @EventName("exceptionRevoked")
  fun onExceptionRevoked(eventListener: suspend (ExceptionRevoked) -> Unit): EventListener

  @EventName("exceptionThrown")
  fun onExceptionThrown(eventListener: EventHandler<ExceptionThrown>): EventListener

  @EventName("exceptionThrown")
  fun onExceptionThrown(eventListener: suspend (ExceptionThrown) -> Unit): EventListener

  @EventName("executionContextCreated")
  fun onExecutionContextCreated(eventListener: EventHandler<ExecutionContextCreated>): EventListener

  @EventName("executionContextCreated")
  fun onExecutionContextCreated(eventListener: suspend (ExecutionContextCreated) -> Unit): EventListener

  @EventName("executionContextDestroyed")
  fun onExecutionContextDestroyed(eventListener: EventHandler<ExecutionContextDestroyed>): EventListener

  @EventName("executionContextDestroyed")
  fun onExecutionContextDestroyed(eventListener: suspend (ExecutionContextDestroyed) -> Unit): EventListener

  @EventName("executionContextsCleared")
  fun onExecutionContextsCleared(eventListener: EventHandler<ExecutionContextsCleared>): EventListener

  @EventName("executionContextsCleared")
  fun onExecutionContextsCleared(eventListener: suspend (ExecutionContextsCleared) -> Unit): EventListener

  @EventName("inspectRequested")
  fun onInspectRequested(eventListener: EventHandler<InspectRequested>): EventListener

  @EventName("inspectRequested")
  fun onInspectRequested(eventListener: suspend (InspectRequested) -> Unit): EventListener
}
