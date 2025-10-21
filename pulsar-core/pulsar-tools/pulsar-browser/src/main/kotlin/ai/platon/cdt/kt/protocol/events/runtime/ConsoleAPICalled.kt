package ai.platon.cdt.kt.protocol.events.runtime

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import ai.platon.cdt.kt.protocol.types.runtime.ConsoleAPICalledType
import ai.platon.cdt.kt.protocol.types.runtime.RemoteObject
import ai.platon.cdt.kt.protocol.types.runtime.StackTrace
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.Int
import kotlin.String
import kotlin.collections.List

/**
 * Issued when console API was called.
 */
public data class ConsoleAPICalled(
  @JsonProperty("type")
  public val type: ConsoleAPICalledType,
  @JsonProperty("args")
  public val args: List<RemoteObject>,
  @JsonProperty("executionContextId")
  public val executionContextId: Int,
  @JsonProperty("timestamp")
  public val timestamp: Double,
  @JsonProperty("stackTrace")
  @Optional
  public val stackTrace: StackTrace? = null,
  @JsonProperty("context")
  @Optional
  @Experimental
  public val context: String? = null,
)
