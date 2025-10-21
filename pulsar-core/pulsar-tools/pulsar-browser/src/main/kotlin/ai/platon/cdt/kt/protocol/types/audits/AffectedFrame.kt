package ai.platon.cdt.kt.protocol.types.audits

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

/**
 * Information about the frame affected by an inspector issue.
 */
public data class AffectedFrame(
  @JsonProperty("frameId")
  public val frameId: String,
)
