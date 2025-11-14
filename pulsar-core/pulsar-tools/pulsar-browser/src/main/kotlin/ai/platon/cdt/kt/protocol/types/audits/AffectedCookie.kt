@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.audits

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

/**
 * Information about a cookie that is affected by an inspector issue.
 */
data class AffectedCookie(
  @param:JsonProperty("name")
  val name: String,
  @param:JsonProperty("path")
  val path: String,
  @param:JsonProperty("domain")
  val domain: String,
)
