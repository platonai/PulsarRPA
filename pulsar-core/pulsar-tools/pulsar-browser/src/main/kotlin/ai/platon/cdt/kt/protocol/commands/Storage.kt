package ai.platon.cdt.kt.protocol.commands

import ai.platon.cdt.kt.protocol.events.storage.CacheStorageContentUpdated
import ai.platon.cdt.kt.protocol.events.storage.CacheStorageListUpdated
import ai.platon.cdt.kt.protocol.events.storage.IndexedDBContentUpdated
import ai.platon.cdt.kt.protocol.events.storage.IndexedDBListUpdated
import ai.platon.cdt.kt.protocol.support.annotations.EventName
import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import ai.platon.cdt.kt.protocol.support.annotations.ParamName
import ai.platon.cdt.kt.protocol.support.annotations.ReturnTypeParameter
import ai.platon.cdt.kt.protocol.support.annotations.Returns
import ai.platon.cdt.kt.protocol.support.types.EventHandler
import ai.platon.cdt.kt.protocol.support.types.EventListener
import ai.platon.cdt.kt.protocol.types.network.Cookie
import ai.platon.cdt.kt.protocol.types.network.CookieParam
import ai.platon.cdt.kt.protocol.types.storage.TrustTokens
import ai.platon.cdt.kt.protocol.types.storage.UsageAndQuota
import kotlin.Boolean
import kotlin.Double
import kotlin.String
import kotlin.Unit
import kotlin.collections.List

@Experimental
public interface Storage {
  /**
   * Clears storage for origin.
   * @param origin Security origin.
   * @param storageTypes Comma separated list of StorageType to clear.
   */
  public suspend fun clearDataForOrigin(@ParamName("origin") origin: String,
      @ParamName("storageTypes") storageTypes: String)

  /**
   * Returns all browser cookies.
   * @param browserContextId Browser context to use when called on the browser endpoint.
   */
  @Returns("cookies")
  @ReturnTypeParameter(Cookie::class)
  public suspend fun getCookies(@ParamName("browserContextId") @Optional browserContextId: String?):
      List<Cookie>

  @Returns("cookies")
  @ReturnTypeParameter(Cookie::class)
  public suspend fun getCookies(): List<Cookie> {
    return getCookies(null)
  }

  /**
   * Sets given cookies.
   * @param cookies Cookies to be set.
   * @param browserContextId Browser context to use when called on the browser endpoint.
   */
  public suspend fun setCookies(@ParamName("cookies") cookies: List<CookieParam>,
      @ParamName("browserContextId") @Optional browserContextId: String?)

  public suspend fun setCookies(@ParamName("cookies") cookies: List<CookieParam>) {
    return setCookies(cookies, null)
  }

  /**
   * Clears cookies.
   * @param browserContextId Browser context to use when called on the browser endpoint.
   */
  public suspend fun clearCookies(@ParamName("browserContextId") @Optional
      browserContextId: String?)

  public suspend fun clearCookies() {
    return clearCookies(null)
  }

  /**
   * Returns usage and quota in bytes.
   * @param origin Security origin.
   */
  public suspend fun getUsageAndQuota(@ParamName("origin") origin: String): UsageAndQuota

  /**
   * Override quota for the specified origin
   * @param origin Security origin.
   * @param quotaSize The quota size (in bytes) to override the original quota with.
   * If this is called multiple times, the overridden quota will be equal to
   * the quotaSize provided in the final call. If this is called without
   * specifying a quotaSize, the quota will be reset to the default value for
   * the specified origin. If this is called multiple times with different
   * origins, the override will be maintained for each origin until it is
   * disabled (called without a quotaSize).
   */
  @Experimental
  public suspend fun overrideQuotaForOrigin(@ParamName("origin") origin: String,
      @ParamName("quotaSize") @Optional quotaSize: Double?)

  @Experimental
  public suspend fun overrideQuotaForOrigin(@ParamName("origin") origin: String) {
    return overrideQuotaForOrigin(origin, null)
  }

  /**
   * Registers origin to be notified when an update occurs to its cache storage list.
   * @param origin Security origin.
   */
  public suspend fun trackCacheStorageForOrigin(@ParamName("origin") origin: String)

  /**
   * Registers origin to be notified when an update occurs to its IndexedDB.
   * @param origin Security origin.
   */
  public suspend fun trackIndexedDBForOrigin(@ParamName("origin") origin: String)

  /**
   * Unregisters origin from receiving notifications for cache storage.
   * @param origin Security origin.
   */
  public suspend fun untrackCacheStorageForOrigin(@ParamName("origin") origin: String)

  /**
   * Unregisters origin from receiving notifications for IndexedDB.
   * @param origin Security origin.
   */
  public suspend fun untrackIndexedDBForOrigin(@ParamName("origin") origin: String)

  /**
   * Returns the number of stored Trust Tokens per issuer for the
   * current browsing context.
   */
  @Experimental
  @Returns("tokens")
  @ReturnTypeParameter(TrustTokens::class)
  public suspend fun getTrustTokens(): List<TrustTokens>

  /**
   * Removes all Trust Tokens issued by the provided issuerOrigin.
   * Leaves other stored data, including the issuer's Redemption Records, intact.
   * @param issuerOrigin
   */
  @Experimental
  @Returns("didDeleteTokens")
  public suspend fun clearTrustTokens(@ParamName("issuerOrigin") issuerOrigin: String): Boolean

  @EventName("cacheStorageContentUpdated")
  public fun onCacheStorageContentUpdated(eventListener: EventHandler<CacheStorageContentUpdated>):
      EventListener

  @EventName("cacheStorageContentUpdated")
  public
      fun onCacheStorageContentUpdated(eventListener: suspend (CacheStorageContentUpdated) -> Unit):
      EventListener

  @EventName("cacheStorageListUpdated")
  public fun onCacheStorageListUpdated(eventListener: EventHandler<CacheStorageListUpdated>):
      EventListener

  @EventName("cacheStorageListUpdated")
  public fun onCacheStorageListUpdated(eventListener: suspend (CacheStorageListUpdated) -> Unit):
      EventListener

  @EventName("indexedDBContentUpdated")
  public fun onIndexedDBContentUpdated(eventListener: EventHandler<IndexedDBContentUpdated>):
      EventListener

  @EventName("indexedDBContentUpdated")
  public fun onIndexedDBContentUpdated(eventListener: suspend (IndexedDBContentUpdated) -> Unit):
      EventListener

  @EventName("indexedDBListUpdated")
  public fun onIndexedDBListUpdated(eventListener: EventHandler<IndexedDBListUpdated>):
      EventListener

  @EventName("indexedDBListUpdated")
  public fun onIndexedDBListUpdated(eventListener: suspend (IndexedDBListUpdated) -> Unit):
      EventListener
}
