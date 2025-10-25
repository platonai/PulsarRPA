@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.storage

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double

/**
 * Usage for a storage type.
 */
data class UsageForType(
  @param:JsonProperty("storageType")
  val storageType: StorageType,
  @param:JsonProperty("usage")
  val usage: Double,
)
