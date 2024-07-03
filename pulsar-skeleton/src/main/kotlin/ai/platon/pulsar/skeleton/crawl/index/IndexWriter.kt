package ai.platon.pulsar.skeleton.crawl.index

import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Parameterized
import ai.platon.pulsar.skeleton.crawl.common.JobInitialized
import org.slf4j.LoggerFactory
import java.io.IOException

/**
 * Created by vincent on 16-8-1.
 */
interface IndexWriter : Parameterized, JobInitialized, AutoCloseable {
    val name: String get() = javaClass.simpleName
    val isActive: Boolean
        get() = true

    @Throws(IOException::class)
    fun open(conf: ImmutableConfig?)

    @Throws(IOException::class)
    fun open(indexerUrl: String?)

    @Throws(IOException::class)
    fun write(doc: IndexDocument?)

    @Throws(IOException::class)
    fun delete(key: String?)

    @Throws(IOException::class)
    fun update(doc: IndexDocument?)

    @Throws(IOException::class)
    fun commit()

    @Throws(IOException::class)
    override fun close()

    /**
     * Returns a String describing the IndexWriter instance and the specific
     * parameters it can take
     */
    fun describe(): String?

    companion object {
        val LOG = LoggerFactory.getLogger(IndexWriter::class.java)
    }
}