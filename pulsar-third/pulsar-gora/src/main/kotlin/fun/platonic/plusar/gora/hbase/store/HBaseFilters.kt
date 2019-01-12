package `fun`.platonic.plusar.gora.hbase.store

import org.apache.commons.logging.LogFactory
import org.apache.gora.filter.Filter
import org.apache.gora.filter.FilterOp
import org.apache.gora.filter.MapFieldValueFilter
import org.apache.gora.filter.SingleFieldValueFilter
import org.apache.gora.persistency.impl.PersistentBase
import org.apache.gora.util.GoraException
import org.apache.gora.util.ReflectionUtils
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.hbase.client.Scan
import org.apache.hadoop.hbase.filter.CompareFilter
import org.apache.hadoop.hbase.filter.FilterList
import org.apache.hadoop.hbase.filter.SingleColumnValueFilter
import java.util.*

class HBaseFilterUtil<K, T : PersistentBase>
constructor(conf: Configuration) {
    private val LOG = LogFactory.getLog(HBaseFilterUtil::class.java)

    private val factories = LinkedHashMap<String, FilterFactory<K, T>>()

    init {
        val factoryClassNames = conf.getStrings("gora.hbase.filter.factories", "org.apache.gora.hbase.util.DefaultFactory")

        for (factoryClass in factoryClassNames) {
            try {
                val factory = ReflectionUtils.newInstance(factoryClass) as FilterFactory<K, T>
                for (filterClass in factory.supportedFilters) {
                    factories[filterClass] = factory
                }
                factory.hbaseFitlerUtil = this
            } catch (e: Exception) {
                throw GoraException(e)
            }

        }
    }

    fun getFactory(fitler: Filter<K, T>): FilterFactory<K, T>? {
        return factories[fitler.javaClass.canonicalName]
    }

    /**
     * Set a filter on the Scan. It translates a Gora filter to a HBase filter.
     *
     * @param scan
     * @param filter
     * The Gora filter.
     * @param store
     * The HBaseStore.
     * @return if remote filter is succesfully applied.
     */
    fun setFilter(scan: Scan, filter: Filter<K, T>, store: HBaseStore<K, T>): Boolean {

        val factory = getFactory(filter)
        if (factory != null) {
            val hbaseFilter = factory.createFilter(filter, store)
            if (hbaseFilter != null) {
                scan.filter = hbaseFilter
                return true
            } else {
                LOG.warn("HBase remote filter not yet implemented for " + filter.javaClass.canonicalName)
                return false
            }
        } else {
            LOG.warn("HBase remote filter factory not yet implemented for " + filter.javaClass.canonicalName)
            return false
        }
    }
}

interface FilterFactory<K, T : PersistentBase> {
    var util: HBaseFilterUtil<K, T>
    var hbaseFitlerUtil: HBaseFilterUtil<K, T>
    val supportedFilters: List<String>
    fun createFilter(filter: Filter<K, T>, store: HBaseStore<K, T>): org.apache.hadoop.hbase.filter.Filter?
}

abstract class BaseFactory<K, T : PersistentBase> : FilterFactory<K, T>

class DefaultFactory<K, T : PersistentBase>(
        override var util: HBaseFilterUtil<K, T>,
        override var hbaseFitlerUtil: HBaseFilterUtil<K, T>
) : BaseFactory<K, T>() {
    private val LOG = LogFactory.getLog(DefaultFactory::class.java)

    override val supportedFilters: List<String> get() {
        val filters = ArrayList<String>()
        filters.add(SingleFieldValueFilter::class.java.canonicalName)
        filters.add(MapFieldValueFilter::class.java.canonicalName)
        filters.add(org.apache.gora.filter.FilterList::class.java.canonicalName)
        return filters
    }

    override fun createFilter(filter: Filter<K, T>, store: HBaseStore<K, T>): org.apache.hadoop.hbase.filter.Filter? {
        if (filter is org.apache.gora.filter.FilterList<*, *>) {
            val filterList = filter as org.apache.gora.filter.FilterList<K, T>
            val hbaseFilter = org.apache.hadoop.hbase.filter.FilterList(
                    FilterList.Operator.valueOf(filterList.operator.name))
            for (rowFitler in filterList.filters) {
                val factory = util.getFactory(rowFitler)
                if (factory == null) {
                    LOG.warn("HBase remote filter factory not yet implemented for " + rowFitler.javaClass.canonicalName)
                    return null
                }
                val hbaseRowFilter = factory.createFilter(rowFitler, store)
                hbaseFilter.addFilter(hbaseRowFilter)
            }
            return hbaseFilter
        } else if (filter is SingleFieldValueFilter<*, *>) {
            val fieldFilter = filter as SingleFieldValueFilter<K, T>

            val column = store.mapping.getColumn(fieldFilter.fieldName)
                    ?:throw RuntimeException("Field name doesn't exist")
            val compareOp = getCompareOp(fieldFilter.filterOp)
            val family = column.family
            val qualifier = column.qualifier
            val value = HBaseByteInterface.toBytes(fieldFilter.operands[0])
            val hbaseFilter = SingleColumnValueFilter(family, qualifier, compareOp, value)
            hbaseFilter.filterIfMissing = fieldFilter.isFilterIfMissing

            return hbaseFilter
        } else if (filter is MapFieldValueFilter<*, *>) {
            val mapFilter = filter as MapFieldValueFilter<K, T>

            val column = store.mapping.getColumn(mapFilter.fieldName)
                    ?:throw RuntimeException("Field name doesn't exist")
            val compareOp = getCompareOp(mapFilter.filterOp)
            val family = column.family
            val qualifier = HBaseByteInterface.toBytes(mapFilter.mapKey)
            val value = HBaseByteInterface.toBytes(mapFilter.operands[0])
            val hbaseFilter = SingleColumnValueFilter(family, qualifier, compareOp, value)
            hbaseFilter.filterIfMissing = mapFilter.isFilterIfMissing
            return hbaseFilter
        } else {
            LOG.warn("HBase remote filter not yet implemented for " + filter.javaClass.canonicalName)
            return null
        }
    }

    private fun getCompareOp(filterOp: FilterOp): CompareFilter.CompareOp {
        when (filterOp) {
            FilterOp.EQUALS -> return CompareFilter.CompareOp.EQUAL
            FilterOp.NOT_EQUALS -> return CompareFilter.CompareOp.NOT_EQUAL
            FilterOp.LESS -> return CompareFilter.CompareOp.LESS
            FilterOp.LESS_OR_EQUAL -> return CompareFilter.CompareOp.LESS_OR_EQUAL
            FilterOp.GREATER -> return CompareFilter.CompareOp.GREATER
            FilterOp.GREATER_OR_EQUAL -> return CompareFilter.CompareOp.GREATER_OR_EQUAL
            else -> throw IllegalArgumentException(filterOp.toString() + " no HBase equivalent yet")
        }
    }
}
