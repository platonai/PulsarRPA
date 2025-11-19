@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.cachestorage

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

/**
 * Cached response
 */
data class CachedResponse(
  @param:JsonProperty("body")
  val body: String,
)
