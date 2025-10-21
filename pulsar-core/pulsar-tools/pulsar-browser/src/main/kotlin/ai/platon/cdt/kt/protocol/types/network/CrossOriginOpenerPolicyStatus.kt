package ai.platon.cdt.kt.protocol.types.network

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

@Experimental
public data class CrossOriginOpenerPolicyStatus(
  @JsonProperty("value")
  public val `value`: CrossOriginOpenerPolicyValue,
  @JsonProperty("reportOnlyValue")
  public val reportOnlyValue: CrossOriginOpenerPolicyValue,
  @JsonProperty("reportingEndpoint")
  @Optional
  public val reportingEndpoint: String? = null,
  @JsonProperty("reportOnlyReportingEndpoint")
  @Optional
  public val reportOnlyReportingEndpoint: String? = null,
)
