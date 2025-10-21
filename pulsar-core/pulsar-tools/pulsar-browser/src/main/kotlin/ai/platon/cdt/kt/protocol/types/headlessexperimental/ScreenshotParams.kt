package ai.platon.cdt.kt.protocol.types.headlessexperimental

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int

/**
 * Encoding options for a screenshot.
 */
public data class ScreenshotParams(
  @JsonProperty("format")
  @Optional
  public val format: ScreenshotParamsFormat? = null,
  @JsonProperty("quality")
  @Optional
  public val quality: Int? = null,
)
