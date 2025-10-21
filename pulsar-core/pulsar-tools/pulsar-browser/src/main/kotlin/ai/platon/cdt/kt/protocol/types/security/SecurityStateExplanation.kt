package ai.platon.cdt.kt.protocol.types.security

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String
import kotlin.collections.List

/**
 * An explanation of an factor contributing to the security state.
 */
public data class SecurityStateExplanation(
  @JsonProperty("securityState")
  public val securityState: SecurityState,
  @JsonProperty("title")
  public val title: String,
  @JsonProperty("summary")
  public val summary: String,
  @JsonProperty("description")
  public val description: String,
  @JsonProperty("mixedContentType")
  public val mixedContentType: MixedContentType,
  @JsonProperty("certificate")
  public val certificate: List<String>,
  @JsonProperty("recommendations")
  @Optional
  public val recommendations: List<String>? = null,
)
