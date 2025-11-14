@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.page

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

/**
 * Generic font families collection.
 */
@Experimental
data class FontFamilies(
  @param:JsonProperty("standard")
  @param:Optional
  val standard: String? = null,
  @param:JsonProperty("fixed")
  @param:Optional
  val fixed: String? = null,
  @param:JsonProperty("serif")
  @param:Optional
  val serif: String? = null,
  @param:JsonProperty("sansSerif")
  @param:Optional
  val sansSerif: String? = null,
  @param:JsonProperty("cursive")
  @param:Optional
  val cursive: String? = null,
  @param:JsonProperty("fantasy")
  @param:Optional
  val fantasy: String? = null,
  @param:JsonProperty("pictograph")
  @param:Optional
  val pictograph: String? = null,
)
