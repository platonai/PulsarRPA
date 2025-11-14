@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.layertree

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Deprecated
import kotlin.String
import kotlin.collections.List

data class CompositingReasons(
  @param:JsonProperty("compositingReasons")
  @Deprecated("Deprecated by protocol")
  val compositingReasons: List<String>,
  @param:JsonProperty("compositingReasonIds")
  val compositingReasonIds: List<String>,
)
