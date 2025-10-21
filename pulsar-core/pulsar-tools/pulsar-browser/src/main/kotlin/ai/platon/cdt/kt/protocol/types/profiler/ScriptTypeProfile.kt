package ai.platon.cdt.kt.protocol.types.profiler

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String
import kotlin.collections.List

/**
 * Type profile data collected during runtime for a JavaScript script.
 */
@Experimental
public data class ScriptTypeProfile(
  @JsonProperty("scriptId")
  public val scriptId: String,
  @JsonProperty("url")
  public val url: String,
  @JsonProperty("entries")
  public val entries: List<TypeProfileEntry>,
)
