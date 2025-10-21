package ai.platon.cdt.kt.protocol.types.network

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean

@Experimental
public data class ClientSecurityState(
  @JsonProperty("initiatorIsSecureContext")
  public val initiatorIsSecureContext: Boolean,
  @JsonProperty("initiatorIPAddressSpace")
  public val initiatorIPAddressSpace: IPAddressSpace,
  @JsonProperty("privateNetworkRequestPolicy")
  public val privateNetworkRequestPolicy: PrivateNetworkRequestPolicy,
)
