@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.events.storage

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

/**
 * The origin's IndexedDB database list has been modified.
 */
data class IndexedDBListUpdated(
  @param:JsonProperty("origin")
  val origin: String,
)
