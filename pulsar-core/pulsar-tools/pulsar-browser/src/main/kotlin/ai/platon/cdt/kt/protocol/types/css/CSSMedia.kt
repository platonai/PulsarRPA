package ai.platon.cdt.kt.protocol.types.css

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String
import kotlin.collections.List

/**
 * CSS media rule descriptor.
 */
public data class CSSMedia(
  @JsonProperty("text")
  public val text: String,
  @JsonProperty("source")
  public val source: CSSMediaSource,
  @JsonProperty("sourceURL")
  @Optional
  public val sourceURL: String? = null,
  @JsonProperty("range")
  @Optional
  public val range: SourceRange? = null,
  @JsonProperty("styleSheetId")
  @Optional
  public val styleSheetId: String? = null,
  @JsonProperty("mediaList")
  @Optional
  public val mediaList: List<MediaQuery>? = null,
)
