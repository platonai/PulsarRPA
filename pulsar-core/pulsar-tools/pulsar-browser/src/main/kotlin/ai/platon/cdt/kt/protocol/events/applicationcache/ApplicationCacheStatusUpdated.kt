@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.events.applicationcache

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int
import kotlin.String

data class ApplicationCacheStatusUpdated(
  @param:JsonProperty("frameId")
  val frameId: String,
  @param:JsonProperty("manifestURL")
  val manifestURL: String,
  @param:JsonProperty("status")
  val status: Int,
)
