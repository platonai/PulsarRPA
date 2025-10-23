@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.webauthn

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean
import kotlin.Int
import kotlin.String

data class Credential(
  @param:JsonProperty("credentialId")
  val credentialId: String,
  @param:JsonProperty("isResidentCredential")
  val isResidentCredential: Boolean,
  @param:JsonProperty("rpId")
  @param:Optional
  val rpId: String? = null,
  @param:JsonProperty("privateKey")
  val privateKey: String,
  @param:JsonProperty("userHandle")
  @param:Optional
  val userHandle: String? = null,
  @param:JsonProperty("signCount")
  val signCount: Int,
  @param:JsonProperty("largeBlob")
  @param:Optional
  val largeBlob: String? = null,
)
