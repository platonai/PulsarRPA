@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.page

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import ai.platon.cdt.kt.protocol.types.network.ResourceType
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean
import kotlin.Double
import kotlin.String

/**
 * Information about the Resource on the page.
 */
@Experimental
data class FrameResource(
  @param:JsonProperty("url")
  val url: String,
  @param:JsonProperty("type")
  val type: ResourceType,
  @param:JsonProperty("mimeType")
  val mimeType: String,
  @param:JsonProperty("lastModified")
  @param:Optional
  val lastModified: Double? = null,
  @param:JsonProperty("contentSize")
  @param:Optional
  val contentSize: Double? = null,
  @param:JsonProperty("failed")
  @param:Optional
  val failed: Boolean? = null,
  @param:JsonProperty("canceled")
  @param:Optional
  val canceled: Boolean? = null,
)
