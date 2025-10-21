package ai.platon.cdt.kt.protocol.commands

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import ai.platon.cdt.kt.protocol.support.annotations.ParamName
import ai.platon.cdt.kt.protocol.support.annotations.Returns
import ai.platon.cdt.kt.protocol.types.io.Read
import kotlin.Int
import kotlin.String

/**
 * Input/Output operations for streams produced by DevTools.
 */
public interface IO {
  /**
   * Close the stream, discard any temporary backing storage.
   * @param handle Handle of the stream to close.
   */
  public suspend fun close(@ParamName("handle") handle: String)

  /**
   * Read a chunk of the stream
   * @param handle Handle of the stream to read.
   * @param offset Seek to the specified offset before reading (if not specificed, proceed with
   * offset
   * following the last read). Some types of streams may only support sequential reads.
   * @param size Maximum number of bytes to read (left upon the agent discretion if not specified).
   */
  public suspend fun read(
    @ParamName("handle") handle: String,
    @ParamName("offset") @Optional offset: Int?,
    @ParamName("size") @Optional size: Int?,
  ): Read

  public suspend fun read(@ParamName("handle") handle: String): Read {
    return read(handle, null, null)
  }

  /**
   * Return UUID of Blob object specified by a remote object id.
   * @param objectId Object id of a Blob object wrapper.
   */
  @Returns("uuid")
  public suspend fun resolveBlob(@ParamName("objectId") objectId: String): String
}
