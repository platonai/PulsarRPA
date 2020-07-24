package ai.platon.pulsar.ql;

import org.h2.tools.SimpleResultSet;
import org.h2.value.DataType;
import org.h2.value.Value;

public class SqlUtils {
    public static void addColumn(String name, SimpleResultSet rs) {
        rs.addColumn(name, DataType.convertTypeToSQLType(Value.STRING), 0, 0);
    }
}
