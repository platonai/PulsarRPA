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
import java.lang.Deprecated
import kotlin.Boolean
import kotlin.Int
import kotlin.Unit

/**
 * Security
 */
public interface Security {
  /**
   * Disables tracking security state changes.
   */
  public suspend fun disable()

  /**
   * Enables tracking security state changes.
   */
  public suspend fun enable()

  /**
   * Enable/disable whether all certificate errors should be ignored.
   * @param ignore If true, all certificate errors will be ignored.
   */
  @Experimental
  public suspend fun setIgnoreCertificateErrors(@ParamName("ignore") ignore: Boolean)

  /**
   * Handles a certificate error that fired a certificateError event.
   * @param eventId The ID of the event.
   * @param action The action to take on the certificate error.
   */
  @Deprecated
  public suspend fun handleCertificateError(@ParamName("eventId") eventId: Int, @ParamName("action")
      action: CertificateErrorAction)

  /**
   * Enable/disable overriding certificate errors. If enabled, all certificate error events need to
   * be handled by the DevTools client and should be answered with `handleCertificateError`
   * commands.
   * @param override If true, certificate errors will be overridden.
   */
  @Deprecated
  public suspend fun setOverrideCertificateErrors(@ParamName("override") `override`: Boolean)

  @EventName("certificateError")
  @Deprecated
  public fun onCertificateError(eventListener: EventHandler<CertificateError>): EventListener

  @EventName("certificateError")
  @Deprecated
  public fun onCertificateError(eventListener: suspend (CertificateError) -> Unit): EventListener

  @EventName("visibleSecurityStateChanged")
  @Experimental
  public
      fun onVisibleSecurityStateChanged(eventListener: EventHandler<VisibleSecurityStateChanged>):
      EventListener

  @EventName("visibleSecurityStateChanged")
  @Experimental
  public
      fun onVisibleSecurityStateChanged(eventListener: suspend (VisibleSecurityStateChanged) -> Unit):
      EventListener

  @EventName("securityStateChanged")
  public fun onSecurityStateChanged(eventListener: EventHandler<SecurityStateChanged>):
      EventListener

  @EventName("securityStateChanged")
  public fun onSecurityStateChanged(eventListener: suspend (SecurityStateChanged) -> Unit):
      EventListener
}
