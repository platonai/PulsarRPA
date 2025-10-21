package ai.platon.cdt.kt.protocol.types.page

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String
import kotlin.collections.List

/**
 * Information about the Frame on the page.
 */
public data class Frame(
  @JsonProperty("id")
  public val id: String,
  @JsonProperty("parentId")
  @Optional
  public val parentId: String? = null,
  @JsonProperty("loaderId")
  public val loaderId: String,
  @JsonProperty("name")
  @Optional
  public val name: String? = null,
  @JsonProperty("url")
  public val url: String,
  @JsonProperty("urlFragment")
  @Optional
  @Experimental
  public val urlFragment: String? = null,
  @JsonProperty("domainAndRegistry")
  @Experimental
  public val domainAndRegistry: String,
  @JsonProperty("securityOrigin")
  public val securityOrigin: String,
  @JsonProperty("mimeType")
  public val mimeType: String,
  @JsonProperty("unreachableUrl")
  @Optional
  @Experimental
  public val unreachableUrl: String? = null,
  @JsonProperty("adFrameType")
  @Optional
  @Experimental
  public val adFrameType: AdFrameType? = null,
  @JsonProperty("secureContextType")
  @Experimental
  public val secureContextType: SecureContextType,
  @JsonProperty("crossOriginIsolatedContextType")
  @Experimental
  public val crossOriginIsolatedContextType: CrossOriginIsolatedContextType,
  @JsonProperty("gatedAPIFeatures")
  @Experimental
  public val gatedAPIFeatures: List<GatedAPIFeatures>,
)
