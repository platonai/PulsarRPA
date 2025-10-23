@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.indexeddb

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double

data class Metadata(
  @param:JsonProperty("entriesCount")
  val entriesCount: Double,
  @param:JsonProperty("keyGeneratorValue")
  val keyGeneratorValue: Double,
)
