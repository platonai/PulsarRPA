package ai.platon.cdt.kt.protocol.commands

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import ai.platon.cdt.kt.protocol.support.annotations.ParamName
import ai.platon.cdt.kt.protocol.support.annotations.ReturnTypeParameter
import ai.platon.cdt.kt.protocol.support.annotations.Returns
import ai.platon.cdt.kt.protocol.types.cachestorage.Cache
import ai.platon.cdt.kt.protocol.types.cachestorage.CachedResponse
import ai.platon.cdt.kt.protocol.types.cachestorage.Header
import ai.platon.cdt.kt.protocol.types.cachestorage.RequestEntries
import kotlin.Int
import kotlin.String
import kotlin.collections.List

@Experimental
public interface CacheStorage {
  /**
   * Deletes a cache.
   * @param cacheId Id of cache for deletion.
   */
  public suspend fun deleteCache(@ParamName("cacheId") cacheId: String)

  /**
   * Deletes a cache entry.
   * @param cacheId Id of cache where the entry will be deleted.
   * @param request URL spec of the request.
   */
  public suspend fun deleteEntry(@ParamName("cacheId") cacheId: String, @ParamName("request")
      request: String)

  /**
   * Requests cache names.
   * @param securityOrigin Security origin.
   */
  @Returns("caches")
  @ReturnTypeParameter(Cache::class)
  public suspend fun requestCacheNames(@ParamName("securityOrigin") securityOrigin: String):
      List<Cache>

  /**
   * Fetches cache entry.
   * @param cacheId Id of cache that contains the entry.
   * @param requestURL URL spec of the request.
   * @param requestHeaders headers of the request.
   */
  @Returns("response")
  public suspend fun requestCachedResponse(
    @ParamName("cacheId") cacheId: String,
    @ParamName("requestURL") requestURL: String,
    @ParamName("requestHeaders") requestHeaders: List<Header>,
  ): CachedResponse

  /**
   * Requests data from cache.
   * @param cacheId ID of cache to get entries from.
   * @param skipCount Number of records to skip.
   * @param pageSize Number of records to fetch.
   * @param pathFilter If present, only return the entries containing this substring in the path
   */
  public suspend fun requestEntries(
    @ParamName("cacheId") cacheId: String,
    @ParamName("skipCount") @Optional skipCount: Int?,
    @ParamName("pageSize") @Optional pageSize: Int?,
    @ParamName("pathFilter") @Optional pathFilter: String?,
  ): RequestEntries

  public suspend fun requestEntries(@ParamName("cacheId") cacheId: String): RequestEntries {
    return requestEntries(cacheId, null, null, null)
  }
}
