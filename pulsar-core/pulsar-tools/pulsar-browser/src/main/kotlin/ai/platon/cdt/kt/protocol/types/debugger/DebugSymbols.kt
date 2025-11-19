@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.debugger

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

/**
 * Debug symbols available for a wasm script.
 */
data class DebugSymbols(
  @param:JsonProperty("type")
  val type: DebugSymbolsType,
  @param:JsonProperty("externalURL")
  @param:Optional
  val externalURL: String? = null,
)
