package ai.platon.cdt.kt.protocol.types.audits

import com.fasterxml.jackson.`annotation`.JsonProperty

public enum class SameSiteCookieOperation {
  @JsonProperty("SetCookie")
  SET_COOKIE,
  @JsonProperty("ReadCookie")
  READ_COOKIE,
}
