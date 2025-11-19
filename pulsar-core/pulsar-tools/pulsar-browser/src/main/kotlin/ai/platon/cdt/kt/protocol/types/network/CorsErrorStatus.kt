@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.network

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

data class CorsErrorStatus(
  @param:JsonProperty("corsError")
  val corsError: CorsError,
  @param:JsonProperty("failedParameter")
  val failedParameter: String,
)
