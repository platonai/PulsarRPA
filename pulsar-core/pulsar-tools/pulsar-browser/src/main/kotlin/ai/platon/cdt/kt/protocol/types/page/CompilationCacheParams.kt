@file:Suppress("unused")
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
data class CompilationCacheParams(
  @param:JsonProperty("url")
  val url: String,
  @param:JsonProperty("eager")
  @param:Optional
  val eager: Boolean? = null,
)
