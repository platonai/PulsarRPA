package ai.platon.cdt.kt.protocol.events.security

import com.fasterxml.jackson.`annotation`.JsonProperty
import java.lang.Deprecated
import kotlin.Int
import kotlin.String

/**
 * There is a certificate error. If overriding certificate errors is enabled, then it should be
 * handled with the `handleCertificateError` command. Note: this event does not fire if the
 * certificate error has been allowed internally. Only one client per target should override
 * certificate errors at the same time.
 */
@Deprecated
public data class CertificateError(
  @JsonProperty("eventId")
  public val eventId: Int,
  @JsonProperty("errorType")
  public val errorType: String,
  @JsonProperty("requestURL")
  public val requestURL: String,
)
