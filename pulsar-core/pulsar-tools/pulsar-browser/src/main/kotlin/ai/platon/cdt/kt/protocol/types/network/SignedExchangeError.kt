@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.network

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int
import kotlin.String

/**
 * Information about a signed exchange response.
 */
@Experimental
data class SignedExchangeError(
  @param:JsonProperty("message")
  val message: String,
  @param:JsonProperty("signatureIndex")
  @param:Optional
  val signatureIndex: Int? = null,
  @param:JsonProperty("errorField")
  @param:Optional
  val errorField: SignedExchangeErrorField? = null,
)
