/*
 * Copyright 2004-2014 H2 Group. Multiple-Licensed under the MPL 2.0,
 * and the EPL 1.0 (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package ai.platon.pulsar.ql.types;

import ai.platon.pulsar.ql.PulsarDataTypesHandler;
import org.h2.api.ErrorCode;
import org.h2.engine.CastDataProvider;
import org.h2.message.DbException;
import org.h2.util.StringUtils;
import org.h2.value.CompareMode;
import org.h2.value.TypeInfo;
import org.h2.value.Value;

import java.net.URI;
import java.sql.PreparedStatement;

/**
 * Implementation of the URI data type.
 */
public class ValueURI extends Value {

    public static int type = PulsarDataTypesHandler.URI_DATA_TYPE_ID;
    public static TypeInfo typeInfo = new TypeInfo(type, 0, 0, 10, null);

    private final URI uri;

    private ValueURI(URI uri) {
        this.uri = uri;
    }

    public URI getURI() {
        return uri;
    }

    /**
     * Create a result set value for the given result set.
     * The result set will be wrapped.
     *
     * @param uri the URI
     * @return the value
     */
    public static ValueURI get(URI uri) {
        ValueURI val = new ValueURI(uri);
        return val;
    }

    public static ValueURI get(String s) {
        return ValueURI.get(URI.create(s));
    }

    public static ValueURI get(byte[] s) {
        return ValueURI.get(URI.create(new String(s)));
    }

    @Override
    public TypeInfo getType() {
        return typeInfo;
    }

    @Override
    public int getValueType() {
        return 0;
    }

    @Override
    public String getString() {
        return uri.toString();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ValueURI && ((ValueURI) other).uri.equals(this.uri);
    }

    @Override
    public int hashCode() {
        return uri.hashCode();
    }

    @Override
    public Object getObject() {
        return uri;
    }

    @Override
    public void set(PreparedStatement prep, int parameterIndex) {
        throw DbException.get(ErrorCode.FEATURE_NOT_SUPPORTED_1, "ValueURI");
    }

    @Override
    public String getSQL() {
        return "X'" + StringUtils.convertBytesToHex(getBytesNoCopy()) + "'::URI";
    }

    @Override
    public StringBuilder getSQL(StringBuilder builder) {
        builder.append("X'").append(toString()).append("'::URI");
        return builder;
    }

    @Override
    public byte[] getBytes() {
        return uri.toString().getBytes();
    }

    /**
     * Used by ValueDataType.writeValue for serialization
     * */
    @Override
    public byte[] getBytesNoCopy() {
        return uri.toString().getBytes();
    }

    @Override
    public int compareTypeSafe(Value v, CompareMode mode, CastDataProvider provider) {
        return 0;
    }

    @Override
    public String toString() {
        return uri.toString();
    }
}
