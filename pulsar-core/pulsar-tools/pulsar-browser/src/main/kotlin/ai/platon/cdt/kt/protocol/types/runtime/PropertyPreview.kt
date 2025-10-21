package ai.platon.cdt.kt.protocol.types.runtime

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

@Experimental
public data class PropertyPreview(
  @JsonProperty("name")
  public val name: String,
  @JsonProperty("type")
  public val type: PropertyPreviewType,
  @JsonProperty("value")
  @Optional
  public val `value`: String? = null,
  @JsonProperty("valuePreview")
  @Optional
  public val valuePreview: ObjectPreview? = null,
  @JsonProperty("subtype")
  @Optional
  public val subtype: PropertyPreviewSubtype? = null,
)
