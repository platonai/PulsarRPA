/*
 * Copyright 2004-2014 H2 Group. Multiple-Licensed under the MPL 2.0,
 * and the EPL 1.0 (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package fun.platonic.pulsar.ql.types;

import fun.platonic.pulsar.ql.PulsarDataTypesHandler;
import org.h2.api.ErrorCode;
import org.h2.message.DbException;
import org.h2.util.JdbcUtils;
import org.h2.util.StringUtils;
import org.h2.value.CompareMode;
import org.h2.value.Value;
import org.h2.value.ValueBytes;
import org.h2.value.ValueString;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import javax.annotation.Nonnull;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Objects;

/**
 * Implementation of the DOM data type.
 *
 * TODO: make ValueDom nullable
 */
public class ValueDom extends Value {

    public static final String DEFAULT_BASE_URI = "http://example.com/";
    public static final Document NIL_DOC = Document.createShell(DEFAULT_BASE_URI + "nil");
    public static final ValueDom NIL = new ValueDom(NIL_DOC);

    public static int type = PulsarDataTypesHandler.DOM_DATA_TYPE_ID;

    private final Element element;
    private String outHtml;

    private ValueDom(Element element) {
        Objects.requireNonNull(element);

        this.element = element;
    }

    public Element getElement() {
        return element;
    }

    public boolean isNil() {
        return this == NIL;
    }

    public boolean isNotNil() {
        return this != NIL;
    }

    public void setChanged() {
        outHtml = null;
    }

    public boolean isDocument() {
        return element instanceof Document;
    }

    public String getOutHtml() {
        if (outHtml == null) {
            outHtml = element.outerHtml();
        }

        return outHtml;
    }

    /**
     * Create a result set value for the given result set.
     * The result set will be wrapped.
     *
     * @param element the Element
     * @return the value
     */
    @Nonnull
    public static ValueDom get(Element element) {
        Objects.requireNonNull(element);

        if (element instanceof Document) {
            Document doc = (Document) element;
            String baseUri = doc.baseUri();

            if (baseUri != null && !baseUri.isEmpty()) {
                doc.body().attr("baseUri", baseUri);
            } else {
                baseUri = doc.body().attr("baseUri");
                doc.setBaseUri(baseUri);
            }

            return new ValueDom(doc);
        }

        return new ValueDom(element);
    }

    @Nonnull
    public static ValueDom getOrNil(Element element) {
        return element == null ? NIL : get(element);
    }

    @Nonnull
    public static ValueDom get(String html) {
        Objects.requireNonNull(html);
        return get(Jsoup.parse(html));
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
        }
//        else if (targetType == Value.JAVA_OBJECT) {
//            System.out.println("Convert ValueDom to ValueJavaObject");
//            return ValueJavaObject.getNoCopy(null, getBytesNoCopy(), getDataHandler());
//        }

        throw DbException.get(ErrorCode.DATA_CONVERSION_ERROR_1, getString());
    }

    @Override
    public int getType() {
        return type;
    }

    @Override
    public long getPrecision() {
        return getOutHtml().length();
    }

    @Override
    public int getDisplaySize() {
        return getOutHtml().length();
    }

    @Override
    public int getMemory() {
        return getDisplaySize() * 2 + 48;
    }

    @Override
    public String getString() {
        // return getOutHtml();
        return toString();
    }

    /**
     * TODO: performance
     * When to use compareSecure?
     * */
    @Override
    protected int compareSecure(Value o, CompareMode mode) {
        ValueDom v = (ValueDom) o;
        return mode.compareString(getOutHtml(), v.getOutHtml(), false);
    }

    /**
     * TODO: We might have a better Element.equals implementation
     * */
    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof ValueDom)) {
            return false;
        }

        ValueDom dom = (ValueDom) other;
        return dom.element.equals(this.element) || dom.getOutHtml().equals(this.getOutHtml());
    }

    @Override
    public Object getObject() {
        return element;
    }

    @Override
    public void set(PreparedStatement prep, int parameterIndex) throws SQLException {
        Object obj = JdbcUtils.deserialize(getBytesNoCopy(), getDataHandler());
        prep.setObject(parameterIndex, obj, Types.JAVA_OBJECT);
    }

    @Override
    public String getSQL() {
        return "X'" + StringUtils.convertBytesToHex(getBytesNoCopy()) + "'::Dom";
    }

    @Override
    public byte[] getBytes() {
        return getOutHtml().getBytes();
    }

    /**
     * Used by ValueDataType.writeValue for serialization
     * */
    @Override
    public byte[] getBytesNoCopy() {
        return getOutHtml().getBytes();
    }

    @Override
    public int hashCode() {
        return getOutHtml().hashCode();
    }

    @Override
    public String toString() {
        return element.getNodeSequence() + "-" + element.nodeName();
    }
}
