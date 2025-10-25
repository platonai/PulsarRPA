@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.events.cast

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

/**
 * This is fired whenever the outstanding issue/error message changes.
 * |issueMessage| is empty if there is no issue.
 */
data class IssueUpdated(
  @param:JsonProperty("issueMessage")
  val issueMessage: String,
)
