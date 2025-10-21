package ai.platon.cdt.kt.protocol.events.network

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import ai.platon.cdt.kt.protocol.types.network.AuthChallenge
import ai.platon.cdt.kt.protocol.types.network.ErrorReason
import ai.platon.cdt.kt.protocol.types.network.Request
import ai.platon.cdt.kt.protocol.types.network.ResourceType
import com.fasterxml.jackson.`annotation`.JsonProperty
import java.lang.Deprecated
import kotlin.Any
import kotlin.Boolean
import kotlin.Int
import kotlin.String
import kotlin.collections.Map

/**
 * Details of an intercepted HTTP request, which must be either allowed, blocked, modified or
 * mocked.
 * Deprecated, use Fetch.requestPaused instead.
 */
@Experimental
@Deprecated
public data class RequestIntercepted(
  @JsonProperty("interceptionId")
  public val interceptionId: String,
  @JsonProperty("request")
  public val request: Request,
  @JsonProperty("frameId")
  public val frameId: String,
  @JsonProperty("resourceType")
  public val resourceType: ResourceType,
  @JsonProperty("isNavigationRequest")
  public val isNavigationRequest: Boolean,
  @JsonProperty("isDownload")
  @Optional
  public val isDownload: Boolean? = null,
  @JsonProperty("redirectUrl")
  @Optional
  public val redirectUrl: String? = null,
  @JsonProperty("authChallenge")
  @Optional
  public val authChallenge: AuthChallenge? = null,
  @JsonProperty("responseErrorReason")
  @Optional
  public val responseErrorReason: ErrorReason? = null,
  @JsonProperty("responseStatusCode")
  @Optional
  public val responseStatusCode: Int? = null,
  @JsonProperty("responseHeaders")
  @Optional
  public val responseHeaders: Map<String, Any?>? = null,
  @JsonProperty("requestId")
  @Optional
  public val requestId: String? = null,
)
