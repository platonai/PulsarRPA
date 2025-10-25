@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.audits

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

/**
 * Information about a request that is affected by an inspector issue.
 */
data class AffectedRequest(
  @param:JsonProperty("requestId")
  val requestId: String,
  @param:JsonProperty("url")
  @param:Optional
  val url: String? = null,
)
