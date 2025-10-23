@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.network

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.String

/**
 * Information about the cached resource.
 */
data class CachedResource(
  @param:JsonProperty("url")
  val url: String,
  @param:JsonProperty("type")
  val type: ResourceType,
  @param:JsonProperty("response")
  @param:Optional
  val response: Response? = null,
  @param:JsonProperty("bodySize")
  val bodySize: Double,
)
