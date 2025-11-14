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
 * Cookie object
 */
data class Cookie(
  @param:JsonProperty("name")
  val name: String,
  @param:JsonProperty("value")
  val `value`: String,
  @param:JsonProperty("domain")
  val domain: String,
  @param:JsonProperty("path")
  val path: String,
  @param:JsonProperty("expires")
  val expires: Double,
  @param:JsonProperty("size")
  val size: Int,
  @param:JsonProperty("httpOnly")
  val httpOnly: Boolean,
  @param:JsonProperty("secure")
  val secure: Boolean,
  @param:JsonProperty("session")
  val session: Boolean,
  @param:JsonProperty("sameSite")
  @param:Optional
  val sameSite: CookieSameSite? = null,
  @param:JsonProperty("priority")
  @param:Experimental
  val priority: CookiePriority,
  @param:JsonProperty("sameParty")
  @param:Experimental
  val sameParty: Boolean,
  @param:JsonProperty("sourceScheme")
  @param:Experimental
  val sourceScheme: CookieSourceScheme,
  @param:JsonProperty("sourcePort")
  @param:Experimental
  val sourcePort: Int,
)
