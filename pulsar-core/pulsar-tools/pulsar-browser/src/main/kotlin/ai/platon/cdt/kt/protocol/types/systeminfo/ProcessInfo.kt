package ai.platon.cdt.kt.protocol.types.systeminfo

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.Int
import kotlin.String

/**
 * Represents process info.
 */
public data class ProcessInfo(
  @JsonProperty("type")
  public val type: String,
  @JsonProperty("id")
  public val id: Int,
  @JsonProperty("cpuTime")
  public val cpuTime: Double,
)
