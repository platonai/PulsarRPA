package ai.platon.cdt.kt.protocol.types.domsnapshot

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

/**
 * A name/value pair.
 */
public data class NameValue(
  @JsonProperty("name")
  public val name: String,
  @JsonProperty("value")
  public val `value`: String,
)
