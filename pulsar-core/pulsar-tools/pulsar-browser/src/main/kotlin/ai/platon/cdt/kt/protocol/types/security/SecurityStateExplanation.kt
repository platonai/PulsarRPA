@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.security

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String
import kotlin.collections.List

/**
 * An explanation of an factor contributing to the security state.
 */
data class SecurityStateExplanation(
  @param:JsonProperty("securityState")
  val securityState: SecurityState,
  @param:JsonProperty("title")
  val title: String,
  @param:JsonProperty("summary")
  val summary: String,
  @param:JsonProperty("description")
  val description: String,
  @param:JsonProperty("mixedContentType")
  val mixedContentType: MixedContentType,
  @param:JsonProperty("certificate")
  val certificate: List<String>,
  @param:JsonProperty("recommendations")
  @param:Optional
  val recommendations: List<String>? = null,
)
