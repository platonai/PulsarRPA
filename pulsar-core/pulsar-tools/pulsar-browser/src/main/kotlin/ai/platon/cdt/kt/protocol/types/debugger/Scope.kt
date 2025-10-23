@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.debugger

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import ai.platon.cdt.kt.protocol.types.runtime.RemoteObject
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

/**
 * Scope description.
 */
data class Scope(
  @param:JsonProperty("type")
  val type: ScopeType,
  @param:JsonProperty("object")
  val `object`: RemoteObject,
  @param:JsonProperty("name")
  @param:Optional
  val name: String? = null,
  @param:JsonProperty("startLocation")
  @param:Optional
  val startLocation: Location? = null,
  @param:JsonProperty("endLocation")
  @param:Optional
  val endLocation: Location? = null,
)
