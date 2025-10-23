@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.input

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

@Experimental
data class DragDataItem(
  @param:JsonProperty("mimeType")
  val mimeType: String,
  @param:JsonProperty("data")
  val `data`: String,
  @param:JsonProperty("title")
  @param:Optional
  val title: String? = null,
  @param:JsonProperty("baseURL")
  @param:Optional
  val baseURL: String? = null,
)
