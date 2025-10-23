@file:Suppress("unused")
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
data class CookieParam(
  @param:JsonProperty("name")
  val name: String,
  @param:JsonProperty("value")
  val `value`: String,
  @param:JsonProperty("url")
  @param:Optional
  val url: String? = null,
  @param:JsonProperty("domain")
  @param:Optional
  val domain: String? = null,
  @param:JsonProperty("path")
  @param:Optional
  val path: String? = null,
  @param:JsonProperty("secure")
  @param:Optional
  val secure: Boolean? = null,
  @param:JsonProperty("httpOnly")
  @param:Optional
  val httpOnly: Boolean? = null,
  @param:JsonProperty("sameSite")
  @param:Optional
  val sameSite: CookieSameSite? = null,
  @param:JsonProperty("expires")
  @param:Optional
  val expires: Double? = null,
  @param:JsonProperty("priority")
  @param:Optional
  @param:Experimental
  val priority: CookiePriority? = null,
  @param:JsonProperty("sameParty")
  @param:Optional
  @param:Experimental
  val sameParty: Boolean? = null,
  @param:JsonProperty("sourceScheme")
  @param:Optional
  @param:Experimental
  val sourceScheme: CookieSourceScheme? = null,
  @param:JsonProperty("sourcePort")
  @param:Optional
  @param:Experimental
  val sourcePort: Int? = null,
)
