package ai.platon.cdt.kt.protocol.types.audits

import com.fasterxml.jackson.`annotation`.JsonProperty

public enum class TwaQualityEnforcementViolationType {
  @JsonProperty("kHttpError")
  K_HTTP_ERROR,
  @JsonProperty("kUnavailableOffline")
  K_UNAVAILABLE_OFFLINE,
  @JsonProperty("kDigitalAssetLinks")
  K_DIGITAL_ASSET_LINKS,
}
