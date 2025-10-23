@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.profiler

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

/**
 * Describes a type collected during runtime.
 */
@Experimental
data class TypeObject(
  @param:JsonProperty("name")
  val name: String,
)
