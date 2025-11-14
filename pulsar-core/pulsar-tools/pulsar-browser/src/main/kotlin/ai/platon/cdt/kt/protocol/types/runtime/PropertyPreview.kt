@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.runtime

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

@Experimental
data class PropertyPreview(
  @param:JsonProperty("name")
  val name: String,
  @param:JsonProperty("type")
  val type: PropertyPreviewType,
  @param:JsonProperty("value")
  @param:Optional
  val `value`: String? = null,
  @param:JsonProperty("valuePreview")
  @param:Optional
  val valuePreview: ObjectPreview? = null,
  @param:JsonProperty("subtype")
  @param:Optional
  val subtype: PropertyPreviewSubtype? = null,
)
