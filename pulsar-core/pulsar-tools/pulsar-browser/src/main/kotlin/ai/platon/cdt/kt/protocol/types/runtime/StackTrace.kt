package ai.platon.cdt.kt.protocol.types.runtime

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String
import kotlin.collections.List

/**
 * Call frames for assertions or error messages.
 */
public data class StackTrace(
  @JsonProperty("description")
  @Optional
  public val description: String? = null,
  @JsonProperty("callFrames")
  public val callFrames: List<CallFrame>,
  @JsonProperty("parent")
  @Optional
  public val parent: StackTrace? = null,
  @JsonProperty("parentId")
  @Optional
  @Experimental
  public val parentId: StackTraceId? = null,
)
