package ai.platon.cdt.kt.protocol.types.domdebugger

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import ai.platon.cdt.kt.protocol.types.runtime.RemoteObject
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean
import kotlin.Int
import kotlin.String

/**
 * Object event listener.
 */
public data class EventListener(
  @JsonProperty("type")
  public val type: String,
  @JsonProperty("useCapture")
  public val useCapture: Boolean,
  @JsonProperty("passive")
  public val passive: Boolean,
  @JsonProperty("once")
  public val once: Boolean,
  @JsonProperty("scriptId")
  public val scriptId: String,
  @JsonProperty("lineNumber")
  public val lineNumber: Int,
  @JsonProperty("columnNumber")
  public val columnNumber: Int,
  @JsonProperty("handler")
  @Optional
  public val handler: RemoteObject? = null,
  @JsonProperty("originalHandler")
  @Optional
  public val originalHandler: RemoteObject? = null,
  @JsonProperty("backendNodeId")
  @Optional
  public val backendNodeId: Int? = null,
)
