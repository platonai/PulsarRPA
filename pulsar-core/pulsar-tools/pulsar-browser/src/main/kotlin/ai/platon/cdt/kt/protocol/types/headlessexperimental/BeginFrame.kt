package ai.platon.cdt.kt.protocol.types.headlessexperimental

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean
import kotlin.String

public data class BeginFrame(
  @JsonProperty("hasDamage")
  public val hasDamage: Boolean,
  @JsonProperty("screenshotData")
  @Optional
  public val screenshotData: String? = null,
)
