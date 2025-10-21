package ai.platon.cdt.kt.protocol.types.page

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean
import kotlin.String

/**
 * Per-script compilation cache parameters for `Page.produceCompilationCache`
 */
@Experimental
public data class CompilationCacheParams(
  @JsonProperty("url")
  public val url: String,
  @JsonProperty("eager")
  @Optional
  public val eager: Boolean? = null,
)
