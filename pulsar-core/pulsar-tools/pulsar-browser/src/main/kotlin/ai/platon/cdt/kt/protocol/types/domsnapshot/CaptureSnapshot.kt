@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.domsnapshot

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String
import kotlin.collections.List

data class CaptureSnapshot(
  @param:JsonProperty("documents")
  val documents: List<DocumentSnapshot>,
  @param:JsonProperty("strings")
  val strings: List<String>,
)
