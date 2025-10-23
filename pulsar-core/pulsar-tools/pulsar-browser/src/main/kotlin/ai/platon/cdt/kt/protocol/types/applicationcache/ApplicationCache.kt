@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.applicationcache

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.String
import kotlin.collections.List

/**
 * Detailed application cache information.
 */
data class ApplicationCache(
  @param:JsonProperty("manifestURL")
  val manifestURL: String,
  @param:JsonProperty("size")
  val size: Double,
  @param:JsonProperty("creationTime")
  val creationTime: Double,
  @param:JsonProperty("updateTime")
  val updateTime: Double,
  @param:JsonProperty("resources")
  val resources: List<ApplicationCacheResource>,
)
