@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.events.storage

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

/**
 * A cache's contents have been modified.
 */
data class CacheStorageContentUpdated(
  @param:JsonProperty("origin")
  val origin: String,
  @param:JsonProperty("cacheName")
  val cacheName: String,
)
