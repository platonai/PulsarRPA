package ai.platon.cdt.kt.protocol.events.debugger

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import ai.platon.cdt.kt.protocol.types.debugger.ScriptLanguage
import ai.platon.cdt.kt.protocol.types.runtime.StackTrace
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Any
import kotlin.Boolean
import kotlin.Int
import kotlin.String
import kotlin.collections.Map

/**
 * Fired when virtual machine fails to parse the script.
 */
public data class ScriptFailedToParse(
  @JsonProperty("scriptId")
  public val scriptId: String,
  @JsonProperty("url")
  public val url: String,
  @JsonProperty("startLine")
  public val startLine: Int,
  @JsonProperty("startColumn")
  public val startColumn: Int,
  @JsonProperty("endLine")
  public val endLine: Int,
  @JsonProperty("endColumn")
  public val endColumn: Int,
  @JsonProperty("executionContextId")
  public val executionContextId: Int,
  @JsonProperty("hash")
  public val hash: String,
  @JsonProperty("executionContextAuxData")
  @Optional
  public val executionContextAuxData: Map<String, Any?>? = null,
  @JsonProperty("sourceMapURL")
  @Optional
  public val sourceMapURL: String? = null,
  @JsonProperty("hasSourceURL")
  @Optional
  public val hasSourceURL: Boolean? = null,
  @JsonProperty("isModule")
  @Optional
  public val isModule: Boolean? = null,
  @JsonProperty("length")
  @Optional
  public val length: Int? = null,
  @JsonProperty("stackTrace")
  @Optional
  @Experimental
  public val stackTrace: StackTrace? = null,
  @JsonProperty("codeOffset")
  @Optional
  @Experimental
  public val codeOffset: Int? = null,
  @JsonProperty("scriptLanguage")
  @Optional
  @Experimental
  public val scriptLanguage: ScriptLanguage? = null,
  @JsonProperty("embedderName")
  @Optional
  @Experimental
  public val embedderName: String? = null,
)
