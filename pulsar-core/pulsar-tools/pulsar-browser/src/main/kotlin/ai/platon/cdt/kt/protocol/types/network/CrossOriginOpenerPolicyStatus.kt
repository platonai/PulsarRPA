@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.network

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

@Experimental
data class CrossOriginOpenerPolicyStatus(
  @param:JsonProperty("value")
  val `value`: CrossOriginOpenerPolicyValue,
  @param:JsonProperty("reportOnlyValue")
  val reportOnlyValue: CrossOriginOpenerPolicyValue,
  @param:JsonProperty("reportingEndpoint")
  @param:Optional
  val reportingEndpoint: String? = null,
  @param:JsonProperty("reportOnlyReportingEndpoint")
  @param:Optional
  val reportOnlyReportingEndpoint: String? = null,
)
