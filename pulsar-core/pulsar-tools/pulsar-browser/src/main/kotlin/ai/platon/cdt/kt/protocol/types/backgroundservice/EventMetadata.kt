@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.backgroundservice

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

/**
 * A key-value pair for additional event information to pass along.
 */
data class EventMetadata(
  @param:JsonProperty("key")
  val key: String,
  @param:JsonProperty("value")
  val `value`: String,
)
