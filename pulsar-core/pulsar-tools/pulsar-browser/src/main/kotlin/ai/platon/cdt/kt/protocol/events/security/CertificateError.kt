@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.events.security

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Deprecated
import kotlin.Int
import kotlin.String

/**
 * There is a certificate error. If overriding certificate errors is enabled, then it should be
 * handled with the `handleCertificateError` command. Note: this event does not fire if the
 * certificate error has been allowed internally. Only one client per target should override
 * certificate errors at the same time.
 */
@Deprecated("Deprecated")
data class CertificateError(
  @param:JsonProperty("eventId")
  val eventId: Int,
  @param:JsonProperty("errorType")
  val errorType: String,
  @param:JsonProperty("requestURL")
  val requestURL: String,
)
