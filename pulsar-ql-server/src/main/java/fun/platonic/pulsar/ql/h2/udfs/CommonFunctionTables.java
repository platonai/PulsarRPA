package fun.platonic.pulsar.ql.h2.udfs;

import fun.platonic.pulsar.ql.annotation.UDFGroup;
import fun.platonic.pulsar.ql.annotation.UDFunction;
import org.h2.engine.Session;
import org.h2.ext.pulsar.annotation.H2Context;
import org.h2.tools.SimpleResultSet;
import org.h2.value.DataType;
import org.h2.value.Value;
import org.h2.value.ValueArray;

import java.sql.ResultSet;
import java.sql.Types;

@SuppressWarnings("unused")
@UDFGroup
public class CommonFunctionTables {

    @UDFunction
    public static ResultSet map(Value... kvs) {
        SimpleResultSet rs = new SimpleResultSet();
        rs.setAutoClose(false);
        rs.addColumn("KEY");
        rs.addColumn("VALUE");

        if (kvs.length == 0) {
            return rs;
        }

        for (int i = 0; i < kvs.length / 2; i += 2) {
            rs.addRow(kvs[i], kvs[i + 1]);
        }

        return rs;
    }

    @UDFunction
    public static ResultSet explode() {
        SimpleResultSet rs = new SimpleResultSet();
        rs.setAutoClose(false);
        return rs;
    }

    @UDFunction
    public static ResultSet explode(@H2Context Session h2session, ValueArray values) {
        SimpleResultSet rs = new SimpleResultSet();
        rs.setAutoClose(false);

        if (values.getList().length == 0) {
            return rs;
        }

        Value template = values.getList()[0];

        DataType dt = DataType.getDataType(template.getType());
        rs.addColumn("COL", dt.sqlType, (int) template.getPrecision(), template.getScale());

        for (int i = 0; i < values.getList().length; i++) {
            Value value = values.getList()[i];
            rs.addRow(value);
        }

        return rs;
    }

    @UDFunction
    public static ResultSet posexplode() {
        SimpleResultSet rs = new SimpleResultSet();
        rs.setAutoClose(false);
        return rs;
    }

    @UDFunction
    public static ResultSet posexplode(ValueArray values) {
        SimpleResultSet rs = new SimpleResultSet();
        rs.setAutoClose(false);

        if (values.getList().length == 0) {
            return rs;
        }

        rs.addColumn("POS", Types.INTEGER, 10, 0);
        Value template = values.getList()[0];
        DataType dt = DataType.getDataType(template.getType());
        rs.addColumn("COL", dt.sqlType, (int)template.getPrecision(), template.getScale());

        for (int i = 0; i < values.getList().length; i++) {
            rs.addRow(i + 1, values.getList()[i]);
        }
        return rs;
    }
}
