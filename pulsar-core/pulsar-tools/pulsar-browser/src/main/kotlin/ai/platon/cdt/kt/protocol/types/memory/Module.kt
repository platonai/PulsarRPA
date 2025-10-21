package ai.platon.cdt.kt.protocol.types.memory

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.String

/**
 * Executable module information
 */
public data class Module(
  @JsonProperty("name")
  public val name: String,
  @JsonProperty("uuid")
  public val uuid: String,
  @JsonProperty("baseAddress")
  public val baseAddress: String,
  @JsonProperty("size")
  public val size: Double,
)
