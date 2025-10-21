package ai.platon.cdt.kt.protocol.types.network

import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Network level fetch failure reason.
 */
public enum class ErrorReason {
  @JsonProperty("Failed")
  FAILED,
  @JsonProperty("Aborted")
  ABORTED,
  @JsonProperty("TimedOut")
  TIMED_OUT,
  @JsonProperty("AccessDenied")
  ACCESS_DENIED,
  @JsonProperty("ConnectionClosed")
  CONNECTION_CLOSED,
  @JsonProperty("ConnectionReset")
  CONNECTION_RESET,
  @JsonProperty("ConnectionRefused")
  CONNECTION_REFUSED,
  @JsonProperty("ConnectionAborted")
  CONNECTION_ABORTED,
  @JsonProperty("ConnectionFailed")
  CONNECTION_FAILED,
  @JsonProperty("NameNotResolved")
  NAME_NOT_RESOLVED,
  @JsonProperty("InternetDisconnected")
  INTERNET_DISCONNECTED,
  @JsonProperty("AddressUnreachable")
  ADDRESS_UNREACHABLE,
  @JsonProperty("BlockedByClient")
  BLOCKED_BY_CLIENT,
  @JsonProperty("BlockedByResponse")
  BLOCKED_BY_RESPONSE,
}
