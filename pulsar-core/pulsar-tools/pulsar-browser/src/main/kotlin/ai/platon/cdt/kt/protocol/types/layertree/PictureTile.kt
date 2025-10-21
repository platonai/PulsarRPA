package ai.platon.cdt.kt.protocol.types.layertree

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.String

/**
 * Serialized fragment of layer picture along with its offset within the layer.
 */
public data class PictureTile(
  @JsonProperty("x")
  public val x: Double,
  @JsonProperty("y")
  public val y: Double,
  @JsonProperty("picture")
  public val picture: String,
)
