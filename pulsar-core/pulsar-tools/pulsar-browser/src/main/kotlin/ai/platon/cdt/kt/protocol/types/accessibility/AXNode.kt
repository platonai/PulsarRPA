package ai.platon.cdt.kt.protocol.types.accessibility

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean
import kotlin.Int
import kotlin.String
import kotlin.collections.List

/**
 * A node in the accessibility tree.
 */
public data class AXNode(
  @JsonProperty("nodeId")
  public val nodeId: String,
  @JsonProperty("ignored")
  public val ignored: Boolean,
  @JsonProperty("ignoredReasons")
  @Optional
  public val ignoredReasons: List<AXProperty>? = null,
  @JsonProperty("role")
  @Optional
  public val role: AXValue? = null,
  @JsonProperty("name")
  @Optional
  public val name: AXValue? = null,
  @JsonProperty("description")
  @Optional
  public val description: AXValue? = null,
  @JsonProperty("value")
  @Optional
  public val `value`: AXValue? = null,
  @JsonProperty("properties")
  @Optional
  public val properties: List<AXProperty>? = null,
  @JsonProperty("childIds")
  @Optional
  public val childIds: List<String>? = null,
  @JsonProperty("backendDOMNodeId")
  @Optional
  public val backendDOMNodeId: Int? = null,
)
