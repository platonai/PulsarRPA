package fun.platonic.pulsar.persist.hbase;

import fun.platonic.pulsar.common.config.ImmutableConfig;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Created by vincent on 17-5-20.
 * Copyright @ 2013-2017 Warpspeed Information. All rights reserved
 */
public class HAdmin implements AutoCloseable {

    public static final Logger LOG = LoggerFactory.getLogger(HAdmin.class);

    private ImmutableConfig conf;
    private Connection conn;
    private Admin admin;

    public HAdmin(ImmutableConfig conf) throws IOException {
        this.conf = conf;
        this.conn = ConnectionFactory.createConnection(conf.unbox());
        this.admin = conn.getAdmin();
    }

    public void addFamily(String tableName, String familyName) throws IOException {
        try (Table table = conn.getTable(TableName.valueOf(tableName))) {
            HTableDescriptor descriptor = new HTableDescriptor(table.getTableDescriptor());
            descriptor.addFamily(new HColumnDescriptor(familyName));
            modifyTable(tableName, descriptor);
        }
    }

    public void removeFamily(String tableName, String familyName) throws IOException {
        try (Table table = conn.getTable(TableName.valueOf(tableName))) {
            HTableDescriptor descriptor = new HTableDescriptor(table.getTableDescriptor());
            descriptor.removeFamily(Bytes.toBytes(familyName));
            modifyTable(tableName, descriptor);
        }
    }

    public boolean exists(String tableName) {
        try {
            return admin.tableExists(TableName.valueOf(tableName));
        } catch (IOException ignored) {
        }
        return false;
    }

    public String describe(String tableName) throws IOException {
        try (Table table = conn.getTable(TableName.valueOf(tableName))) {
            HTableDescriptor descriptor = new HTableDescriptor(table.getTableDescriptor());
            return descriptor.toString();
        }
    }

    public void truncate(String tableName) throws IOException {
        try (Table table = conn.getTable(TableName.valueOf(tableName))) {
            admin.truncateTable(table.getName(), false);
        }
    }

    private void modifyTable(String tableName, HTableDescriptor descriptor) throws IOException {
        TableName table = TableName.valueOf(tableName);
        admin.disableTable(table);
        admin.modifyTable(table, descriptor);
        admin.enableTable(table);
    }

    @Override
    public void close() throws Exception {
        admin.close();
    }
}
