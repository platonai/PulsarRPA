@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.security

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean
import kotlin.Deprecated

/**
 * Information about insecure content on the page.
 */
@Deprecated("Deprecated")
data class InsecureContentStatus(
  @param:JsonProperty("ranMixedContent")
  val ranMixedContent: Boolean,
  @param:JsonProperty("displayedMixedContent")
  val displayedMixedContent: Boolean,
  @param:JsonProperty("containedMixedForm")
  val containedMixedForm: Boolean,
  @param:JsonProperty("ranContentWithCertErrors")
  val ranContentWithCertErrors: Boolean,
  @param:JsonProperty("displayedContentWithCertErrors")
  val displayedContentWithCertErrors: Boolean,
  @param:JsonProperty("ranInsecureContentStyle")
  val ranInsecureContentStyle: SecurityState,
  @param:JsonProperty("displayedInsecureContentStyle")
  val displayedInsecureContentStyle: SecurityState,
)
