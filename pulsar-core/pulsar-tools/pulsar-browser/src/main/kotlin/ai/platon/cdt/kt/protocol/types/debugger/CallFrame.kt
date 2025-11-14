@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.debugger

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import ai.platon.cdt.kt.protocol.types.runtime.RemoteObject
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String
import kotlin.collections.List

/**
 * JavaScript call frame. Array of call frames form the call stack.
 */
data class CallFrame(
  @param:JsonProperty("callFrameId")
  val callFrameId: String,
  @param:JsonProperty("functionName")
  val functionName: String,
  @param:JsonProperty("functionLocation")
  @param:Optional
  val functionLocation: Location? = null,
  @param:JsonProperty("location")
  val location: Location,
  @param:JsonProperty("url")
  val url: String,
  @param:JsonProperty("scopeChain")
  val scopeChain: List<Scope>,
  @param:JsonProperty("this")
  val `this`: RemoteObject,
  @param:JsonProperty("returnValue")
  @param:Optional
  val returnValue: RemoteObject? = null,
)
