package ai.platon.cdt.kt.protocol.types.page

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double

/**
 * Screencast frame metadata.
 */
@Experimental
public data class ScreencastFrameMetadata(
  @JsonProperty("offsetTop")
  public val offsetTop: Double,
  @JsonProperty("pageScaleFactor")
  public val pageScaleFactor: Double,
  @JsonProperty("deviceWidth")
  public val deviceWidth: Double,
  @JsonProperty("deviceHeight")
  public val deviceHeight: Double,
  @JsonProperty("scrollOffsetX")
  public val scrollOffsetX: Double,
  @JsonProperty("scrollOffsetY")
  public val scrollOffsetY: Double,
  @JsonProperty("timestamp")
  @Optional
  public val timestamp: Double? = null,
)
