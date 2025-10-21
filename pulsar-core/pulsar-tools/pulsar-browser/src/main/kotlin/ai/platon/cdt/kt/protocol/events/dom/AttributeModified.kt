package ai.platon.cdt.kt.protocol.events.dom

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int
import kotlin.String

/**
 * Fired when `Element`'s attribute is modified.
 */
public data class AttributeModified(
  @JsonProperty("nodeId")
  public val nodeId: Int,
  @JsonProperty("name")
  public val name: String,
  @JsonProperty("value")
  public val `value`: String,
)
