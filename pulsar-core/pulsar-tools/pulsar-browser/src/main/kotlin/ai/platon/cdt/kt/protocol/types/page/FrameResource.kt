package ai.platon.cdt.kt.protocol.types.page

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import ai.platon.cdt.kt.protocol.types.network.ResourceType
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean
import kotlin.Double
import kotlin.String

/**
 * Information about the Resource on the page.
 */
@Experimental
public data class FrameResource(
  @JsonProperty("url")
  public val url: String,
  @JsonProperty("type")
  public val type: ResourceType,
  @JsonProperty("mimeType")
  public val mimeType: String,
  @JsonProperty("lastModified")
  @Optional
  public val lastModified: Double? = null,
  @JsonProperty("contentSize")
  @Optional
  public val contentSize: Double? = null,
  @JsonProperty("failed")
  @Optional
  public val failed: Boolean? = null,
  @JsonProperty("canceled")
  @Optional
  public val canceled: Boolean? = null,
)
