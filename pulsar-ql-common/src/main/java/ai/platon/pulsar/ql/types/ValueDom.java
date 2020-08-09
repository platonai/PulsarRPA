/*
 * Copyright 2004-2014 H2 Group. Multiple-Licensed under the MPL 2.0,
 * and the EPL 1.0 (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package ai.platon.pulsar.ql.types;

import ai.platon.pulsar.dom.FeaturedDocument;
import ai.platon.pulsar.dom.nodes.node.ext.NodeExtKt;
import ai.platon.pulsar.ql.PulsarDataTypesHandler;
import org.h2.api.ErrorCode;
import org.h2.message.DbException;
import org.h2.util.JdbcUtils;
import org.h2.util.StringUtils;
import org.h2.value.CompareMode;
import org.h2.value.Value;
import org.h2.value.ValueBytes;
import org.h2.value.ValueJavaObject;
import org.h2.value.ValueString;
import org.jetbrains.annotations.NotNull;
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
 */
public class ValueDom extends Value implements Comparable<ValueDom> {

    public static final Document NIL_DOC = FeaturedDocument.Companion.getNIL().getDocument();
    public static final ValueDom NIL = new ValueDom(NIL_DOC);

    public static int type = PulsarDataTypesHandler.DOM_DATA_TYPE_ID;

    private final Document document;
    private final Element element;
    private String outerHtml = null;

    private ValueDom(Element element) {
        Objects.requireNonNull(element);

        this.document = element.ownerDocument();
        this.element = element;
    }

    public Document getDocument() {
        return document;
    }

    public Element getElement() {
        return element;
    }

    public String globalId() {
        return NodeExtKt.getGlobalId(element);
    }

    public boolean isNil() {
        return this == NIL;
    }

    public boolean isNotNil() {
        return this != NIL;
    }

    public boolean isDocument() {
        return element == document;
    }

    /**
     * TODO: might use lot of memory so memory should be managed
     * */
    public String getOuterHtml() {
        // TODO: document scope cache
        if (outerHtml == null) outerHtml = element.outerHtml();
        return outerHtml;
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
        }

        return new ValueDom(element);
    }

    @Nonnull
    public static ValueDom get(FeaturedDocument doc) {
        return get(doc.unbox());
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
        } else if (targetType == Value.JAVA_OBJECT) {
            System.err.println("Convert ValueDom to ValueJavaObject");
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
        return 128;
    }

    @Override
    public int getDisplaySize() {
        return 128;
    }

    @Override
    public int getMemory() {
        return getDisplaySize() * 2 + 48;
    }

    @Override
    public String getString() {
        return toString();
    }

    /**
     * TODO: performance
     * When to use compareSecure?
     * */
    @Override
    protected int compareSecure(Value o, CompareMode mode) {
        ValueDom v = (ValueDom) o;
        return mode.compareString(getOuterHtml(), v.getOuterHtml(), false);
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
        return globalId().equals(dom.globalId());
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
        return getOuterHtml().getBytes();
    }

    /**
     * Used by ValueDataType.writeValue for serialization
     * */
    @Override
    public byte[] getBytesNoCopy() {
        return getOuterHtml().getBytes();
    }

    @Override
    public int hashCode() {
        return globalId().hashCode();
    }

    @Override
    public String toString() {
        if (element != null) {
            if (isNotNil()) {
                return NodeExtKt.getName(element);
            } else {
                return "(nil)";
            }
        }
        return "(dom)";
    }

    @Override
    public int compareTo(@NotNull ValueDom o) {
        return globalId().compareTo(o.globalId());
    }
}
