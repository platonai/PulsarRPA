@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.emulation

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean
import kotlin.String
import kotlin.collections.List

/**
 * Used to specify User Agent Cient Hints to emulate. See https://wicg.github.io/ua-client-hints
 * Missing optional values will be filled in by the target with what it would normally use.
 */
@Experimental
data class UserAgentMetadata(
  @param:JsonProperty("brands")
  @param:Optional
  val brands: List<UserAgentBrandVersion>? = null,
  @param:JsonProperty("fullVersion")
  @param:Optional
  val fullVersion: String? = null,
  @param:JsonProperty("platform")
  val platform: String,
  @param:JsonProperty("platformVersion")
  val platformVersion: String,
  @param:JsonProperty("architecture")
  val architecture: String,
  @param:JsonProperty("model")
  val model: String,
  @param:JsonProperty("mobile")
  val mobile: Boolean,
)
