package ai.platon.cdt.kt.protocol.types.page

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int
import kotlin.String

/**
 * Error while paring app manifest.
 */
public data class AppManifestError(
  @JsonProperty("message")
  public val message: String,
  @JsonProperty("critical")
  public val critical: Int,
  @JsonProperty("line")
  public val line: Int,
  @JsonProperty("column")
  public val column: Int,
)
