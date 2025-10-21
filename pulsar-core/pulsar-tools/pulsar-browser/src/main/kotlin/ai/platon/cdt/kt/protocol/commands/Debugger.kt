package ai.platon.cdt.kt.protocol.commands

import ai.platon.cdt.kt.protocol.events.debugger.BreakpointResolved
import ai.platon.cdt.kt.protocol.events.debugger.Paused
import ai.platon.cdt.kt.protocol.events.debugger.Resumed
import ai.platon.cdt.kt.protocol.events.debugger.ScriptFailedToParse
import ai.platon.cdt.kt.protocol.events.debugger.ScriptParsed
import ai.platon.cdt.kt.protocol.support.annotations.EventName
import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import ai.platon.cdt.kt.protocol.support.annotations.ParamName
import ai.platon.cdt.kt.protocol.support.annotations.ReturnTypeParameter
import ai.platon.cdt.kt.protocol.support.annotations.Returns
import ai.platon.cdt.kt.protocol.support.types.EventHandler
import ai.platon.cdt.kt.protocol.support.types.EventListener
import ai.platon.cdt.kt.protocol.types.debugger.BreakLocation
import ai.platon.cdt.kt.protocol.types.debugger.ContinueToLocationTargetCallFrames
import ai.platon.cdt.kt.protocol.types.debugger.EvaluateOnCallFrame
import ai.platon.cdt.kt.protocol.types.debugger.Location
import ai.platon.cdt.kt.protocol.types.debugger.LocationRange
import ai.platon.cdt.kt.protocol.types.debugger.RestartFrame
import ai.platon.cdt.kt.protocol.types.debugger.ScriptPosition
import ai.platon.cdt.kt.protocol.types.debugger.ScriptSource
import ai.platon.cdt.kt.protocol.types.debugger.SearchMatch
import ai.platon.cdt.kt.protocol.types.debugger.SetBreakpoint
import ai.platon.cdt.kt.protocol.types.debugger.SetBreakpointByUrl
import ai.platon.cdt.kt.protocol.types.debugger.SetInstrumentationBreakpointInstrumentation
import ai.platon.cdt.kt.protocol.types.debugger.SetPauseOnExceptionsState
import ai.platon.cdt.kt.protocol.types.debugger.SetScriptSource
import ai.platon.cdt.kt.protocol.types.runtime.CallArgument
import ai.platon.cdt.kt.protocol.types.runtime.StackTrace
import ai.platon.cdt.kt.protocol.types.runtime.StackTraceId
import java.lang.Deprecated
import kotlin.Boolean
import kotlin.Double
import kotlin.Int
import kotlin.String
import kotlin.Unit
import kotlin.collections.List

/**
 * Debugger domain exposes JavaScript debugging capabilities. It allows setting and removing
 * breakpoints, stepping through execution, exploring stack traces, etc.
 */
public interface Debugger {
  /**
   * Continues execution until specific location is reached.
   * @param location Location to continue to.
   * @param targetCallFrames
   */
  public suspend fun continueToLocation(@ParamName("location") location: Location,
      @ParamName("targetCallFrames") @Optional
      targetCallFrames: ContinueToLocationTargetCallFrames?)

  public suspend fun continueToLocation(@ParamName("location") location: Location) {
    return continueToLocation(location, null)
  }

  /**
   * Disables debugger for given page.
   */
  public suspend fun disable()

  /**
   * Enables debugger for the given page. Clients should not assume that the debugging has been
   * enabled until the result for this command is received.
   * @param maxScriptsCacheSize The maximum size in bytes of collected scripts (not referenced by
   * other heap objects)
   * the debugger can hold. Puts no limit if paramter is omitted.
   */
  @Returns("debuggerId")
  public suspend fun enable(@ParamName("maxScriptsCacheSize") @Optional @Experimental
      maxScriptsCacheSize: Double?): String

  @Returns("debuggerId")
  public suspend fun enable(): String {
    return enable(null)
  }

  /**
   * Evaluates expression on a given call frame.
   * @param callFrameId Call frame identifier to evaluate on.
   * @param expression Expression to evaluate.
   * @param objectGroup String object group name to put result into (allows rapid releasing
   * resulting object handles
   * using `releaseObjectGroup`).
   * @param includeCommandLineAPI Specifies whether command line API should be available to the
   * evaluated expression, defaults
   * to false.
   * @param silent In silent mode exceptions thrown during evaluation are not reported and do not
   * pause
   * execution. Overrides `setPauseOnException` state.
   * @param returnByValue Whether the result is expected to be a JSON object that should be sent by
   * value.
   * @param generatePreview Whether preview should be generated for the result.
   * @param throwOnSideEffect Whether to throw an exception if side effect cannot be ruled out
   * during evaluation.
   * @param timeout Terminate execution after timing out (number of milliseconds).
   */
  public suspend fun evaluateOnCallFrame(
    @ParamName("callFrameId") callFrameId: String,
    @ParamName("expression") expression: String,
    @ParamName("objectGroup") @Optional objectGroup: String?,
    @ParamName("includeCommandLineAPI") @Optional includeCommandLineAPI: Boolean?,
    @ParamName("silent") @Optional silent: Boolean?,
    @ParamName("returnByValue") @Optional returnByValue: Boolean?,
    @ParamName("generatePreview") @Optional @Experimental generatePreview: Boolean?,
    @ParamName("throwOnSideEffect") @Optional throwOnSideEffect: Boolean?,
    @ParamName("timeout") @Optional @Experimental timeout: Double?,
  ): EvaluateOnCallFrame

  public suspend fun evaluateOnCallFrame(@ParamName("callFrameId") callFrameId: String,
      @ParamName("expression") expression: String): EvaluateOnCallFrame {
    return evaluateOnCallFrame(callFrameId, expression, null, null, null, null, null, null, null)
  }

  /**
   * Returns possible locations for breakpoint. scriptId in start and end range locations should be
   * the same.
   * @param start Start of range to search possible breakpoint locations in.
   * @param end End of range to search possible breakpoint locations in (excluding). When not
   * specified, end
   * of scripts is used as end of range.
   * @param restrictToFunction Only consider locations which are in the same (non-nested) function
   * as start.
   */
  @Returns("locations")
  @ReturnTypeParameter(BreakLocation::class)
  public suspend fun getPossibleBreakpoints(
    @ParamName("start") start: Location,
    @ParamName("end") @Optional end: Location?,
    @ParamName("restrictToFunction") @Optional restrictToFunction: Boolean?,
  ): List<BreakLocation>

  @Returns("locations")
  @ReturnTypeParameter(BreakLocation::class)
  public suspend fun getPossibleBreakpoints(@ParamName("start") start: Location):
      List<BreakLocation> {
    return getPossibleBreakpoints(start, null, null)
  }

  /**
   * Returns source for the script with given id.
   * @param scriptId Id of the script to get source for.
   */
  public suspend fun getScriptSource(@ParamName("scriptId") scriptId: String): ScriptSource

  /**
   * This command is deprecated. Use getScriptSource instead.
   * @param scriptId Id of the Wasm script to get source for.
   */
  @Deprecated
  @Returns("bytecode")
  public suspend fun getWasmBytecode(@ParamName("scriptId") scriptId: String): String

  /**
   * Returns stack trace with given `stackTraceId`.
   * @param stackTraceId
   */
  @Experimental
  @Returns("stackTrace")
  public suspend fun getStackTrace(@ParamName("stackTraceId") stackTraceId: StackTraceId):
      StackTrace

  /**
   * Stops on the next JavaScript statement.
   */
  public suspend fun pause()

  /**
   * @param parentStackTraceId Debugger will pause when async call with given stack trace is
   * started.
   */
  @Deprecated
  @Experimental
  public suspend fun pauseOnAsyncCall(@ParamName("parentStackTraceId")
      parentStackTraceId: StackTraceId)

  /**
   * Removes JavaScript breakpoint.
   * @param breakpointId
   */
  public suspend fun removeBreakpoint(@ParamName("breakpointId") breakpointId: String)

  /**
   * Restarts particular call frame from the beginning.
   * @param callFrameId Call frame identifier to evaluate on.
   */
  public suspend fun restartFrame(@ParamName("callFrameId") callFrameId: String): RestartFrame

  /**
   * Resumes JavaScript execution.
   * @param terminateOnResume Set to true to terminate execution upon resuming execution. In
   * contrast
   * to Runtime.terminateExecution, this will allows to execute further
   * JavaScript (i.e. via evaluation) until execution of the paused code
   * is actually resumed, at which point termination is triggered.
   * If execution is currently not paused, this parameter has no effect.
   */
  public suspend fun resume(@ParamName("terminateOnResume") @Optional terminateOnResume: Boolean?)

  public suspend fun resume() {
    return resume(null)
  }

  /**
   * Searches for given string in script content.
   * @param scriptId Id of the script to search in.
   * @param query String to search for.
   * @param caseSensitive If true, search is case sensitive.
   * @param isRegex If true, treats string parameter as regex.
   */
  @Returns("result")
  @ReturnTypeParameter(SearchMatch::class)
  public suspend fun searchInContent(
    @ParamName("scriptId") scriptId: String,
    @ParamName("query") query: String,
    @ParamName("caseSensitive") @Optional caseSensitive: Boolean?,
    @ParamName("isRegex") @Optional isRegex: Boolean?,
  ): List<SearchMatch>

  @Returns("result")
  @ReturnTypeParameter(SearchMatch::class)
  public suspend fun searchInContent(@ParamName("scriptId") scriptId: String, @ParamName("query")
      query: String): List<SearchMatch> {
    return searchInContent(scriptId, query, null, null)
  }

  /**
   * Enables or disables async call stacks tracking.
   * @param maxDepth Maximum depth of async call stacks. Setting to `0` will effectively disable
   * collecting async
   * call stacks (default).
   */
  public suspend fun setAsyncCallStackDepth(@ParamName("maxDepth") maxDepth: Int)

  /**
   * Replace previous blackbox patterns with passed ones. Forces backend to skip stepping/pausing in
   * scripts with url matching one of the patterns. VM will try to leave blackboxed script by
   * performing 'step in' several times, finally resorting to 'step out' if unsuccessful.
   * @param patterns Array of regexps that will be used to check script url for blackbox state.
   */
  @Experimental
  public suspend fun setBlackboxPatterns(@ParamName("patterns") patterns: List<String>)

  /**
   * Makes backend skip steps in the script in blackboxed ranges. VM will try leave blacklisted
   * scripts by performing 'step in' several times, finally resorting to 'step out' if unsuccessful.
   * Positions array contains positions where blackbox state is changed. First interval isn't
   * blackboxed. Array should be sorted.
   * @param scriptId Id of the script.
   * @param positions
   */
  @Experimental
  public suspend fun setBlackboxedRanges(@ParamName("scriptId") scriptId: String,
      @ParamName("positions") positions: List<ScriptPosition>)

  /**
   * Sets JavaScript breakpoint at a given location.
   * @param location Location to set breakpoint in.
   * @param condition Expression to use as a breakpoint condition. When specified, debugger will
   * only stop on the
   * breakpoint if this expression evaluates to true.
   */
  public suspend fun setBreakpoint(@ParamName("location") location: Location,
      @ParamName("condition") @Optional condition: String?): SetBreakpoint

  public suspend fun setBreakpoint(@ParamName("location") location: Location): SetBreakpoint {
    return setBreakpoint(location, null)
  }

  /**
   * Sets instrumentation breakpoint.
   * @param instrumentation Instrumentation name.
   */
  @Returns("breakpointId")
  public suspend fun setInstrumentationBreakpoint(@ParamName("instrumentation")
      instrumentation: SetInstrumentationBreakpointInstrumentation): String

  /**
   * Sets JavaScript breakpoint at given location specified either by URL or URL regex. Once this
   * command is issued, all existing parsed scripts will have breakpoints resolved and returned in
   * `locations` property. Further matching script parsing will result in subsequent
   * `breakpointResolved` events issued. This logical breakpoint will survive page reloads.
   * @param lineNumber Line number to set breakpoint at.
   * @param url URL of the resources to set breakpoint on.
   * @param urlRegex Regex pattern for the URLs of the resources to set breakpoints on. Either `url`
   * or
   * `urlRegex` must be specified.
   * @param scriptHash Script hash of the resources to set breakpoint on.
   * @param columnNumber Offset in the line to set breakpoint at.
   * @param condition Expression to use as a breakpoint condition. When specified, debugger will
   * only stop on the
   * breakpoint if this expression evaluates to true.
   */
  public suspend fun setBreakpointByUrl(
    @ParamName("lineNumber") lineNumber: Int,
    @ParamName("url") @Optional url: String?,
    @ParamName("urlRegex") @Optional urlRegex: String?,
    @ParamName("scriptHash") @Optional scriptHash: String?,
    @ParamName("columnNumber") @Optional columnNumber: Int?,
    @ParamName("condition") @Optional condition: String?,
  ): SetBreakpointByUrl

  public suspend fun setBreakpointByUrl(@ParamName("lineNumber") lineNumber: Int):
      SetBreakpointByUrl {
    return setBreakpointByUrl(lineNumber, null, null, null, null, null)
  }

  /**
   * Sets JavaScript breakpoint before each call to the given function.
   * If another function was created from the same source as a given one,
   * calling it will also trigger the breakpoint.
   * @param objectId Function object id.
   * @param condition Expression to use as a breakpoint condition. When specified, debugger will
   * stop on the breakpoint if this expression evaluates to true.
   */
  @Experimental
  @Returns("breakpointId")
  public suspend fun setBreakpointOnFunctionCall(@ParamName("objectId") objectId: String,
      @ParamName("condition") @Optional condition: String?): String

  @Experimental
  @Returns("breakpointId")
  public suspend fun setBreakpointOnFunctionCall(@ParamName("objectId") objectId: String): String {
    return setBreakpointOnFunctionCall(objectId, null)
  }

  /**
   * Activates / deactivates all breakpoints on the page.
   * @param active New value for breakpoints active state.
   */
  public suspend fun setBreakpointsActive(@ParamName("active") active: Boolean)

  /**
   * Defines pause on exceptions state. Can be set to stop on all exceptions, uncaught exceptions or
   * no exceptions. Initial pause on exceptions state is `none`.
   * @param state Pause on exceptions mode.
   */
  public suspend fun setPauseOnExceptions(@ParamName("state") state: SetPauseOnExceptionsState)

  /**
   * Changes return value in top frame. Available only at return break position.
   * @param newValue New return value.
   */
  @Experimental
  public suspend fun setReturnValue(@ParamName("newValue") newValue: CallArgument)

  /**
   * Edits JavaScript source live.
   * @param scriptId Id of the script to edit.
   * @param scriptSource New content of the script.
   * @param dryRun If true the change will not actually be applied. Dry run may be used to get
   * result
   * description without actually modifying the code.
   */
  public suspend fun setScriptSource(
    @ParamName("scriptId") scriptId: String,
    @ParamName("scriptSource") scriptSource: String,
    @ParamName("dryRun") @Optional dryRun: Boolean?,
  ): SetScriptSource

  public suspend fun setScriptSource(@ParamName("scriptId") scriptId: String,
      @ParamName("scriptSource") scriptSource: String): SetScriptSource {
    return setScriptSource(scriptId, scriptSource, null)
  }

  /**
   * Makes page not interrupt on any pauses (breakpoint, exception, dom exception etc).
   * @param skip New value for skip pauses state.
   */
  public suspend fun setSkipAllPauses(@ParamName("skip") skip: Boolean)

  /**
   * Changes value of variable in a callframe. Object-based scopes are not supported and must be
   * mutated manually.
   * @param scopeNumber 0-based number of scope as was listed in scope chain. Only 'local',
   * 'closure' and 'catch'
   * scope types are allowed. Other scopes could be manipulated manually.
   * @param variableName Variable name.
   * @param newValue New variable value.
   * @param callFrameId Id of callframe that holds variable.
   */
  public suspend fun setVariableValue(
    @ParamName("scopeNumber") scopeNumber: Int,
    @ParamName("variableName") variableName: String,
    @ParamName("newValue") newValue: CallArgument,
    @ParamName("callFrameId") callFrameId: String,
  )

  /**
   * Steps into the function call.
   * @param breakOnAsyncCall Debugger will pause on the execution of the first async task which was
   * scheduled
   * before next pause.
   * @param skipList The skipList specifies location ranges that should be skipped on step into.
   */
  public suspend fun stepInto(@ParamName("breakOnAsyncCall") @Optional @Experimental
      breakOnAsyncCall: Boolean?, @ParamName("skipList") @Optional @Experimental
      skipList: List<LocationRange>?)

  public suspend fun stepInto() {
    return stepInto(null, null)
  }

  /**
   * Steps out of the function call.
   */
  public suspend fun stepOut()

  /**
   * Steps over the statement.
   * @param skipList The skipList specifies location ranges that should be skipped on step over.
   */
  public suspend fun stepOver(@ParamName("skipList") @Optional @Experimental
      skipList: List<LocationRange>?)

  public suspend fun stepOver() {
    return stepOver(null)
  }

  @EventName("breakpointResolved")
  public fun onBreakpointResolved(eventListener: EventHandler<BreakpointResolved>): EventListener

  @EventName("breakpointResolved")
  public fun onBreakpointResolved(eventListener: suspend (BreakpointResolved) -> Unit):
      EventListener

  @EventName("paused")
  public fun onPaused(eventListener: EventHandler<Paused>): EventListener

  @EventName("paused")
  public fun onPaused(eventListener: suspend (Paused) -> Unit): EventListener

  @EventName("resumed")
  public fun onResumed(eventListener: EventHandler<Resumed>): EventListener

  @EventName("resumed")
  public fun onResumed(eventListener: suspend (Resumed) -> Unit): EventListener

  @EventName("scriptFailedToParse")
  public fun onScriptFailedToParse(eventListener: EventHandler<ScriptFailedToParse>): EventListener

  @EventName("scriptFailedToParse")
  public fun onScriptFailedToParse(eventListener: suspend (ScriptFailedToParse) -> Unit):
      EventListener

  @EventName("scriptParsed")
  public fun onScriptParsed(eventListener: EventHandler<ScriptParsed>): EventListener

  @EventName("scriptParsed")
  public fun onScriptParsed(eventListener: suspend (ScriptParsed) -> Unit): EventListener
}
