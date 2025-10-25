@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.network

import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Detailed success or error status of the operation.
 * 'AlreadyExists' also signifies a successful operation, as the result
 * of the operation already exists und thus, the operation was abort
 * preemptively (e.g. a cache hit).
 */
public enum class TrustTokenOperationDoneStatus {
  @JsonProperty("Ok")
  OK,
  @JsonProperty("InvalidArgument")
  INVALID_ARGUMENT,
  @JsonProperty("FailedPrecondition")
  FAILED_PRECONDITION,
  @JsonProperty("ResourceExhausted")
  RESOURCE_EXHAUSTED,
  @JsonProperty("AlreadyExists")
  ALREADY_EXISTS,
  @JsonProperty("Unavailable")
  UNAVAILABLE,
  @JsonProperty("BadResponse")
  BAD_RESPONSE,
  @JsonProperty("InternalError")
  INTERNAL_ERROR,
  @JsonProperty("UnknownError")
  UNKNOWN_ERROR,
  @JsonProperty("FulfilledLocally")
  FULFILLED_LOCALLY,
}
