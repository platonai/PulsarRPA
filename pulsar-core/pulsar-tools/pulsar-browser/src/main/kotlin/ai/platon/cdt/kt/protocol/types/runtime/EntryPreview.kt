package ai.platon.cdt.kt.protocol.types.runtime

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty

@Experimental
public data class EntryPreview(
  @JsonProperty("key")
  @Optional
  public val key: ObjectPreview? = null,
  @JsonProperty("value")
  public val `value`: ObjectPreview,
)
