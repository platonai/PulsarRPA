package com.github.kklisura.cdt.protocol.v2023.commands;

/*-
 * #%L
 * cdt-java-client
 * %%
 * Copyright (C) 2018 - 2023 Kenan Klisura
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.github.kklisura.cdt.protocol.v2023.events.storage.*;
import com.github.kklisura.cdt.protocol.v2023.support.annotations.*;
import com.github.kklisura.cdt.protocol.v2023.support.types.EventHandler;
import com.github.kklisura.cdt.protocol.v2023.support.types.EventListener;
import com.github.kklisura.cdt.protocol.v2023.types.network.Cookie;
import com.github.kklisura.cdt.protocol.v2023.types.network.CookieParam;
import com.github.kklisura.cdt.protocol.v2023.types.storage.*;

import java.util.List;

@Experimental
public interface Storage {

  /**
   * Returns a storage key given a frame id.
   *
   * @param frameId
   */
  @Returns("storageKey")
  String getStorageKeyForFrame(@ParamName("frameId") String frameId);

  /**
   * Clears storage for origin.
   *
   * @param origin Security origin.
   * @param storageTypes Comma separated list of StorageType to clear.
   */
  void clearDataForOrigin(
      @ParamName("origin") String origin, @ParamName("storageTypes") String storageTypes);

  /**
   * Clears storage for storage key.
   *
   * @param storageKey Storage key.
   * @param storageTypes Comma separated list of StorageType to clear.
   */
  void clearDataForStorageKey(
      @ParamName("storageKey") String storageKey, @ParamName("storageTypes") String storageTypes);

  /** Returns all browser cookies. */
  @Returns("cookies")
  @ReturnTypeParameter(Cookie.class)
  List<Cookie> getCookies();

  /**
   * Returns all browser cookies.
   *
   * @param browserContextId Browser context to use when called on the browser endpoint.
   */
  @Returns("cookies")
  @ReturnTypeParameter(Cookie.class)
  List<Cookie> getCookies(@Optional @ParamName("browserContextId") String browserContextId);

  /**
   * Sets given cookies.
   *
   * @param cookies Cookies to be set.
   */
  void setCookies(@ParamName("cookies") List<CookieParam> cookies);

  /**
   * Sets given cookies.
   *
   * @param cookies Cookies to be set.
   * @param browserContextId Browser context to use when called on the browser endpoint.
   */
  void setCookies(
      @ParamName("cookies") List<CookieParam> cookies,
      @Optional @ParamName("browserContextId") String browserContextId);

  /** Clears cookies. */
  void clearCookies();

  /**
   * Clears cookies.
   *
   * @param browserContextId Browser context to use when called on the browser endpoint.
   */
  void clearCookies(@Optional @ParamName("browserContextId") String browserContextId);

  /**
   * Returns usage and quota in bytes.
   *
   * @param origin Security origin.
   */
  UsageAndQuota getUsageAndQuota(@ParamName("origin") String origin);

  /**
   * Override quota for the specified origin
   *
   * @param origin Security origin.
   */
  @Experimental
  void overrideQuotaForOrigin(@ParamName("origin") String origin);

  /**
   * Override quota for the specified origin
   *
   * @param origin Security origin.
   * @param quotaSize The quota size (in bytes) to override the original quota with. If this is
   *     called multiple times, the overridden quota will be equal to the quotaSize provided in the
   *     final call. If this is called without specifying a quotaSize, the quota will be reset to
   *     the default value for the specified origin. If this is called multiple times with different
   *     origins, the override will be maintained for each origin until it is disabled (called
   *     without a quotaSize).
   */
  @Experimental
  void overrideQuotaForOrigin(
      @ParamName("origin") String origin, @Optional @ParamName("quotaSize") Double quotaSize);

  /**
   * Registers origin to be notified when an update occurs to its cache storage list.
   *
   * @param origin Security origin.
   */
  void trackCacheStorageForOrigin(@ParamName("origin") String origin);

  /**
   * Registers storage key to be notified when an update occurs to its cache storage list.
   *
   * @param storageKey Storage key.
   */
  void trackCacheStorageForStorageKey(@ParamName("storageKey") String storageKey);

  /**
   * Registers origin to be notified when an update occurs to its IndexedDB.
   *
   * @param origin Security origin.
   */
  void trackIndexedDBForOrigin(@ParamName("origin") String origin);

  /**
   * Registers storage key to be notified when an update occurs to its IndexedDB.
   *
   * @param storageKey Storage key.
   */
  void trackIndexedDBForStorageKey(@ParamName("storageKey") String storageKey);

  /**
   * Unregisters origin from receiving notifications for cache storage.
   *
   * @param origin Security origin.
   */
  void untrackCacheStorageForOrigin(@ParamName("origin") String origin);

  /**
   * Unregisters storage key from receiving notifications for cache storage.
   *
   * @param storageKey Storage key.
   */
  void untrackCacheStorageForStorageKey(@ParamName("storageKey") String storageKey);

  /**
   * Unregisters origin from receiving notifications for IndexedDB.
   *
   * @param origin Security origin.
   */
  void untrackIndexedDBForOrigin(@ParamName("origin") String origin);

  /**
   * Unregisters storage key from receiving notifications for IndexedDB.
   *
   * @param storageKey Storage key.
   */
  void untrackIndexedDBForStorageKey(@ParamName("storageKey") String storageKey);

  /** Returns the number of stored Trust Tokens per issuer for the current browsing context. */
  @Experimental
  @Returns("tokens")
  @ReturnTypeParameter(TrustTokens.class)
  List<TrustTokens> getTrustTokens();

  /**
   * Removes all Trust Tokens issued by the provided issuerOrigin. Leaves other stored data,
   * including the issuer's Redemption Records, intact.
   *
   * @param issuerOrigin
   */
  @Experimental
  @Returns("didDeleteTokens")
  Boolean clearTrustTokens(@ParamName("issuerOrigin") String issuerOrigin);

  /**
   * Gets details for a named interest group.
   *
   * @param ownerOrigin
   * @param name
   */
  @Experimental
  @Returns("details")
  InterestGroupDetails getInterestGroupDetails(
      @ParamName("ownerOrigin") String ownerOrigin, @ParamName("name") String name);

  /**
   * Enables/Disables issuing of interestGroupAccessed events.
   *
   * @param enable
   */
  @Experimental
  void setInterestGroupTracking(@ParamName("enable") Boolean enable);

  /**
   * Gets metadata for an origin's shared storage.
   *
   * @param ownerOrigin
   */
  @Experimental
  @Returns("metadata")
  SharedStorageMetadata getSharedStorageMetadata(@ParamName("ownerOrigin") String ownerOrigin);

  /**
   * Gets the entries in an given origin's shared storage.
   *
   * @param ownerOrigin
   */
  @Experimental
  @Returns("entries")
  @ReturnTypeParameter(SharedStorageEntry.class)
  List<SharedStorageEntry> getSharedStorageEntries(@ParamName("ownerOrigin") String ownerOrigin);

  /**
   * Sets entry with `key` and `value` for a given origin's shared storage.
   *
   * @param ownerOrigin
   * @param key
   * @param value
   */
  @Experimental
  void setSharedStorageEntry(
      @ParamName("ownerOrigin") String ownerOrigin,
      @ParamName("key") String key,
      @ParamName("value") String value);

  /**
   * Sets entry with `key` and `value` for a given origin's shared storage.
   *
   * @param ownerOrigin
   * @param key
   * @param value
   * @param ignoreIfPresent If `ignoreIfPresent` is included and true, then only sets the entry if
   *     `key` doesn't already exist.
   */
  @Experimental
  void setSharedStorageEntry(
      @ParamName("ownerOrigin") String ownerOrigin,
      @ParamName("key") String key,
      @ParamName("value") String value,
      @Optional @ParamName("ignoreIfPresent") Boolean ignoreIfPresent);

  /**
   * Deletes entry for `key` (if it exists) for a given origin's shared storage.
   *
   * @param ownerOrigin
   * @param key
   */
  @Experimental
  void deleteSharedStorageEntry(
      @ParamName("ownerOrigin") String ownerOrigin, @ParamName("key") String key);

  /**
   * Clears all entries for a given origin's shared storage.
   *
   * @param ownerOrigin
   */
  @Experimental
  void clearSharedStorageEntries(@ParamName("ownerOrigin") String ownerOrigin);

  /**
   * Resets the budget for `ownerOrigin` by clearing all budget withdrawals.
   *
   * @param ownerOrigin
   */
  @Experimental
  void resetSharedStorageBudget(@ParamName("ownerOrigin") String ownerOrigin);

  /**
   * Enables/disables issuing of sharedStorageAccessed events.
   *
   * @param enable
   */
  @Experimental
  void setSharedStorageTracking(@ParamName("enable") Boolean enable);

  /**
   * Set tracking for a storage key's buckets.
   *
   * @param storageKey
   * @param enable
   */
  @Experimental
  void setStorageBucketTracking(
      @ParamName("storageKey") String storageKey, @ParamName("enable") Boolean enable);

  /**
   * Deletes the Storage Bucket with the given storage key and bucket name.
   *
   * @param bucket
   */
  @Experimental
  void deleteStorageBucket(@ParamName("bucket") StorageBucket bucket);

  /** Deletes state for sites identified as potential bounce trackers, immediately. */
  @Experimental
  @Returns("deletedSites")
  @ReturnTypeParameter(String.class)
  List<String> runBounceTrackingMitigations();

  /**
   * https://wicg.github.io/attribution-reporting-api/
   *
   * @param enabled If enabled, noise is suppressed and reports are sent immediately.
   */
  @Experimental
  void setAttributionReportingLocalTestingMode(@ParamName("enabled") Boolean enabled);

  /**
   * Enables/disables issuing of Attribution Reporting events.
   *
   * @param enable
   */
  @Experimental
  void setAttributionReportingTracking(@ParamName("enable") Boolean enable);

  /** A cache's contents have been modified. */
  @EventName("cacheStorageContentUpdated")
  EventListener onCacheStorageContentUpdated(
      EventHandler<CacheStorageContentUpdated> eventListener);

  /** A cache has been added/deleted. */
  @EventName("cacheStorageListUpdated")
  EventListener onCacheStorageListUpdated(EventHandler<CacheStorageListUpdated> eventListener);

  /** The origin's IndexedDB object store has been modified. */
  @EventName("indexedDBContentUpdated")
  EventListener onIndexedDBContentUpdated(EventHandler<IndexedDBContentUpdated> eventListener);

  /** The origin's IndexedDB database list has been modified. */
  @EventName("indexedDBListUpdated")
  EventListener onIndexedDBListUpdated(EventHandler<IndexedDBListUpdated> eventListener);

  /** One of the interest groups was accessed by the associated page. */
  @EventName("interestGroupAccessed")
  EventListener onInterestGroupAccessed(EventHandler<InterestGroupAccessed> eventListener);

  /**
   * Shared storage was accessed by the associated page. The following parameters are included in
   * all events.
   */
  @EventName("sharedStorageAccessed")
  EventListener onSharedStorageAccessed(EventHandler<SharedStorageAccessed> eventListener);

  @EventName("storageBucketCreatedOrUpdated")
  EventListener onStorageBucketCreatedOrUpdated(
      EventHandler<StorageBucketCreatedOrUpdated> eventListener);

  @EventName("storageBucketDeleted")
  EventListener onStorageBucketDeleted(EventHandler<StorageBucketDeleted> eventListener);

  /** TODO(crbug.com/1458532): Add other Attribution Reporting events, e.g. trigger registration. */
  @EventName("attributionReportingSourceRegistered")
  @Experimental
  EventListener onAttributionReportingSourceRegistered(
      EventHandler<AttributionReportingSourceRegistered> eventListener);
}
