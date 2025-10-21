package ai.platon.cdt.kt.protocol.types.debugger

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import ai.platon.cdt.kt.protocol.types.runtime.RemoteObject
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String
import kotlin.collections.List

/**
 * JavaScript call frame. Array of call frames form the call stack.
 */
public data class CallFrame(
  @JsonProperty("callFrameId")
  public val callFrameId: String,
  @JsonProperty("functionName")
  public val functionName: String,
  @JsonProperty("functionLocation")
  @Optional
  public val functionLocation: Location? = null,
  @JsonProperty("location")
  public val location: Location,
  @JsonProperty("url")
  public val url: String,
  @JsonProperty("scopeChain")
  public val scopeChain: List<Scope>,
  @JsonProperty("this")
  public val `this`: RemoteObject,
  @JsonProperty("returnValue")
  @Optional
  public val returnValue: RemoteObject? = null,
)
