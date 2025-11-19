@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.domsnapshot

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.collections.List

/**
 * A subset of the full ComputedStyle as defined by the request whitelist.
 */
data class ComputedStyle(
  @param:JsonProperty("properties")
  val properties: List<NameValue>,
)
