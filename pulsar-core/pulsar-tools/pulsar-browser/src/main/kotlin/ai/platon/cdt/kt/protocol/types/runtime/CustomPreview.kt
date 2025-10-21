package ai.platon.cdt.kt.protocol.types.runtime

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

@Experimental
public data class CustomPreview(
  @JsonProperty("header")
  public val `header`: String,
  @JsonProperty("bodyGetterId")
  @Optional
  public val bodyGetterId: String? = null,
)
