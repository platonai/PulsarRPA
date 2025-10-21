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
public data class SameSiteCookieIssueDetails(
  @JsonProperty("cookie")
  public val cookie: AffectedCookie,
  @JsonProperty("cookieWarningReasons")
  public val cookieWarningReasons: List<SameSiteCookieWarningReason>,
  @JsonProperty("cookieExclusionReasons")
  public val cookieExclusionReasons: List<SameSiteCookieExclusionReason>,
  @JsonProperty("operation")
  public val operation: SameSiteCookieOperation,
  @JsonProperty("siteForCookies")
  @Optional
  public val siteForCookies: String? = null,
  @JsonProperty("cookieUrl")
  @Optional
  public val cookieUrl: String? = null,
  @JsonProperty("request")
  @Optional
  public val request: AffectedRequest? = null,
)
