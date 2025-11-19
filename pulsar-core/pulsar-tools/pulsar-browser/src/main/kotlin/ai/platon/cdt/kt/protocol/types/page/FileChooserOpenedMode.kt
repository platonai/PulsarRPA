@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.page

import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Input mode.
 */
public enum class FileChooserOpenedMode {
  @JsonProperty("selectSingle")
  SELECT_SINGLE,
  @JsonProperty("selectMultiple")
  SELECT_MULTIPLE,
}
