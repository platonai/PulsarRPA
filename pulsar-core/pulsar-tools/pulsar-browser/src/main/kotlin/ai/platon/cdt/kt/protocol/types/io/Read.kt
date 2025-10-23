@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.io

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean
import kotlin.String

data class Read(
  @param:JsonProperty("base64Encoded")
  @param:Optional
  val base64Encoded: Boolean? = null,
  @param:JsonProperty("data")
  val `data`: String,
  @param:JsonProperty("eof")
  val eof: Boolean,
)
