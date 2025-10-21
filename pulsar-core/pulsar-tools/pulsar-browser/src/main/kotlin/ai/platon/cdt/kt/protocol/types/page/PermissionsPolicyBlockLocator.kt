package ai.platon.cdt.kt.protocol.types.page

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

@Experimental
public data class PermissionsPolicyBlockLocator(
  @JsonProperty("frameId")
  public val frameId: String,
  @JsonProperty("blockReason")
  public val blockReason: PermissionsPolicyBlockReason,
)
