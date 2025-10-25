@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.events.storage

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

/**
 * The origin's IndexedDB object store has been modified.
 */
data class IndexedDBContentUpdated(
  @param:JsonProperty("origin")
  val origin: String,
  @param:JsonProperty("databaseName")
  val databaseName: String,
  @param:JsonProperty("objectStoreName")
  val objectStoreName: String,
)
