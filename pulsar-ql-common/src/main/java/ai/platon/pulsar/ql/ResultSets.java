package ai.platon.pulsar.ql;

import org.h2.tools.SimpleResultSet;
import org.h2.value.DataType;
import org.h2.value.Value;

import java.sql.ResultSet;

public class ResultSets {
    public static void addColumn(SimpleResultSet rs, String name) {
        rs.addColumn(name, DataType.convertTypeToSQLType(Value.STRING), 0, 0);
    }

    public static void addColumns(SimpleResultSet rs, String... names) {
        for (String name : names) {
            rs.addColumn(name, DataType.convertTypeToSQLType(Value.STRING), 0, 0);
        }
    }

    public static void addColumns(SimpleResultSet rs, Iterable<String> names) {
        for (String name : names) {
            rs.addColumn(name, DataType.convertTypeToSQLType(Value.STRING), 0, 0);
        }
    }

    public static ResultSet newResultSet() {
        SimpleResultSet rs = new SimpleResultSet();
        rs.setAutoClose(false);
        return rs;
    }

    public static SimpleResultSet newSimpleResultSet() {
        SimpleResultSet rs = new SimpleResultSet();
        rs.setAutoClose(false);
        return rs;
    }

    public static SimpleResultSet newSimpleResultSet(Iterable<String> names) {
        SimpleResultSet rs = newSimpleResultSet();
        rs.setAutoClose(false);
        addColumns(rs, names);
        return rs;
    }

    public static SimpleResultSet newSimpleResultSet(String... names) {
        SimpleResultSet rs = newSimpleResultSet();
        rs.setAutoClose(false);
        addColumns(rs, names);
        return rs;
    }
}
