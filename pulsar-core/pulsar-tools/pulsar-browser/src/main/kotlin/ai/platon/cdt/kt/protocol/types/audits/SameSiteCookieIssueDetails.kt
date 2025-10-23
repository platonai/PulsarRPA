@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.audits

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String
import kotlin.collections.List

/**
 * This information is currently necessary, as the front-end has a difficult
 * time finding a specific cookie. With this, we can convey specific error
 * information without the cookie.
 */
data class SameSiteCookieIssueDetails(
  @param:JsonProperty("cookie")
  val cookie: AffectedCookie,
  @param:JsonProperty("cookieWarningReasons")
  val cookieWarningReasons: List<SameSiteCookieWarningReason>,
  @param:JsonProperty("cookieExclusionReasons")
  val cookieExclusionReasons: List<SameSiteCookieExclusionReason>,
  @param:JsonProperty("operation")
  val operation: SameSiteCookieOperation,
  @param:JsonProperty("siteForCookies")
  @param:Optional
  val siteForCookies: String? = null,
  @param:JsonProperty("cookieUrl")
  @param:Optional
  val cookieUrl: String? = null,
  @param:JsonProperty("request")
  @param:Optional
  val request: AffectedRequest? = null,
)
