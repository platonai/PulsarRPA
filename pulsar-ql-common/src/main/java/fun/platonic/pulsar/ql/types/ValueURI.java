/*
 * Copyright 2004-2014 H2 Group. Multiple-Licensed under the MPL 2.0,
 * and the EPL 1.0 (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package fun.platonic.pulsar.ql.types;

import fun.platonic.pulsar.ql.PulsarDataTypesHandler;
import org.h2.util.StringUtils;
import org.h2.value.CompareMode;
import org.h2.value.Value;

import java.net.URI;
import java.sql.PreparedStatement;

/**
 * Implementation of the URI data type.
 */
public class ValueURI extends Value {

    public static int type = PulsarDataTypesHandler.URI_DATA_TYPE_ID;

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
    public int getType() {
        return type;
    }

    @Override
    public long getPrecision() {
        return 0;
    }

    @Override
    public int getDisplaySize() {
        // it doesn't make sense to calculate it
        return uri.toString().length();
    }

    @Override
    public String getString() {
        return uri.toString();
    }

    @Override
    protected int compareSecure(Value v, CompareMode mode) {
        return this == v ? 0 : super.toString().compareTo(v.toString());
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
        throw throwUnsupportedExceptionForType("ValueURI.set");
    }

    @Override
    public String getSQL() {
        return "X'" + StringUtils.convertBytesToHex(getBytesNoCopy()) + "'::URI";
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
    public String toString() {
        return uri.toString();
    }
}
