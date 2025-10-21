package ai.platon.cdt.kt.protocol.types.webauthn

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean
import kotlin.Int
import kotlin.String

public data class Credential(
  @JsonProperty("credentialId")
  public val credentialId: String,
  @JsonProperty("isResidentCredential")
  public val isResidentCredential: Boolean,
  @JsonProperty("rpId")
  @Optional
  public val rpId: String? = null,
  @JsonProperty("privateKey")
  public val privateKey: String,
  @JsonProperty("userHandle")
  @Optional
  public val userHandle: String? = null,
  @JsonProperty("signCount")
  public val signCount: Int,
  @JsonProperty("largeBlob")
  @Optional
  public val largeBlob: String? = null,
)
