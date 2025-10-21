package ai.platon.cdt.kt.protocol.types.network

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import ai.platon.cdt.kt.protocol.types.security.MixedContentType
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Any
import kotlin.Boolean
import kotlin.String
import kotlin.collections.List
import kotlin.collections.Map

/**
 * HTTP request data.
 */
public data class Request(
  @JsonProperty("url")
  public val url: String,
  @JsonProperty("urlFragment")
  @Optional
  public val urlFragment: String? = null,
  @JsonProperty("method")
  public val method: String,
  @JsonProperty("headers")
  public val headers: MutableMap<String, Any?>,
  @JsonProperty("postData")
  @Optional
  public val postData: String? = null,
  @JsonProperty("hasPostData")
  @Optional
  public val hasPostData: Boolean? = null,
  @JsonProperty("postDataEntries")
  @Optional
  @Experimental
  public val postDataEntries: List<PostDataEntry>? = null,
  @JsonProperty("mixedContentType")
  @Optional
  public val mixedContentType: MixedContentType? = null,
  @JsonProperty("initialPriority")
  public val initialPriority: ResourcePriority,
  @JsonProperty("referrerPolicy")
  public val referrerPolicy: RequestReferrerPolicy,
  @JsonProperty("isLinkPreload")
  @Optional
  public val isLinkPreload: Boolean? = null,
  @JsonProperty("trustTokenParams")
  @Optional
  @Experimental
  public val trustTokenParams: TrustTokenParams? = null,
)
