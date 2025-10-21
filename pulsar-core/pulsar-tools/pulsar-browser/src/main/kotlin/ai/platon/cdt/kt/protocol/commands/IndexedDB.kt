package ai.platon.cdt.kt.protocol.commands

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import ai.platon.cdt.kt.protocol.support.annotations.ParamName
import ai.platon.cdt.kt.protocol.support.annotations.ReturnTypeParameter
import ai.platon.cdt.kt.protocol.support.annotations.Returns
import ai.platon.cdt.kt.protocol.types.indexeddb.DatabaseWithObjectStores
import ai.platon.cdt.kt.protocol.types.indexeddb.KeyRange
import ai.platon.cdt.kt.protocol.types.indexeddb.Metadata
import ai.platon.cdt.kt.protocol.types.indexeddb.RequestData
import kotlin.Int
import kotlin.String
import kotlin.collections.List

@Experimental
public interface IndexedDB {
  /**
   * Clears all entries from an object store.
   * @param securityOrigin Security origin.
   * @param databaseName Database name.
   * @param objectStoreName Object store name.
   */
  public suspend fun clearObjectStore(
    @ParamName("securityOrigin") securityOrigin: String,
    @ParamName("databaseName") databaseName: String,
    @ParamName("objectStoreName") objectStoreName: String,
  )

  /**
   * Deletes a database.
   * @param securityOrigin Security origin.
   * @param databaseName Database name.
   */
  public suspend fun deleteDatabase(@ParamName("securityOrigin") securityOrigin: String,
      @ParamName("databaseName") databaseName: String)

  /**
   * Delete a range of entries from an object store
   * @param securityOrigin
   * @param databaseName
   * @param objectStoreName
   * @param keyRange Range of entry keys to delete
   */
  public suspend fun deleteObjectStoreEntries(
    @ParamName("securityOrigin") securityOrigin: String,
    @ParamName("databaseName") databaseName: String,
    @ParamName("objectStoreName") objectStoreName: String,
    @ParamName("keyRange") keyRange: KeyRange,
  )

  /**
   * Disables events from backend.
   */
  public suspend fun disable()

  /**
   * Enables events from backend.
   */
  public suspend fun enable()

  /**
   * Requests data from object store or index.
   * @param securityOrigin Security origin.
   * @param databaseName Database name.
   * @param objectStoreName Object store name.
   * @param indexName Index name, empty string for object store data requests.
   * @param skipCount Number of records to skip.
   * @param pageSize Number of records to fetch.
   * @param keyRange Key range.
   */
  public suspend fun requestData(
    @ParamName("securityOrigin") securityOrigin: String,
    @ParamName("databaseName") databaseName: String,
    @ParamName("objectStoreName") objectStoreName: String,
    @ParamName("indexName") indexName: String,
    @ParamName("skipCount") skipCount: Int,
    @ParamName("pageSize") pageSize: Int,
    @ParamName("keyRange") @Optional keyRange: KeyRange?,
  ): RequestData

  public suspend fun requestData(
    @ParamName("securityOrigin") securityOrigin: String,
    @ParamName("databaseName") databaseName: String,
    @ParamName("objectStoreName") objectStoreName: String,
    @ParamName("indexName") indexName: String,
    @ParamName("skipCount") skipCount: Int,
    @ParamName("pageSize") pageSize: Int,
  ): RequestData {
    return requestData(securityOrigin, databaseName, objectStoreName, indexName, skipCount,
        pageSize, null)
  }

  /**
   * Gets metadata of an object store
   * @param securityOrigin Security origin.
   * @param databaseName Database name.
   * @param objectStoreName Object store name.
   */
  public suspend fun getMetadata(
    @ParamName("securityOrigin") securityOrigin: String,
    @ParamName("databaseName") databaseName: String,
    @ParamName("objectStoreName") objectStoreName: String,
  ): Metadata

  /**
   * Requests database with given name in given frame.
   * @param securityOrigin Security origin.
   * @param databaseName Database name.
   */
  @Returns("databaseWithObjectStores")
  public suspend fun requestDatabase(@ParamName("securityOrigin") securityOrigin: String,
      @ParamName("databaseName") databaseName: String): DatabaseWithObjectStores

  /**
   * Requests database names for given security origin.
   * @param securityOrigin Security origin.
   */
  @Returns("databaseNames")
  @ReturnTypeParameter(String::class)
  public suspend fun requestDatabaseNames(@ParamName("securityOrigin") securityOrigin: String):
      List<String>
}
