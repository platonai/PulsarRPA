@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.runtime

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

/**
 * Object internal property descriptor. This property isn't normally visible in JavaScript code.
 */
data class InternalPropertyDescriptor(
  @param:JsonProperty("name")
  val name: String,
  @param:JsonProperty("value")
  @param:Optional
  val `value`: RemoteObject? = null,
)
