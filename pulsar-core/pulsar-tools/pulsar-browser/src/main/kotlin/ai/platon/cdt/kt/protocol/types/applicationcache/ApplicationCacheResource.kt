@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.applicationcache

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int
import kotlin.String

/**
 * Detailed application cache resource information.
 */
data class ApplicationCacheResource(
  @param:JsonProperty("url")
  val url: String,
  @param:JsonProperty("size")
  val size: Int,
  @param:JsonProperty("type")
  val type: String,
)
