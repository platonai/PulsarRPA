package ai.platon.pulsar.ql.utils;

import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Copy from org.h2.tools.Shell
 * */
public class ResultSetFormatter {
    public static int MAX_ROW_BUFFER = 5000;
    public static int MAX_COLUMN_LENGTH = 1000;
    public static char BOX_VERTICAL = '|';
    private ResultSet rs;
    private int rowCount;

    public ResultSetFormatter(ResultSet rs) {
        this.rs = rs;
    }

    public String format() {
        return format(false);
    }

    public String format(boolean asList) {
        try {
            if (asList) {
                return getResultAsList();
            }
            return getResultAsTable();
        } catch (SQLException e) {
            return "(Exception)" + e.getMessage();
        }
    }

    public String formatAsLine() {
        return format(true);
    }

    private String getResultAsTable() throws SQLException {
        StringBuilder sb = new StringBuilder();

        ResultSetMetaData meta = rs.getMetaData();
        int len = meta.getColumnCount();
        ArrayList<String[]> rows = new ArrayList<>();
        // buffer the header
        String[] columns = new String[len];
        for (int i = 0; i < len; i++) {
            String s = meta.getColumnLabel(i + 1);
            columns[i] = s == null ? "" : s;
        }
        rows.add(columns);

        while (rs.next()) {
            rowCount++;
            loadRow(len, rows);
            if (rowCount > MAX_ROW_BUFFER) {
                sb.append(formatRows(rows, len));
                sb.append("\n");
                rows.clear();
            }
        }

        sb.append(formatRows(rows, len));

        return sb.toString();
    }

    private void loadRow(int len, ArrayList<String[]> rows) throws SQLException {
        String[] row = new String[len];
        int i = 0;
        while (i < len) {
            String s = formatRow(rs, i + 1);
            // only truncate if more than one column
            if (len > 1 && s.length() > MAX_COLUMN_LENGTH) {
                s = s.substring(0, MAX_COLUMN_LENGTH);
                s += " ...";
            }
            row[i] = s;

            ++i;
        }
        rows.add(row);
    }

    /**
     * Precision and scale:
     *
     * Precision is the number of digits in a number.
     * Scale is the number of digits to the right of the decimal point in a number.
     * For example, the number 123.45 has a precision of 5 and a scale of 2.
     * @link {https://docs.microsoft.com/en-us/sql/t-sql/data-types/precision-scale-and-length-transact-sql?view=sql-server-2017}
     * */
    private String formatRow(ResultSet rs, int columnIndex) throws SQLException {
        String s;
        int type = rs.getMetaData().getColumnType(columnIndex);
        switch (type) {
            case Types.DOUBLE:
            case Types.FLOAT:
            case Types.REAL:
                int precision = rs.getMetaData().getPrecision(columnIndex);
                int scale = rs.getMetaData().getScale(columnIndex);
                if (precision <= 0) precision = 10;
                if (scale <= 0) scale = 10;
                double d = rs.getDouble(columnIndex);
                s = String.format("%" + precision + "." + scale + "f", d);
                break;
            default:
                s = rs.getString(columnIndex);
        }
        if (s == null) {
            s = "null";
        }

        return s;
    }

    private String formatRows(final ArrayList<String[]> rows, final int len) {
        int[] columnSizes = new int[len];
        for (int i = 0; i < len; i++) {
            int max = 0;
            for (String[] row : rows) {
                max = Math.max(max, row[i].length());
            }
            if (len > 1) {
                max = Math.min(MAX_COLUMN_LENGTH, max);
            }
            columnSizes[i] = max;
        }

        StringBuilder buff = new StringBuilder();
        for (String[] row : rows) {
            int i = 0;
            while (i < len) {
                if (i > 0) {
                    buff.append(' ').append(BOX_VERTICAL).append(' ');
                }

                String s = row[i];
                buff.append(s);
                if (i < len - 1) {
                    for (int j = s.length(); j < columnSizes[i]; j++) {
                        buff.append(' ');
                    }
                }

                ++i;
            }

            buff.append("\n");
        }

        return buff.toString();
    }

    private String getResultAsList() throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        int longestLabel = 0;
        final int len = meta.getColumnCount();
        String[] columns = new String[len];
        for (int i = 0; i < len; i++) {
            String s = meta.getColumnLabel(i + 1);
            columns[i] = s;
            longestLabel = Math.max(longestLabel, s.length());
        }

        StringBuilder sb = new StringBuilder();
        while (rs.next()) {
            rowCount++;
            sb.setLength(0);
            if (rowCount > 1) {
                sb.append("");
            }

            for (int i = 0; i < len; ++i) {
                if (i > 0) {
                    sb.append('\n');
                }

                sb.append(StringUtils.rightPad(columns[i] + ":", 15 + longestLabel))
                        .append(rs.getString(i + 1));
            }
            sb.append("\n");
        }
        if (rowCount == 0) {
            String s = String.join("\n", columns);
            sb.append(s).append("\n");
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return format() + "Total " + rowCount + " rows";
    }
}
