package ai.platon.cdt.kt.protocol.types.network

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean
import kotlin.Double
import kotlin.Int
import kotlin.String

/**
 * Cookie object
 */
public data class Cookie(
  @JsonProperty("name")
  public val name: String,
  @JsonProperty("value")
  public val `value`: String,
  @JsonProperty("domain")
  public val domain: String,
  @JsonProperty("path")
  public val path: String,
  @JsonProperty("expires")
  public val expires: Double,
  @JsonProperty("size")
  public val size: Int,
  @JsonProperty("httpOnly")
  public val httpOnly: Boolean,
  @JsonProperty("secure")
  public val secure: Boolean,
  @JsonProperty("session")
  public val session: Boolean,
  @JsonProperty("sameSite")
  @Optional
  public val sameSite: CookieSameSite? = null,
  @JsonProperty("priority")
  @Experimental
  public val priority: CookiePriority,
  @JsonProperty("sameParty")
  @Experimental
  public val sameParty: Boolean,
  @JsonProperty("sourceScheme")
  @Experimental
  public val sourceScheme: CookieSourceScheme,
  @JsonProperty("sourcePort")
  @Experimental
  public val sourcePort: Int,
)
