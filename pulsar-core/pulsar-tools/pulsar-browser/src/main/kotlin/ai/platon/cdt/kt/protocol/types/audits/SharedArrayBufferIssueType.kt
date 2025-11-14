@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.audits

import com.fasterxml.jackson.`annotation`.JsonProperty

public enum class SharedArrayBufferIssueType {
  @JsonProperty("TransferIssue")
  TRANSFER_ISSUE,
  @JsonProperty("CreationIssue")
  CREATION_ISSUE,
}
