@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.network

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean
import kotlin.String

data class ResponseBody(
  @param:JsonProperty("body")
  val body: String,
  @param:JsonProperty("base64Encoded")
  val base64Encoded: Boolean,
)
