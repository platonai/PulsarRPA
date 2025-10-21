package ai.platon.cdt.kt.protocol.commands

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import ai.platon.cdt.kt.protocol.support.annotations.ParamName
import ai.platon.cdt.kt.protocol.support.annotations.ReturnTypeParameter
import ai.platon.cdt.kt.protocol.support.annotations.Returns
import ai.platon.cdt.kt.protocol.types.domdebugger.CSPViolationType
import ai.platon.cdt.kt.protocol.types.domdebugger.DOMBreakpointType
import ai.platon.cdt.kt.protocol.types.domdebugger.EventListener
import kotlin.Boolean
import kotlin.Int
import kotlin.String
import kotlin.collections.List

/**
 * DOM debugging allows setting breakpoints on particular DOM operations and events. JavaScript
 * execution will stop on these operations as if there was a regular breakpoint set.
 */
public interface DOMDebugger {
  /**
   * Returns event listeners of the given object.
   * @param objectId Identifier of the object to return listeners for.
   * @param depth The maximum depth at which Node children should be retrieved, defaults to 1.
   * Use -1 for the
   * entire subtree or provide an integer larger than 0.
   * @param pierce Whether or not iframes and shadow roots should be traversed when returning the
   * subtree
   * (default is false). Reports listeners for all contexts if pierce is enabled.
   */
  @Returns("listeners")
  @ReturnTypeParameter(EventListener::class)
  public suspend fun getEventListeners(
    @ParamName("objectId") objectId: String,
    @ParamName("depth") @Optional depth: Int?,
    @ParamName("pierce") @Optional pierce: Boolean?,
  ): List<EventListener>

  @Returns("listeners")
  @ReturnTypeParameter(EventListener::class)
  public suspend fun getEventListeners(@ParamName("objectId") objectId: String):
      List<EventListener> {
    return getEventListeners(objectId, null, null)
  }

  /**
   * Removes DOM breakpoint that was set using `setDOMBreakpoint`.
   * @param nodeId Identifier of the node to remove breakpoint from.
   * @param type Type of the breakpoint to remove.
   */
  public suspend fun removeDOMBreakpoint(@ParamName("nodeId") nodeId: Int, @ParamName("type")
      type: DOMBreakpointType)

  /**
   * Removes breakpoint on particular DOM event.
   * @param eventName Event name.
   * @param targetName EventTarget interface name.
   */
  public suspend fun removeEventListenerBreakpoint(@ParamName("eventName") eventName: String,
      @ParamName("targetName") @Optional @Experimental targetName: String?)

  public suspend fun removeEventListenerBreakpoint(@ParamName("eventName") eventName: String) {
    return removeEventListenerBreakpoint(eventName, null)
  }

  /**
   * Removes breakpoint on particular native event.
   * @param eventName Instrumentation name to stop on.
   */
  @Experimental
  public suspend fun removeInstrumentationBreakpoint(@ParamName("eventName") eventName: String)

  /**
   * Removes breakpoint from XMLHttpRequest.
   * @param url Resource URL substring.
   */
  public suspend fun removeXHRBreakpoint(@ParamName("url") url: String)

  /**
   * Sets breakpoint on particular CSP violations.
   * @param violationTypes CSP Violations to stop upon.
   */
  @Experimental
  public suspend fun setBreakOnCSPViolation(@ParamName("violationTypes")
      violationTypes: List<CSPViolationType>)

  /**
   * Sets breakpoint on particular operation with DOM.
   * @param nodeId Identifier of the node to set breakpoint on.
   * @param type Type of the operation to stop upon.
   */
  public suspend fun setDOMBreakpoint(@ParamName("nodeId") nodeId: Int, @ParamName("type")
      type: DOMBreakpointType)

  /**
   * Sets breakpoint on particular DOM event.
   * @param eventName DOM Event name to stop on (any DOM event will do).
   * @param targetName EventTarget interface name to stop on. If equal to `"*"` or not provided,
   * will stop on any
   * EventTarget.
   */
  public suspend fun setEventListenerBreakpoint(@ParamName("eventName") eventName: String,
      @ParamName("targetName") @Optional @Experimental targetName: String?)

  public suspend fun setEventListenerBreakpoint(@ParamName("eventName") eventName: String) {
    return setEventListenerBreakpoint(eventName, null)
  }

  /**
   * Sets breakpoint on particular native event.
   * @param eventName Instrumentation name to stop on.
   */
  @Experimental
  public suspend fun setInstrumentationBreakpoint(@ParamName("eventName") eventName: String)

  /**
   * Sets breakpoint on XMLHttpRequest.
   * @param url Resource URL substring. All XHRs having this substring in the URL will get stopped
   * upon.
   */
  public suspend fun setXHRBreakpoint(@ParamName("url") url: String)
}
