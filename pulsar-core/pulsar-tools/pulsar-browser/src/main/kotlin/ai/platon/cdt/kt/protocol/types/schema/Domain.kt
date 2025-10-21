package ai.platon.cdt.kt.protocol.types.schema

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

/**
 * Description of the protocol domain.
 */
public data class Domain(
  @JsonProperty("name")
  public val name: String,
  @JsonProperty("version")
  public val version: String,
)
