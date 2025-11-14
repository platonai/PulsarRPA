@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.audits

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int
import kotlin.String

data class SourceCodeLocation(
  @param:JsonProperty("scriptId")
  @param:Optional
  val scriptId: String? = null,
  @param:JsonProperty("url")
  val url: String,
  @param:JsonProperty("lineNumber")
  val lineNumber: Int,
  @param:JsonProperty("columnNumber")
  val columnNumber: Int,
)
