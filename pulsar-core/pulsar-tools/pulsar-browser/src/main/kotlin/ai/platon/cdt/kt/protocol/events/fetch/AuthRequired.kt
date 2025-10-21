package ai.platon.cdt.kt.protocol.events.fetch

import ai.platon.cdt.kt.protocol.types.fetch.AuthChallenge
import ai.platon.cdt.kt.protocol.types.network.Request
import ai.platon.cdt.kt.protocol.types.network.ResourceType
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

/**
 * Issued when the domain is enabled with handleAuthRequests set to true.
 * The request is paused until client responds with continueWithAuth.
 */
public data class AuthRequired(
  @JsonProperty("requestId")
  public val requestId: String,
  @JsonProperty("request")
  public val request: Request,
  @JsonProperty("frameId")
  public val frameId: String,
  @JsonProperty("resourceType")
  public val resourceType: ResourceType,
  @JsonProperty("authChallenge")
  public val authChallenge: AuthChallenge,
)
