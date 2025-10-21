package ai.platon.cdt.kt.protocol.types.storage

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double

/**
 * Usage for a storage type.
 */
public data class UsageForType(
  @JsonProperty("storageType")
  public val storageType: StorageType,
  @JsonProperty("usage")
  public val usage: Double,
)
