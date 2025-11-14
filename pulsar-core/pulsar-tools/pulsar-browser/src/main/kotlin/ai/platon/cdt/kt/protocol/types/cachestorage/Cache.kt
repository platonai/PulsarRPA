@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.cachestorage

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

/**
 * Cache identifier.
 */
data class Cache(
  @param:JsonProperty("cacheId")
  val cacheId: String,
  @param:JsonProperty("securityOrigin")
  val securityOrigin: String,
  @param:JsonProperty("cacheName")
  val cacheName: String,
)
