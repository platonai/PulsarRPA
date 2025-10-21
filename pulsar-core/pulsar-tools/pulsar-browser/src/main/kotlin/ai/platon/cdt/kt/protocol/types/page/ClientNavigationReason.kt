package ai.platon.cdt.kt.protocol.types.page

import com.fasterxml.jackson.`annotation`.JsonProperty

public enum class ClientNavigationReason {
  @JsonProperty("formSubmissionGet")
  FORM_SUBMISSION_GET,
  @JsonProperty("formSubmissionPost")
  FORM_SUBMISSION_POST,
  @JsonProperty("httpHeaderRefresh")
  HTTP_HEADER_REFRESH,
  @JsonProperty("scriptInitiated")
  SCRIPT_INITIATED,
  @JsonProperty("metaTagRefresh")
  META_TAG_REFRESH,
  @JsonProperty("pageBlockInterstitial")
  PAGE_BLOCK_INTERSTITIAL,
  @JsonProperty("reload")
  RELOAD,
  @JsonProperty("anchorClick")
  ANCHOR_CLICK,
}
