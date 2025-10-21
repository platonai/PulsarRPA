package ai.platon.cdt.kt.protocol.types.network

import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * The reason why request was blocked.
 */
public enum class CorsError {
  @JsonProperty("DisallowedByMode")
  DISALLOWED_BY_MODE,
  @JsonProperty("InvalidResponse")
  INVALID_RESPONSE,
  @JsonProperty("WildcardOriginNotAllowed")
  WILDCARD_ORIGIN_NOT_ALLOWED,
  @JsonProperty("MissingAllowOriginHeader")
  MISSING_ALLOW_ORIGIN_HEADER,
  @JsonProperty("MultipleAllowOriginValues")
  MULTIPLE_ALLOW_ORIGIN_VALUES,
  @JsonProperty("InvalidAllowOriginValue")
  INVALID_ALLOW_ORIGIN_VALUE,
  @JsonProperty("AllowOriginMismatch")
  ALLOW_ORIGIN_MISMATCH,
  @JsonProperty("InvalidAllowCredentials")
  INVALID_ALLOW_CREDENTIALS,
  @JsonProperty("CorsDisabledScheme")
  CORS_DISABLED_SCHEME,
  @JsonProperty("PreflightInvalidStatus")
  PREFLIGHT_INVALID_STATUS,
  @JsonProperty("PreflightDisallowedRedirect")
  PREFLIGHT_DISALLOWED_REDIRECT,
  @JsonProperty("PreflightWildcardOriginNotAllowed")
  PREFLIGHT_WILDCARD_ORIGIN_NOT_ALLOWED,
  @JsonProperty("PreflightMissingAllowOriginHeader")
  PREFLIGHT_MISSING_ALLOW_ORIGIN_HEADER,
  @JsonProperty("PreflightMultipleAllowOriginValues")
  PREFLIGHT_MULTIPLE_ALLOW_ORIGIN_VALUES,
  @JsonProperty("PreflightInvalidAllowOriginValue")
  PREFLIGHT_INVALID_ALLOW_ORIGIN_VALUE,
  @JsonProperty("PreflightAllowOriginMismatch")
  PREFLIGHT_ALLOW_ORIGIN_MISMATCH,
  @JsonProperty("PreflightInvalidAllowCredentials")
  PREFLIGHT_INVALID_ALLOW_CREDENTIALS,
  @JsonProperty("PreflightMissingAllowExternal")
  PREFLIGHT_MISSING_ALLOW_EXTERNAL,
  @JsonProperty("PreflightInvalidAllowExternal")
  PREFLIGHT_INVALID_ALLOW_EXTERNAL,
  @JsonProperty("InvalidAllowMethodsPreflightResponse")
  INVALID_ALLOW_METHODS_PREFLIGHT_RESPONSE,
  @JsonProperty("InvalidAllowHeadersPreflightResponse")
  INVALID_ALLOW_HEADERS_PREFLIGHT_RESPONSE,
  @JsonProperty("MethodDisallowedByPreflightResponse")
  METHOD_DISALLOWED_BY_PREFLIGHT_RESPONSE,
  @JsonProperty("HeaderDisallowedByPreflightResponse")
  HEADER_DISALLOWED_BY_PREFLIGHT_RESPONSE,
  @JsonProperty("RedirectContainsCredentials")
  REDIRECT_CONTAINS_CREDENTIALS,
  @JsonProperty("InsecurePrivateNetwork")
  INSECURE_PRIVATE_NETWORK,
}
