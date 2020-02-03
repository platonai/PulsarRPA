package com.github.kklisura.cdt.protocol.commands;

/*-
 * #%L
 * cdt-java-client
 * %%
 * Copyright (C) 2018 - 2019 Kenan Klisura
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.github.kklisura.cdt.protocol.events.runtime.BindingCalled;
import com.github.kklisura.cdt.protocol.events.runtime.ConsoleAPICalled;
import com.github.kklisura.cdt.protocol.events.runtime.ExceptionRevoked;
import com.github.kklisura.cdt.protocol.events.runtime.ExceptionThrown;
import com.github.kklisura.cdt.protocol.events.runtime.ExecutionContextCreated;
import com.github.kklisura.cdt.protocol.events.runtime.ExecutionContextDestroyed;
import com.github.kklisura.cdt.protocol.events.runtime.ExecutionContextsCleared;
import com.github.kklisura.cdt.protocol.events.runtime.InspectRequested;
import com.github.kklisura.cdt.protocol.support.annotations.EventName;
import com.github.kklisura.cdt.protocol.support.annotations.Experimental;
import com.github.kklisura.cdt.protocol.support.annotations.Optional;
import com.github.kklisura.cdt.protocol.support.annotations.ParamName;
import com.github.kklisura.cdt.protocol.support.annotations.ReturnTypeParameter;
import com.github.kklisura.cdt.protocol.support.annotations.Returns;
import com.github.kklisura.cdt.protocol.support.types.EventHandler;
import com.github.kklisura.cdt.protocol.support.types.EventListener;
import com.github.kklisura.cdt.protocol.types.runtime.AwaitPromise;
import com.github.kklisura.cdt.protocol.types.runtime.CallArgument;
import com.github.kklisura.cdt.protocol.types.runtime.CallFunctionOn;
import com.github.kklisura.cdt.protocol.types.runtime.CompileScript;
import com.github.kklisura.cdt.protocol.types.runtime.Evaluate;
import com.github.kklisura.cdt.protocol.types.runtime.HeapUsage;
import com.github.kklisura.cdt.protocol.types.runtime.Properties;
import com.github.kklisura.cdt.protocol.types.runtime.RemoteObject;
import com.github.kklisura.cdt.protocol.types.runtime.RunScript;
import java.util.List;

/**
 * Runtime domain exposes JavaScript runtime by means of remote evaluation and mirror objects.
 * Evaluation results are returned as mirror object that expose object type, string representation
 * and unique identifier that can be used for further object reference. Original objects are
 * maintained in memory unless they are either explicitly released or are released along with the
 * other objects in their object group.
 */
public interface Runtime {

  /**
   * Add handler to promise with given promise object id.
   *
   * @param promiseObjectId Identifier of the promise.
   */
  AwaitPromise awaitPromise(@ParamName("promiseObjectId") String promiseObjectId);

  /**
   * Add handler to promise with given promise object id.
   *
   * @param promiseObjectId Identifier of the promise.
   * @param returnByValue Whether the result is expected to be a JSON object that should be sent by
   *     value.
   * @param generatePreview Whether preview should be generated for the result.
   */
  AwaitPromise awaitPromise(
      @ParamName("promiseObjectId") String promiseObjectId,
      @Optional @ParamName("returnByValue") Boolean returnByValue,
      @Optional @ParamName("generatePreview") Boolean generatePreview);

  /**
   * Calls function with given declaration on the given object. Object group of the result is
   * inherited from the target object.
   *
   * @param functionDeclaration Declaration of the function to call.
   */
  CallFunctionOn callFunctionOn(@ParamName("functionDeclaration") String functionDeclaration);

  /**
   * Calls function with given declaration on the given object. Object group of the result is
   * inherited from the target object.
   *
   * @param functionDeclaration Declaration of the function to call.
   * @param objectId Identifier of the object to call function on. Either objectId or
   *     executionContextId should be specified.
   * @param arguments Call arguments. All call arguments must belong to the same JavaScript world as
   *     the target object.
   * @param silent In silent mode exceptions thrown during evaluation are not reported and do not
   *     pause execution. Overrides `setPauseOnException` state.
   * @param returnByValue Whether the result is expected to be a JSON object which should be sent by
   *     value.
   * @param generatePreview Whether preview should be generated for the result.
   * @param userGesture Whether execution should be treated as initiated by user in the UI.
   * @param awaitPromise Whether execution should `await` for resulting value and return once
   *     awaited promise is resolved.
   * @param executionContextId Specifies execution context which global object will be used to call
   *     function on. Either executionContextId or objectId should be specified.
   * @param objectGroup Symbolic group name that can be used to release multiple objects. If
   *     objectGroup is not specified and objectId is, objectGroup will be inherited from object.
   */
  CallFunctionOn callFunctionOn(
      @ParamName("functionDeclaration") String functionDeclaration,
      @Optional @ParamName("objectId") String objectId,
      @Optional @ParamName("arguments") List<CallArgument> arguments,
      @Optional @ParamName("silent") Boolean silent,
      @Optional @ParamName("returnByValue") Boolean returnByValue,
      @Experimental @Optional @ParamName("generatePreview") Boolean generatePreview,
      @Optional @ParamName("userGesture") Boolean userGesture,
      @Optional @ParamName("awaitPromise") Boolean awaitPromise,
      @Optional @ParamName("executionContextId") Integer executionContextId,
      @Optional @ParamName("objectGroup") String objectGroup);

  /**
   * Compiles expression.
   *
   * @param expression Expression to compile.
   * @param sourceURL Source url to be set for the script.
   * @param persistScript Specifies whether the compiled script should be persisted.
   */
  CompileScript compileScript(
      @ParamName("expression") String expression,
      @ParamName("sourceURL") String sourceURL,
      @ParamName("persistScript") Boolean persistScript);

  /**
   * Compiles expression.
   *
   * @param expression Expression to compile.
   * @param sourceURL Source url to be set for the script.
   * @param persistScript Specifies whether the compiled script should be persisted.
   * @param executionContextId Specifies in which execution context to perform script run. If the
   *     parameter is omitted the evaluation will be performed in the context of the inspected page.
   */
  CompileScript compileScript(
      @ParamName("expression") String expression,
      @ParamName("sourceURL") String sourceURL,
      @ParamName("persistScript") Boolean persistScript,
      @Optional @ParamName("executionContextId") Integer executionContextId);

  /** Disables reporting of execution contexts creation. */
  void disable();

  /** Discards collected exceptions and console API calls. */
  void discardConsoleEntries();

  /**
   * Enables reporting of execution contexts creation by means of `executionContextCreated` event.
   * When the reporting gets enabled the event will be sent immediately for each existing execution
   * context.
   */
  void enable();

  /**
   * Evaluates expression on global object.
   *
   * @param expression Expression to evaluate.
   */
  Evaluate evaluate(@ParamName("expression") String expression);

  /**
   * Evaluates expression on global object.
   *
   * @param expression Expression to evaluate.
   * @param objectGroup Symbolic group name that can be used to release multiple objects.
   * @param includeCommandLineAPI Determines whether Command Line API should be available during the
   *     evaluation.
   * @param silent In silent mode exceptions thrown during evaluation are not reported and do not
   *     pause execution. Overrides `setPauseOnException` state.
   * @param contextId Specifies in which execution context to perform evaluation. If the parameter
   *     is omitted the evaluation will be performed in the context of the inspected page.
   * @param returnByValue Whether the result is expected to be a JSON object that should be sent by
   *     value.
   * @param generatePreview Whether preview should be generated for the result.
   * @param userGesture Whether execution should be treated as initiated by user in the UI.
   * @param awaitPromise Whether execution should `await` for resulting value and return once
   *     awaited promise is resolved.
   * @param throwOnSideEffect Whether to throw an exception if side effect cannot be ruled out
   *     during evaluation.
   * @param timeout Terminate execution after timing out (number of milliseconds).
   */
  Evaluate evaluate(
      @ParamName("expression") String expression,
      @Optional @ParamName("objectGroup") String objectGroup,
      @Optional @ParamName("includeCommandLineAPI") Boolean includeCommandLineAPI,
      @Optional @ParamName("silent") Boolean silent,
      @Optional @ParamName("contextId") Integer contextId,
      @Optional @ParamName("returnByValue") Boolean returnByValue,
      @Experimental @Optional @ParamName("generatePreview") Boolean generatePreview,
      @Optional @ParamName("userGesture") Boolean userGesture,
      @Optional @ParamName("awaitPromise") Boolean awaitPromise,
      @Experimental @Optional @ParamName("throwOnSideEffect") Boolean throwOnSideEffect,
      @Experimental @Optional @ParamName("timeout") Double timeout);

  /** Returns the isolate id. */
  @Experimental
  @Returns("id")
  String getIsolateId();

  /**
   * Returns the JavaScript heap usage. It is the total usage of the corresponding isolate not
   * scoped to a particular Runtime.
   */
  @Experimental
  HeapUsage getHeapUsage();

  /**
   * Returns properties of a given object. Object group of the result is inherited from the target
   * object.
   *
   * @param objectId Identifier of the object to return properties for.
   */
  Properties getProperties(@ParamName("objectId") String objectId);

  /**
   * Returns properties of a given object. Object group of the result is inherited from the target
   * object.
   *
   * @param objectId Identifier of the object to return properties for.
   * @param ownProperties If true, returns properties belonging only to the element itself, not to
   *     its prototype chain.
   * @param accessorPropertiesOnly If true, returns accessor properties (with getter/setter) only;
   *     internal properties are not returned either.
   * @param generatePreview Whether preview should be generated for the results.
   */
  Properties getProperties(
      @ParamName("objectId") String objectId,
      @Optional @ParamName("ownProperties") Boolean ownProperties,
      @Experimental @Optional @ParamName("accessorPropertiesOnly") Boolean accessorPropertiesOnly,
      @Experimental @Optional @ParamName("generatePreview") Boolean generatePreview);

  /** Returns all let, const and class variables from global scope. */
  @Returns("names")
  @ReturnTypeParameter(String.class)
  List<String> globalLexicalScopeNames();

  /**
   * Returns all let, const and class variables from global scope.
   *
   * @param executionContextId Specifies in which execution context to lookup global scope
   *     variables.
   */
  @Returns("names")
  @ReturnTypeParameter(String.class)
  List<String> globalLexicalScopeNames(
      @Optional @ParamName("executionContextId") Integer executionContextId);

  /** @param prototypeObjectId Identifier of the prototype to return objects for. */
  @Returns("objects")
  RemoteObject queryObjects(@ParamName("prototypeObjectId") String prototypeObjectId);

  /**
   * @param prototypeObjectId Identifier of the prototype to return objects for.
   * @param objectGroup Symbolic group name that can be used to release the results.
   */
  @Returns("objects")
  RemoteObject queryObjects(
      @ParamName("prototypeObjectId") String prototypeObjectId,
      @Optional @ParamName("objectGroup") String objectGroup);

  /**
   * Releases remote object with given id.
   *
   * @param objectId Identifier of the object to release.
   */
  void releaseObject(@ParamName("objectId") String objectId);

  /**
   * Releases all remote objects that belong to a given group.
   *
   * @param objectGroup Symbolic object group name.
   */
  void releaseObjectGroup(@ParamName("objectGroup") String objectGroup);

  /** Tells inspected instance to run if it was waiting for debugger to attach. */
  void runIfWaitingForDebugger();

  /**
   * Runs script with given id in a given context.
   *
   * @param scriptId Id of the script to run.
   */
  RunScript runScript(@ParamName("scriptId") String scriptId);

  /**
   * Runs script with given id in a given context.
   *
   * @param scriptId Id of the script to run.
   * @param executionContextId Specifies in which execution context to perform script run. If the
   *     parameter is omitted the evaluation will be performed in the context of the inspected page.
   * @param objectGroup Symbolic group name that can be used to release multiple objects.
   * @param silent In silent mode exceptions thrown during evaluation are not reported and do not
   *     pause execution. Overrides `setPauseOnException` state.
   * @param includeCommandLineAPI Determines whether Command Line API should be available during the
   *     evaluation.
   * @param returnByValue Whether the result is expected to be a JSON object which should be sent by
   *     value.
   * @param generatePreview Whether preview should be generated for the result.
   * @param awaitPromise Whether execution should `await` for resulting value and return once
   *     awaited promise is resolved.
   */
  RunScript runScript(
      @ParamName("scriptId") String scriptId,
      @Optional @ParamName("executionContextId") Integer executionContextId,
      @Optional @ParamName("objectGroup") String objectGroup,
      @Optional @ParamName("silent") Boolean silent,
      @Optional @ParamName("includeCommandLineAPI") Boolean includeCommandLineAPI,
      @Optional @ParamName("returnByValue") Boolean returnByValue,
      @Optional @ParamName("generatePreview") Boolean generatePreview,
      @Optional @ParamName("awaitPromise") Boolean awaitPromise);

  /** @param enabled */
  @Experimental
  void setCustomObjectFormatterEnabled(@ParamName("enabled") Boolean enabled);

  /** @param size */
  @Experimental
  void setMaxCallStackSizeToCapture(@ParamName("size") Integer size);

  /**
   * Terminate current or next JavaScript execution. Will cancel the termination when the outer-most
   * script execution ends.
   */
  @Experimental
  void terminateExecution();

  /**
   * If executionContextId is empty, adds binding with the given name on the global objects of all
   * inspected contexts, including those created later, bindings survive reloads. If
   * executionContextId is specified, adds binding only on global object of given execution context.
   * Binding function takes exactly one argument, this argument should be string, in case of any
   * other input, function throws an exception. Each binding function call produces
   * Runtime.bindingCalled notification.
   *
   * @param name
   */
  @Experimental
  void addBinding(@ParamName("name") String name);

  /**
   * If executionContextId is empty, adds binding with the given name on the global objects of all
   * inspected contexts, including those created later, bindings survive reloads. If
   * executionContextId is specified, adds binding only on global object of given execution context.
   * Binding function takes exactly one argument, this argument should be string, in case of any
   * other input, function throws an exception. Each binding function call produces
   * Runtime.bindingCalled notification.
   *
   * @param name
   * @param executionContextId
   */
  @Experimental
  void addBinding(
      @ParamName("name") String name,
      @Optional @ParamName("executionContextId") Integer executionContextId);

  /**
   * This method does not remove binding function from global object but unsubscribes current
   * runtime agent from Runtime.bindingCalled notifications.
   *
   * @param name
   */
  @Experimental
  void removeBinding(@ParamName("name") String name);

  /** Notification is issued every time when binding is called. */
  @EventName("bindingCalled")
  @Experimental
  EventListener onBindingCalled(EventHandler<BindingCalled> eventListener);

  /** Issued when console API was called. */
  @EventName("consoleAPICalled")
  EventListener onConsoleAPICalled(EventHandler<ConsoleAPICalled> eventListener);

  /** Issued when unhandled exception was revoked. */
  @EventName("exceptionRevoked")
  EventListener onExceptionRevoked(EventHandler<ExceptionRevoked> eventListener);

  /** Issued when exception was thrown and unhandled. */
  @EventName("exceptionThrown")
  EventListener onExceptionThrown(EventHandler<ExceptionThrown> eventListener);

  /** Issued when new execution context is created. */
  @EventName("executionContextCreated")
  EventListener onExecutionContextCreated(EventHandler<ExecutionContextCreated> eventListener);

  /** Issued when execution context is destroyed. */
  @EventName("executionContextDestroyed")
  EventListener onExecutionContextDestroyed(EventHandler<ExecutionContextDestroyed> eventListener);

  /** Issued when all executionContexts were cleared in browser */
  @EventName("executionContextsCleared")
  EventListener onExecutionContextsCleared(EventHandler<ExecutionContextsCleared> eventListener);

  /**
   * Issued when object should be inspected (for example, as a result of inspect() command line API
   * call).
   */
  @EventName("inspectRequested")
  EventListener onInspectRequested(EventHandler<InspectRequested> eventListener);
}
