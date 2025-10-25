@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.events.storage

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

/**
 * A cache has been added/deleted.
 */
data class CacheStorageListUpdated(
  @param:JsonProperty("origin")
  val origin: String,
)
