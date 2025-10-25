@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.headlessexperimental

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int

/**
 * Encoding options for a screenshot.
 */
data class ScreenshotParams(
  @param:JsonProperty("format")
  @param:Optional
  val format: ScreenshotParamsFormat? = null,
  @param:JsonProperty("quality")
  @param:Optional
  val quality: Int? = null,
)
