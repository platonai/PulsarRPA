@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.browser

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

data class Version(
  @param:JsonProperty("protocolVersion")
  val protocolVersion: String,
  @param:JsonProperty("product")
  val product: String,
  @param:JsonProperty("revision")
  val revision: String,
  @param:JsonProperty("userAgent")
  val userAgent: String,
  @param:JsonProperty("jsVersion")
  val jsVersion: String,
)
