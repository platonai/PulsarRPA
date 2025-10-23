@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.debugger

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

data class ScriptSource(
  @param:JsonProperty("scriptSource")
  val scriptSource: String,
  @param:JsonProperty("bytecode")
  @param:Optional
  val bytecode: String? = null,
)
