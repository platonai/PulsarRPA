@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.css

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String
import kotlin.collections.List

/**
 * CSS media rule descriptor.
 */
data class CSSMedia(
  @param:JsonProperty("text")
  val text: String,
  @param:JsonProperty("source")
  val source: CSSMediaSource,
  @param:JsonProperty("sourceURL")
  @param:Optional
  val sourceURL: String? = null,
  @param:JsonProperty("range")
  @param:Optional
  val range: SourceRange? = null,
  @param:JsonProperty("styleSheetId")
  @param:Optional
  val styleSheetId: String? = null,
  @param:JsonProperty("mediaList")
  @param:Optional
  val mediaList: List<MediaQuery>? = null,
)
