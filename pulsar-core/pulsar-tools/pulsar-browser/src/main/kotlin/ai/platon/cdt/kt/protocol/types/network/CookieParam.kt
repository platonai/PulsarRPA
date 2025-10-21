package ai.platon.cdt.kt.protocol.types.network

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean
import kotlin.Double
import kotlin.Int
import kotlin.String

/**
 * Cookie parameter object
 */
public data class CookieParam(
  @JsonProperty("name")
  public val name: String,
  @JsonProperty("value")
  public val `value`: String,
  @JsonProperty("url")
  @Optional
  public val url: String? = null,
  @JsonProperty("domain")
  @Optional
  public val domain: String? = null,
  @JsonProperty("path")
  @Optional
  public val path: String? = null,
  @JsonProperty("secure")
  @Optional
  public val secure: Boolean? = null,
  @JsonProperty("httpOnly")
  @Optional
  public val httpOnly: Boolean? = null,
  @JsonProperty("sameSite")
  @Optional
  public val sameSite: CookieSameSite? = null,
  @JsonProperty("expires")
  @Optional
  public val expires: Double? = null,
  @JsonProperty("priority")
  @Optional
  @Experimental
  public val priority: CookiePriority? = null,
  @JsonProperty("sameParty")
  @Optional
  @Experimental
  public val sameParty: Boolean? = null,
  @JsonProperty("sourceScheme")
  @Optional
  @Experimental
  public val sourceScheme: CookieSourceScheme? = null,
  @JsonProperty("sourcePort")
  @Optional
  @Experimental
  public val sourcePort: Int? = null,
)
