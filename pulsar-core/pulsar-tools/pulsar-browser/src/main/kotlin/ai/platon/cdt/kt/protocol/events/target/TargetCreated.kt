package ai.platon.cdt.kt.protocol.events.target

import ai.platon.cdt.kt.protocol.types.target.TargetInfo
import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Issued when a possible inspection target is created.
 */
public data class TargetCreated(
  @JsonProperty("targetInfo")
  public val targetInfo: TargetInfo,
)
