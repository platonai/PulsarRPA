package ai.platon.cdt.kt.protocol.types.input

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

@Experimental
public data class DragDataItem(
  @JsonProperty("mimeType")
  public val mimeType: String,
  @JsonProperty("data")
  public val `data`: String,
  @JsonProperty("title")
  @Optional
  public val title: String? = null,
  @JsonProperty("baseURL")
  @Optional
  public val baseURL: String? = null,
)
