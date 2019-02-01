package ai.platon.plusar.gora.hbase.store

import org.apache.gora.persistency.impl.PersistentBase
import org.apache.gora.query.Query
import org.apache.gora.query.impl.QueryBase
import org.apache.gora.query.impl.ResultBase
import org.apache.gora.store.DataStore
import org.apache.hadoop.hbase.client.Get
import org.apache.hadoop.hbase.client.Result
import org.apache.hadoop.hbase.client.ResultScanner

/**
 * An [HBaseResult] based on the result of a HBase [Get] query.
 */
class HBaseGetResult<K, T : PersistentBase>(
        dataStore: HBaseStore<K, T>,
        query: Query<K, T>,
        private val result: Result?
) : HBaseResult<K, T>(dataStore, query) {

    override fun getProgress(): Float {
        return if (key == null) 0f else 1f
    }

    public override fun nextInner(): Boolean {
        if (result == null || result.row == null || result.row.isEmpty()) {
            return false
        }
        if (key == null) {
            readNext(result)
            return key != null
        }

        return false
    }

    override fun close() {}
}

abstract class HBaseResult<K, T : PersistentBase>(
        dataStore: HBaseStore<K, T>,
        query: Query<K, T>
): ResultBase<K, T>(dataStore, query) {

    override fun getDataStore(): HBaseStore<K, T> {
        return super.getDataStore() as HBaseStore<K, T>
    }

    protected fun readNext(result: Result) {
        key = ai.platon.plusar.gora.hbase.store.HBaseByteInterface.fromBytes<K>(keyClass, result.row)
        persistent = getDataStore().newInstance(result, query.fields)
    }
}

class HBaseQuery<K, T : PersistentBase>(dataStore: DataStore<K, T>) : QueryBase<K, T>(dataStore)

class HBaseScannerResult<K, T : PersistentBase>(
        dataStore: HBaseStore<K, T>, query: Query<K, T>,
        private val scanner: ResultScanner
) : HBaseResult<K, T>(dataStore, query) {

    public override fun nextInner(): Boolean {
        val result = scanner.next() ?: return false
        readNext(result)
        return true
    }

    override fun close() {
        scanner.close()
    }

    override fun getProgress(): Float {
        //TODO: if limit is set, we know how far we have gone
        return 0f
    }
}
