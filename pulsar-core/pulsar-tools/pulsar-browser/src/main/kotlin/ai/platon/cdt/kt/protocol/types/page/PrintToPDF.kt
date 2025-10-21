package ai.platon.cdt.kt.protocol.types.page

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

public data class PrintToPDF(
  @JsonProperty("data")
  public val `data`: String,
  @JsonProperty("stream")
  @Optional
  @Experimental
  public val stream: String? = null,
)
