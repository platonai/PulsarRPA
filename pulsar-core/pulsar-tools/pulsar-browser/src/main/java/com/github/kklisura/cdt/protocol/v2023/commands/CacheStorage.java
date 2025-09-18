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

import com.github.kklisura.cdt.protocol.v2023.support.annotations.*;
import com.github.kklisura.cdt.protocol.v2023.types.cachestorage.Cache;
import com.github.kklisura.cdt.protocol.v2023.types.cachestorage.CachedResponse;
import com.github.kklisura.cdt.protocol.v2023.types.cachestorage.Header;
import com.github.kklisura.cdt.protocol.v2023.types.cachestorage.RequestEntries;
import com.github.kklisura.cdt.protocol.v2023.types.storage.StorageBucket;

import java.util.List;

@Experimental
public interface CacheStorage {

  /**
   * Deletes a cache.
   *
   * @param cacheId Id of cache for deletion.
   */
  void deleteCache(@ParamName("cacheId") String cacheId);

  /**
   * Deletes a cache entry.
   *
   * @param cacheId Id of cache where the entry will be deleted.
   * @param request URL spec of the request.
   */
  void deleteEntry(@ParamName("cacheId") String cacheId, @ParamName("request") String request);

  /** Requests cache names. */
  @Returns("caches")
  @ReturnTypeParameter(Cache.class)
  List<Cache> requestCacheNames();

  /**
   * Requests cache names.
   *
   * @param securityOrigin At least and at most one of securityOrigin, storageKey, storageBucket
   *     must be specified. Security origin.
   * @param storageKey Storage key.
   * @param storageBucket Storage bucket. If not specified, it uses the default bucket.
   */
  @Returns("caches")
  @ReturnTypeParameter(Cache.class)
  List<Cache> requestCacheNames(
      @Optional @ParamName("securityOrigin") String securityOrigin,
      @Optional @ParamName("storageKey") String storageKey,
      @Optional @ParamName("storageBucket") StorageBucket storageBucket);

  /**
   * Fetches cache entry.
   *
   * @param cacheId Id of cache that contains the entry.
   * @param requestURL URL spec of the request.
   * @param requestHeaders headers of the request.
   */
  @Returns("response")
  CachedResponse requestCachedResponse(
      @ParamName("cacheId") String cacheId,
      @ParamName("requestURL") String requestURL,
      @ParamName("requestHeaders") List<Header> requestHeaders);

  /**
   * Requests data from cache.
   *
   * @param cacheId ID of cache to get entries from.
   */
  RequestEntries requestEntries(@ParamName("cacheId") String cacheId);

  /**
   * Requests data from cache.
   *
   * @param cacheId ID of cache to get entries from.
   * @param skipCount Number of records to skip.
   * @param pageSize Number of records to fetch.
   * @param pathFilter If present, only return the entries containing this substring in the path
   */
  RequestEntries requestEntries(
      @ParamName("cacheId") String cacheId,
      @Optional @ParamName("skipCount") Integer skipCount,
      @Optional @ParamName("pageSize") Integer pageSize,
      @Optional @ParamName("pathFilter") String pathFilter);
}
