package ai.platon.cdt.kt.protocol.types.network

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import ai.platon.cdt.kt.protocol.types.runtime.StackTrace
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.String

/**
 * Information about the request initiator.
 */
public data class Initiator(
  @JsonProperty("type")
  public val type: InitiatorType,
  @JsonProperty("stack")
  @Optional
  public val stack: StackTrace? = null,
  @JsonProperty("url")
  @Optional
  public val url: String? = null,
  @JsonProperty("lineNumber")
  @Optional
  public val lineNumber: Double? = null,
  @JsonProperty("columnNumber")
  @Optional
  public val columnNumber: Double? = null,
  @JsonProperty("requestId")
  @Optional
  public val requestId: String? = null,
)
