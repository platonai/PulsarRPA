@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.page

import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * return as stream
 */
public enum class PrintToPDFTransferMode {
  @JsonProperty("ReturnAsBase64")
  RETURN_AS_BASE_64,
  @JsonProperty("ReturnAsStream")
  RETURN_AS_STREAM,
}
