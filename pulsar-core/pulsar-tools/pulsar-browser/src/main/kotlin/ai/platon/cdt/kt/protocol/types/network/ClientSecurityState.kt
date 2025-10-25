@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.network

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean

@Experimental
data class ClientSecurityState(
  @param:JsonProperty("initiatorIsSecureContext")
  val initiatorIsSecureContext: Boolean,
  @param:JsonProperty("initiatorIPAddressSpace")
  val initiatorIPAddressSpace: IPAddressSpace,
  @param:JsonProperty("privateNetworkRequestPolicy")
  val privateNetworkRequestPolicy: PrivateNetworkRequestPolicy,
)
