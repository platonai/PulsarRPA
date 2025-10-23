@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.applicationcache

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int
import kotlin.String

/**
 * Frame identifier - manifest URL pair.
 */
data class FrameWithManifest(
  @param:JsonProperty("frameId")
  val frameId: String,
  @param:JsonProperty("manifestURL")
  val manifestURL: String,
  @param:JsonProperty("status")
  val status: Int,
)
