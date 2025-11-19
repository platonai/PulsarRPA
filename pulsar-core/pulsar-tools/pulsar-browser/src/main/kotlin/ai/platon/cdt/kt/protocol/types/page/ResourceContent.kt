@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.page

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean
import kotlin.String

data class ResourceContent(
  @param:JsonProperty("content")
  val content: String,
  @param:JsonProperty("base64Encoded")
  val base64Encoded: Boolean,
)
