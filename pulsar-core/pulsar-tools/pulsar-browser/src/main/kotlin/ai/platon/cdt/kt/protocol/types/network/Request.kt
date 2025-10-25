@file:Suppress("unused")
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
data class Request(
  @param:JsonProperty("url")
  val url: String,
  @param:JsonProperty("urlFragment")
  @param:Optional
  val urlFragment: String? = null,
  @param:JsonProperty("method")
  val method: String,
  @param:JsonProperty("headers")
  val headers: MutableMap<String, Any?>,
  @param:JsonProperty("postData")
  @param:Optional
  val postData: String? = null,
  @param:JsonProperty("hasPostData")
  @param:Optional
  val hasPostData: Boolean? = null,
  @param:JsonProperty("postDataEntries")
  @param:Optional
  @param:Experimental
  val postDataEntries: List<PostDataEntry>? = null,
  @param:JsonProperty("mixedContentType")
  @param:Optional
  val mixedContentType: MixedContentType? = null,
  @param:JsonProperty("initialPriority")
  val initialPriority: ResourcePriority,
  @param:JsonProperty("referrerPolicy")
  val referrerPolicy: RequestReferrerPolicy,
  @param:JsonProperty("isLinkPreload")
  @param:Optional
  val isLinkPreload: Boolean? = null,
  @param:JsonProperty("trustTokenParams")
  @param:Optional
  @param:Experimental
  val trustTokenParams: TrustTokenParams? = null,
)
