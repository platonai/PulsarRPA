@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.audits

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int
import kotlin.String

data class EncodedResponse(
  @param:JsonProperty("body")
  @param:Optional
  val body: String? = null,
  @param:JsonProperty("originalSize")
  val originalSize: Int,
  @param:JsonProperty("encodedSize")
  val encodedSize: Int,
)
