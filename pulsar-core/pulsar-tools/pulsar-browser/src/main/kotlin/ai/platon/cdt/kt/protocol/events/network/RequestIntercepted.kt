@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.events.network

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import ai.platon.cdt.kt.protocol.types.network.AuthChallenge
import ai.platon.cdt.kt.protocol.types.network.ErrorReason
import ai.platon.cdt.kt.protocol.types.network.Request
import ai.platon.cdt.kt.protocol.types.network.ResourceType
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Any
import kotlin.Boolean
import kotlin.Deprecated
import kotlin.Int
import kotlin.String
import kotlin.collections.Map

/**
 * Details of an intercepted HTTP request, which must be either allowed, blocked, modified or
 * mocked.
 * Deprecated, use Fetch.requestPaused instead.
 */
@Experimental
@Deprecated("Deprecated")
data class RequestIntercepted(
  @param:JsonProperty("interceptionId")
  val interceptionId: String,
  @param:JsonProperty("request")
  val request: Request,
  @param:JsonProperty("frameId")
  val frameId: String,
  @param:JsonProperty("resourceType")
  val resourceType: ResourceType,
  @param:JsonProperty("isNavigationRequest")
  val isNavigationRequest: Boolean,
  @param:JsonProperty("isDownload")
  @param:Optional
  val isDownload: Boolean? = null,
  @param:JsonProperty("redirectUrl")
  @param:Optional
  val redirectUrl: String? = null,
  @param:JsonProperty("authChallenge")
  @param:Optional
  val authChallenge: AuthChallenge? = null,
  @param:JsonProperty("responseErrorReason")
  @param:Optional
  val responseErrorReason: ErrorReason? = null,
  @param:JsonProperty("responseStatusCode")
  @param:Optional
  val responseStatusCode: Int? = null,
  @param:JsonProperty("responseHeaders")
  @param:Optional
  val responseHeaders: Map<String, Any?>? = null,
  @param:JsonProperty("requestId")
  @param:Optional
  val requestId: String? = null,
)
