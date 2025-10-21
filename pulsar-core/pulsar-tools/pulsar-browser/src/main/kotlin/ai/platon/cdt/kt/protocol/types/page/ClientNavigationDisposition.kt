package ai.platon.cdt.kt.protocol.types.page

import com.fasterxml.jackson.`annotation`.JsonProperty

public enum class ClientNavigationDisposition {
  @JsonProperty("currentTab")
  CURRENT_TAB,
  @JsonProperty("newTab")
  NEW_TAB,
  @JsonProperty("newWindow")
  NEW_WINDOW,
  @JsonProperty("download")
  DOWNLOAD,
}
