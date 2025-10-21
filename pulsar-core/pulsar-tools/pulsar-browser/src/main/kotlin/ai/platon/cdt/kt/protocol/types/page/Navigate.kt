package ai.platon.cdt.kt.protocol.types.page

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

public data class Navigate(
  @JsonProperty("frameId")
  public val frameId: String,
  @JsonProperty("loaderId")
  @Optional
  public val loaderId: String? = null,
  @JsonProperty("errorText")
  @Optional
  public val errorText: String? = null,
)
