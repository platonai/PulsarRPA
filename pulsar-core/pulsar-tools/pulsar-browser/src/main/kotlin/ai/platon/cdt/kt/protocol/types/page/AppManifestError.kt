@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.page

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int
import kotlin.String

/**
 * Error while paring app manifest.
 */
data class AppManifestError(
  @param:JsonProperty("message")
  val message: String,
  @param:JsonProperty("critical")
  val critical: Int,
  @param:JsonProperty("line")
  val line: Int,
  @param:JsonProperty("column")
  val column: Int,
)
