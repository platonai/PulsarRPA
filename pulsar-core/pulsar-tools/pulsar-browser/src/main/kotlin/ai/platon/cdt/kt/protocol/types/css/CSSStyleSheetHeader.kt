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
public data class CSSStyleSheetHeader(
  @JsonProperty("styleSheetId")
  public val styleSheetId: String,
  @JsonProperty("frameId")
  public val frameId: String,
  @JsonProperty("sourceURL")
  public val sourceURL: String,
  @JsonProperty("sourceMapURL")
  @Optional
  public val sourceMapURL: String? = null,
  @JsonProperty("origin")
  public val origin: StyleSheetOrigin,
  @JsonProperty("title")
  public val title: String,
  @JsonProperty("ownerNode")
  @Optional
  public val ownerNode: Int? = null,
  @JsonProperty("disabled")
  public val disabled: Boolean,
  @JsonProperty("hasSourceURL")
  @Optional
  public val hasSourceURL: Boolean? = null,
  @JsonProperty("isInline")
  public val isInline: Boolean,
  @JsonProperty("isMutable")
  public val isMutable: Boolean,
  @JsonProperty("isConstructed")
  public val isConstructed: Boolean,
  @JsonProperty("startLine")
  public val startLine: Double,
  @JsonProperty("startColumn")
  public val startColumn: Double,
  @JsonProperty("length")
  public val length: Double,
  @JsonProperty("endLine")
  public val endLine: Double,
  @JsonProperty("endColumn")
  public val endColumn: Double,
)
