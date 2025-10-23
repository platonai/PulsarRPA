@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.runtime

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean
import kotlin.String
import kotlin.collections.List

/**
 * Object containing abbreviated remote object value.
 */
@Experimental
data class ObjectPreview(
  @param:JsonProperty("type")
  val type: ObjectPreviewType,
  @param:JsonProperty("subtype")
  @param:Optional
  val subtype: ObjectPreviewSubtype? = null,
  @param:JsonProperty("description")
  @param:Optional
  val description: String? = null,
  @param:JsonProperty("overflow")
  val overflow: Boolean,
  @param:JsonProperty("properties")
  val properties: List<PropertyPreview>,
  @param:JsonProperty("entries")
  @param:Optional
  val entries: List<EntryPreview>? = null,
)
