@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.css

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean
import kotlin.Double
import kotlin.Int
import kotlin.String

/**
 * CSS stylesheet metainformation.
 */
data class CSSStyleSheetHeader(
  @param:JsonProperty("styleSheetId")
  val styleSheetId: String,
  @param:JsonProperty("frameId")
  val frameId: String,
  @param:JsonProperty("sourceURL")
  val sourceURL: String,
  @param:JsonProperty("sourceMapURL")
  @param:Optional
  val sourceMapURL: String? = null,
  @param:JsonProperty("origin")
  val origin: StyleSheetOrigin,
  @param:JsonProperty("title")
  val title: String,
  @param:JsonProperty("ownerNode")
  @param:Optional
  val ownerNode: Int? = null,
  @param:JsonProperty("disabled")
  val disabled: Boolean,
  @param:JsonProperty("hasSourceURL")
  @param:Optional
  val hasSourceURL: Boolean? = null,
  @param:JsonProperty("isInline")
  val isInline: Boolean,
  @param:JsonProperty("isMutable")
  val isMutable: Boolean,
  @param:JsonProperty("isConstructed")
  val isConstructed: Boolean,
  @param:JsonProperty("startLine")
  val startLine: Double,
  @param:JsonProperty("startColumn")
  val startColumn: Double,
  @param:JsonProperty("length")
  val length: Double,
  @param:JsonProperty("endLine")
  val endLine: Double,
  @param:JsonProperty("endColumn")
  val endColumn: Double,
)
