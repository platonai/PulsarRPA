@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.css

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.String

/**
 * Media query expression descriptor.
 */
data class MediaQueryExpression(
  @param:JsonProperty("value")
  val `value`: Double,
  @param:JsonProperty("unit")
  val unit: String,
  @param:JsonProperty("feature")
  val feature: String,
  @param:JsonProperty("valueRange")
  @param:Optional
  val valueRange: SourceRange? = null,
  @param:JsonProperty("computedLength")
  @param:Optional
  val computedLength: Double? = null,
)
