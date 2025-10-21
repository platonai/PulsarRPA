package ai.platon.cdt.kt.protocol.types.browser

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

public data class Version(
  @JsonProperty("protocolVersion")
  public val protocolVersion: String,
  @JsonProperty("product")
  public val product: String,
  @JsonProperty("revision")
  public val revision: String,
  @JsonProperty("userAgent")
  public val userAgent: String,
  @JsonProperty("jsVersion")
  public val jsVersion: String,
)
