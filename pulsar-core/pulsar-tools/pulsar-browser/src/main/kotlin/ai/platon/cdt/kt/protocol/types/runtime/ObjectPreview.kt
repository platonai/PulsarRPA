package ai.platon.cdt.kt.protocol.types.runtime

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean
import kotlin.String
import kotlin.collections.List

/**
 * Object containing abbreviated remote object value.
 */
@Experimental
public data class ObjectPreview(
  @JsonProperty("type")
  public val type: ObjectPreviewType,
  @JsonProperty("subtype")
  @Optional
  public val subtype: ObjectPreviewSubtype? = null,
  @JsonProperty("description")
  @Optional
  public val description: String? = null,
  @JsonProperty("overflow")
  public val overflow: Boolean,
  @JsonProperty("properties")
  public val properties: List<PropertyPreview>,
  @JsonProperty("entries")
  @Optional
  public val entries: List<EntryPreview>? = null,
)
