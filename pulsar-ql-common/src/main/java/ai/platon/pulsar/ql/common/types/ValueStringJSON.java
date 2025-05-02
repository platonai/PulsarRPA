/*
 * Copyright 2004-2014 H2 Group. Multiple-Licensed under the MPL 2.0,
 * and the EPL 1.0 (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package ai.platon.pulsar.ql.common.types;

import ai.platon.pulsar.ql.common.PulsarDataTypesHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.h2.api.ErrorCode;
import org.h2.message.DbException;
import org.h2.util.JdbcUtils;
import org.h2.util.StringUtils;
import org.h2.value.*;
import org.jetbrains.annotations.NotNull;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ValueStringJSON extends Value implements Comparable<ValueStringJSON> {

    public static int type = PulsarDataTypesHandler.STRING_JSON_DATA_TYPE_ID;

    public static ValueStringJSON get(String jsonText) {
        Objects.requireNonNull(jsonText);
        return new ValueStringJSON(jsonText);
    }

    public static ValueStringJSON get(String jsonText, String className) {
        Objects.requireNonNull(jsonText);
        Objects.requireNonNull(className);
        return new ValueStringJSON(jsonText, className);
    }

    public static ValueStringJSON get(byte[] jsonText) {
        Objects.requireNonNull(jsonText);
        return new ValueStringJSON(new String(jsonText));
    }

    private String value;
    private String className = Object.class.getName();

    public ValueStringJSON(String value) {
        this.value = value;
    }

    public ValueStringJSON(String value, String className) {
        this.value = value;
        this.className = className;
    }

    public String getTargetClassName() {
        return className;
    }

    @Override
    public Value convertTo(int targetType) {
        if (getType() == targetType) {
            return this;
        }

        if (targetType == Value.STRING) {
            return ValueString.get(getString());
        } else if (targetType == Value.BYTES) {
            return ValueBytes.get(getBytesNoCopy());
        } else if (targetType == Value.JAVA_OBJECT) {
            if (Objects.equals(className, Map.class.getName())) {
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    Map<?, ?> obj = mapper.readValue(value, Map.class);
                    return ValueJavaObject.getNoCopy(obj, getBytesNoCopy(), getDataHandler());
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }
            return ValueJavaObject.getNoCopy(null, getBytesNoCopy(), getDataHandler());
        }

        throw DbException.get(ErrorCode.DATA_CONVERSION_ERROR_1, getString());
    }

    @Override
    public int getType() {
        return type;
    }

    @Override
    public long getPrecision() {
        return 0;
    }

    @Override
    public int getDisplaySize() {
        return value.length();
    }

    @Override
    public String getString() {
        return value;
    }

    @Override
    public Object getObject() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            if (Objects.equals(className, Map.class.getName())) {
                return mapper.readValue(value, Map.class);
            }
            else if (Objects.equals(className, "kotlin.collections.Map")) {
                return mapper.readValue(value, Map.class);
            }
            else if (Objects.equals(className, List.class.getName())) {
                return mapper.readValue(value, List.class);
            }
            else if (Objects.equals(className, "kotlin.collections.List")) {
                return mapper.readValue(value, List.class);
            }
            else {
                return mapper.readTree(value);
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void set(PreparedStatement prep, int parameterIndex) throws SQLException {
        Object obj = JdbcUtils.deserialize(getBytesNoCopy(), getDataHandler());
        prep.setObject(parameterIndex, obj, Types.JAVA_OBJECT);
    }

    @Override
    protected int compareSecure(Value value, CompareMode compareMode) {
        return this == value ? 0 : super.toString().compareTo(value.toString());
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ValueStringJSON && value.equals(o.toString());
    }

    @Override
    public String getSQL() {
        return "X'" + StringUtils.convertBytesToHex(getBytesNoCopy()) + "'::JSON";
    }

    @Override
    public int compareTo(@NotNull ValueStringJSON o) {
        return getString().compareTo(o.getString());
    }
}
