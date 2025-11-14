@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.accessibility

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean
import kotlin.String

/**
 * A single source for a computed AX property.
 */
data class AXValueSource(
  @param:JsonProperty("type")
  val type: AXValueSourceType,
  @param:JsonProperty("value")
  @param:Optional
  val `value`: AXValue? = null,
  @param:JsonProperty("attribute")
  @param:Optional
  val attribute: String? = null,
  @param:JsonProperty("attributeValue")
  @param:Optional
  val attributeValue: AXValue? = null,
  @param:JsonProperty("superseded")
  @param:Optional
  val superseded: Boolean? = null,
  @param:JsonProperty("nativeSource")
  @param:Optional
  val nativeSource: AXValueNativeSourceType? = null,
  @param:JsonProperty("nativeSourceValue")
  @param:Optional
  val nativeSourceValue: AXValue? = null,
  @param:JsonProperty("invalid")
  @param:Optional
  val invalid: Boolean? = null,
  @param:JsonProperty("invalidReason")
  @param:Optional
  val invalidReason: String? = null,
)
