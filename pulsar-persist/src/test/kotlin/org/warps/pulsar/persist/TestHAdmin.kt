package org.warps.pulsar.persist

import org.apache.avro.util.Utf8
import org.apache.gora.store.DataStore
import org.apache.gora.util.GoraException
import org.apache.hadoop.fs.FileSystem
import org.apache.hadoop.fs.Path
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.warps.pulsar.common.PulsarConstants
import org.warps.pulsar.common.config.ImmutableConfig
import org.warps.pulsar.persist.gora.GoraStorage
import org.warps.pulsar.persist.gora.db.WebDb
import org.warps.pulsar.persist.gora.generated.GWebPage
import org.warps.pulsar.persist.hbase.HAdmin
import kotlin.streams.toList

/**
 * Created by vincent on 17-7-26.
 * Copyright @ 2013-2017 Warpspeed Information. All rights reserved
 */
class TestHAdmin {
    protected lateinit var conf: ImmutableConfig
    protected lateinit var webDb: WebDb

    @Before
    fun setUp() {
        conf = ImmutableConfig()
        webDb = WebDb(conf)
    }

    @After
    fun tearDown() {
    }

    @Test
    fun testHAdmin() {
        val admin = HAdmin(conf)
        println(webDb.schemaName)
        println(admin.describe(webDb.schemaName))
        // admin.addFamily(webDb.schemaName, "vl")
    }
}
