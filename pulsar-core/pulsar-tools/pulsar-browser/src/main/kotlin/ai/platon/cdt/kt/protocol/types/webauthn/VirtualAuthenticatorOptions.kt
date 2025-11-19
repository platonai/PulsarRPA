@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.webauthn

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean

data class VirtualAuthenticatorOptions(
  @param:JsonProperty("protocol")
  val protocol: AuthenticatorProtocol,
  @param:JsonProperty("ctap2Version")
  @param:Optional
  val ctap2Version: Ctap2Version? = null,
  @param:JsonProperty("transport")
  val transport: AuthenticatorTransport,
  @param:JsonProperty("hasResidentKey")
  @param:Optional
  val hasResidentKey: Boolean? = null,
  @param:JsonProperty("hasUserVerification")
  @param:Optional
  val hasUserVerification: Boolean? = null,
  @param:JsonProperty("hasLargeBlob")
  @param:Optional
  val hasLargeBlob: Boolean? = null,
  @param:JsonProperty("hasCredBlob")
  @param:Optional
  val hasCredBlob: Boolean? = null,
  @param:JsonProperty("automaticPresenceSimulation")
  @param:Optional
  val automaticPresenceSimulation: Boolean? = null,
  @param:JsonProperty("isUserVerified")
  @param:Optional
  val isUserVerified: Boolean? = null,
)
