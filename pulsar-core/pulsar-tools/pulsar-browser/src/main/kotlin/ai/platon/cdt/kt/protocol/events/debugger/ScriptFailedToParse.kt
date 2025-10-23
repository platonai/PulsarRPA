@file:Suppress("unused")
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
data class ScriptFailedToParse(
  @param:JsonProperty("scriptId")
  val scriptId: String,
  @param:JsonProperty("url")
  val url: String,
  @param:JsonProperty("startLine")
  val startLine: Int,
  @param:JsonProperty("startColumn")
  val startColumn: Int,
  @param:JsonProperty("endLine")
  val endLine: Int,
  @param:JsonProperty("endColumn")
  val endColumn: Int,
  @param:JsonProperty("executionContextId")
  val executionContextId: Int,
  @param:JsonProperty("hash")
  val hash: String,
  @param:JsonProperty("executionContextAuxData")
  @param:Optional
  val executionContextAuxData: Map<String, Any?>? = null,
  @param:JsonProperty("sourceMapURL")
  @param:Optional
  val sourceMapURL: String? = null,
  @param:JsonProperty("hasSourceURL")
  @param:Optional
  val hasSourceURL: Boolean? = null,
  @param:JsonProperty("isModule")
  @param:Optional
  val isModule: Boolean? = null,
  @param:JsonProperty("length")
  @param:Optional
  val length: Int? = null,
  @param:JsonProperty("stackTrace")
  @param:Optional
  @param:Experimental
  val stackTrace: StackTrace? = null,
  @param:JsonProperty("codeOffset")
  @param:Optional
  @param:Experimental
  val codeOffset: Int? = null,
  @param:JsonProperty("scriptLanguage")
  @param:Optional
  @param:Experimental
  val scriptLanguage: ScriptLanguage? = null,
  @param:JsonProperty("embedderName")
  @param:Optional
  @param:Experimental
  val embedderName: String? = null,
)
