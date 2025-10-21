package ai.platon.cdt.kt.protocol.types.webauthn

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean

public data class VirtualAuthenticatorOptions(
  @JsonProperty("protocol")
  public val protocol: AuthenticatorProtocol,
  @JsonProperty("ctap2Version")
  @Optional
  public val ctap2Version: Ctap2Version? = null,
  @JsonProperty("transport")
  public val transport: AuthenticatorTransport,
  @JsonProperty("hasResidentKey")
  @Optional
  public val hasResidentKey: Boolean? = null,
  @JsonProperty("hasUserVerification")
  @Optional
  public val hasUserVerification: Boolean? = null,
  @JsonProperty("hasLargeBlob")
  @Optional
  public val hasLargeBlob: Boolean? = null,
  @JsonProperty("hasCredBlob")
  @Optional
  public val hasCredBlob: Boolean? = null,
  @JsonProperty("automaticPresenceSimulation")
  @Optional
  public val automaticPresenceSimulation: Boolean? = null,
  @JsonProperty("isUserVerified")
  @Optional
  public val isUserVerified: Boolean? = null,
)
