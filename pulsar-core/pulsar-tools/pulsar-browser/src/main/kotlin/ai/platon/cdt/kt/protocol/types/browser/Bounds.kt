@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.browser

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int

/**
 * Browser window bounds information
 */
@Experimental
data class Bounds(
  @param:JsonProperty("left")
  @param:Optional
  val left: Int? = null,
  @param:JsonProperty("top")
  @param:Optional
  val top: Int? = null,
  @param:JsonProperty("width")
  @param:Optional
  val width: Int? = null,
  @param:JsonProperty("height")
  @param:Optional
  val height: Int? = null,
  @param:JsonProperty("windowState")
  @param:Optional
  val windowState: WindowState? = null,
)
