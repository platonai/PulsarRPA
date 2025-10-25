@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.network

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty

@Experimental
data class SecurityIsolationStatus(
  @param:JsonProperty("coop")
  @param:Optional
  val coop: CrossOriginOpenerPolicyStatus? = null,
  @param:JsonProperty("coep")
  @param:Optional
  val coep: CrossOriginEmbedderPolicyStatus? = null,
)
