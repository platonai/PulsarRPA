@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.page

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int

/**
 * Default font sizes.
 */
@Experimental
data class FontSizes(
  @param:JsonProperty("standard")
  @param:Optional
  val standard: Int? = null,
  @param:JsonProperty("fixed")
  @param:Optional
  val fixed: Int? = null,
)
