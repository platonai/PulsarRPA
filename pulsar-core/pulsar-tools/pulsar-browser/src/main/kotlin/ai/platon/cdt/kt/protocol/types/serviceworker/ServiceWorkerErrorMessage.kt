package ai.platon.cdt.kt.protocol.types.serviceworker

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int
import kotlin.String

/**
 * ServiceWorker error message.
 */
public data class ServiceWorkerErrorMessage(
  @JsonProperty("errorMessage")
  public val errorMessage: String,
  @JsonProperty("registrationId")
  public val registrationId: String,
  @JsonProperty("versionId")
  public val versionId: String,
  @JsonProperty("sourceURL")
  public val sourceURL: String,
  @JsonProperty("lineNumber")
  public val lineNumber: Int,
  @JsonProperty("columnNumber")
  public val columnNumber: Int,
)
