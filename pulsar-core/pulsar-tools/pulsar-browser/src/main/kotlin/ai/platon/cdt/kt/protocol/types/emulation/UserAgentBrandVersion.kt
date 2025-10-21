package ai.platon.cdt.kt.protocol.types.emulation

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

/**
 * Used to specify User Agent Cient Hints to emulate. See https://wicg.github.io/ua-client-hints
 */
@Experimental
public data class UserAgentBrandVersion(
  @JsonProperty("brand")
  public val brand: String,
  @JsonProperty("version")
  public val version: String,
)
