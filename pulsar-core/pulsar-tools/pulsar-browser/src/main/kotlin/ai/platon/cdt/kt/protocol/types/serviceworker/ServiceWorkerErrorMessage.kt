@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.serviceworker

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int
import kotlin.String

/**
 * ServiceWorker error message.
 */
data class ServiceWorkerErrorMessage(
  @param:JsonProperty("errorMessage")
  val errorMessage: String,
  @param:JsonProperty("registrationId")
  val registrationId: String,
  @param:JsonProperty("versionId")
  val versionId: String,
  @param:JsonProperty("sourceURL")
  val sourceURL: String,
  @param:JsonProperty("lineNumber")
  val lineNumber: Int,
  @param:JsonProperty("columnNumber")
  val columnNumber: Int,
)
