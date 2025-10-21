package ai.platon.cdt.kt.protocol.types.network

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty

@Experimental
public data class SecurityIsolationStatus(
  @JsonProperty("coop")
  @Optional
  public val coop: CrossOriginOpenerPolicyStatus? = null,
  @JsonProperty("coep")
  @Optional
  public val coep: CrossOriginEmbedderPolicyStatus? = null,
)
