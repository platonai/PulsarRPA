package ai.platon.cdt.kt.protocol.events.target

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int
import kotlin.String

/**
 * Issued when a target has crashed.
 */
public data class TargetCrashed(
  @JsonProperty("targetId")
  public val targetId: String,
  @JsonProperty("status")
  public val status: String,
  @JsonProperty("errorCode")
  public val errorCode: Int,
)
