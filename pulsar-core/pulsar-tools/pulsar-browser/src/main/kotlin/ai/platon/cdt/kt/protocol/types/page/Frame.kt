@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.page

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String
import kotlin.collections.List

/**
 * Information about the Frame on the page.
 */
data class Frame(
  @param:JsonProperty("id")
  val id: String,
  @param:JsonProperty("parentId")
  @param:Optional
  val parentId: String? = null,
  @param:JsonProperty("loaderId")
  val loaderId: String,
  @param:JsonProperty("name")
  @param:Optional
  val name: String? = null,
  @param:JsonProperty("url")
  val url: String,
  @param:JsonProperty("urlFragment")
  @param:Optional
  @param:Experimental
  val urlFragment: String? = null,
  @param:JsonProperty("domainAndRegistry")
  @param:Experimental
  val domainAndRegistry: String,
  @param:JsonProperty("securityOrigin")
  val securityOrigin: String,
  @param:JsonProperty("mimeType")
  val mimeType: String,
  @param:JsonProperty("unreachableUrl")
  @param:Optional
  @param:Experimental
  val unreachableUrl: String? = null,
  @param:JsonProperty("adFrameType")
  @param:Optional
  @param:Experimental
  val adFrameType: AdFrameType? = null,
  @param:JsonProperty("secureContextType")
  @param:Experimental
  val secureContextType: SecureContextType,
  @param:JsonProperty("crossOriginIsolatedContextType")
  @param:Experimental
  val crossOriginIsolatedContextType: CrossOriginIsolatedContextType,
  @param:JsonProperty("gatedAPIFeatures")
  @param:Experimental
  val gatedAPIFeatures: List<GatedAPIFeatures>,
)
