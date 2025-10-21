package ai.platon.cdt.kt.protocol.types.network

import com.fasterxml.jackson.`annotation`.JsonProperty

public enum class CrossOriginEmbedderPolicyValue {
  @JsonProperty("None")
  NONE,
  @JsonProperty("CorsOrCredentialless")
  CORS_OR_CREDENTIALLESS,
  @JsonProperty("RequireCorp")
  REQUIRE_CORP,
}
