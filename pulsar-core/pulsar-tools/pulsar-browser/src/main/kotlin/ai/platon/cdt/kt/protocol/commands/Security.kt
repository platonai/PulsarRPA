@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.commands

import ai.platon.cdt.kt.protocol.events.security.CertificateError
import ai.platon.cdt.kt.protocol.events.security.SecurityStateChanged
import ai.platon.cdt.kt.protocol.events.security.VisibleSecurityStateChanged
import ai.platon.cdt.kt.protocol.support.annotations.EventName
import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.ParamName
import ai.platon.cdt.kt.protocol.support.types.EventHandler
import ai.platon.cdt.kt.protocol.support.types.EventListener
import ai.platon.cdt.kt.protocol.types.security.CertificateErrorAction
import kotlin.Boolean
import kotlin.Deprecated
import kotlin.Int
import kotlin.Unit

/**
 * Security
 */
interface Security {
  /**
   * Disables tracking security state changes.
   */
  suspend fun disable()

  /**
   * Enables tracking security state changes.
   */
  suspend fun enable()

  /**
   * Enable/disable whether all certificate errors should be ignored.
   * @param ignore If true, all certificate errors will be ignored.
   */
  @Experimental
  suspend fun setIgnoreCertificateErrors(@ParamName("ignore") ignore: Boolean)

  /**
   * Handles a certificate error that fired a certificateError event.
   * @param eventId The ID of the event.
   * @param action The action to take on the certificate error.
   */
  @Deprecated("Deprecated by protocol")
  suspend fun handleCertificateError(@ParamName("eventId") eventId: Int, @ParamName("action") action: CertificateErrorAction)

  /**
   * Enable/disable overriding certificate errors. If enabled, all certificate error events need to
   * be handled by the DevTools client and should be answered with `handleCertificateError` commands.
   * @param override If true, certificate errors will be overridden.
   */
  @Deprecated("Deprecated by protocol")
  suspend fun setOverrideCertificateErrors(@ParamName("override") `override`: Boolean)

  @EventName("certificateError")
  @Deprecated("Deprecated by protocol")
  fun onCertificateError(eventListener: EventHandler<CertificateError>): EventListener

  @EventName("certificateError")
  @Deprecated("Deprecated by protocol")
  fun onCertificateError(eventListener: suspend (CertificateError) -> Unit): EventListener

  @EventName("visibleSecurityStateChanged")
  @Experimental
  fun onVisibleSecurityStateChanged(eventListener: EventHandler<VisibleSecurityStateChanged>): EventListener

  @EventName("visibleSecurityStateChanged")
  @Experimental
  fun onVisibleSecurityStateChanged(eventListener: suspend (VisibleSecurityStateChanged) -> Unit): EventListener

  @EventName("securityStateChanged")
  fun onSecurityStateChanged(eventListener: EventHandler<SecurityStateChanged>): EventListener

  @EventName("securityStateChanged")
  fun onSecurityStateChanged(eventListener: suspend (SecurityStateChanged) -> Unit): EventListener
}
