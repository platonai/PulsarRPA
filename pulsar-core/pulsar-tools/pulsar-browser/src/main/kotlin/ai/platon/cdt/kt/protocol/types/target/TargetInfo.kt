package ai.platon.cdt.kt.protocol.types.target

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean
import kotlin.String

public data class TargetInfo(
  @JsonProperty("targetId")
  public val targetId: String,
  @JsonProperty("type")
  public val type: String,
  @JsonProperty("title")
  public val title: String,
  @JsonProperty("url")
  public val url: String,
  @JsonProperty("attached")
  public val attached: Boolean,
  @JsonProperty("openerId")
  @Optional
  public val openerId: String? = null,
  @JsonProperty("canAccessOpener")
  @Experimental
  public val canAccessOpener: Boolean,
  @JsonProperty("openerFrameId")
  @Optional
  @Experimental
  public val openerFrameId: String? = null,
  @JsonProperty("browserContextId")
  @Optional
  @Experimental
  public val browserContextId: String? = null,
)
