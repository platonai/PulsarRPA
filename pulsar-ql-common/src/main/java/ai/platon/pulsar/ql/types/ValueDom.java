/*
 * Copyright 2004-2014 H2 Group. Multiple-Licensed under the MPL 2.0,
 * and the EPL 1.0 (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package ai.platon.pulsar.ql.types;

import ai.platon.pulsar.dom.FeaturedDocument;
import ai.platon.pulsar.dom.nodes.node.ext.NodeExtKt;
import ai.platon.pulsar.ql.PulsarDataTypesHandler;
import org.h2.engine.CastDataProvider;
import org.h2.util.JdbcUtils;
import org.h2.value.CompareMode;
import org.h2.value.TypeInfo;
import org.h2.value.Value;
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
    public static TypeInfo typeInfo = new TypeInfo(type, 128, 128, 10, null);

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
        // throw new RuntimeException("From here");
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
    public int compareTypeSafe(Value v, CompareMode mode, CastDataProvider provider) {
        return 0;
    }

    @Override
    public TypeInfo getType() {
        return typeInfo;
    }

    @Override
    public int getValueType() {
        return 0;
    }

//    @Override
//    public long getPrecision() {
//        return 128;
//    }
//
//    @Override
//    public int getDisplaySize() {
//        return 128;
//    }

    @Override
    public int getMemory() {
        if (outerHtml == null) {
            return 32;
        } else return outerHtml.length();
    }

    @Override
    public String getString() {
        return toString();
    }

//    /**
//     * TODO: performance
//     * When to use compareSecure?
//     * */
//    @Override
//    protected int compareSecure(Value o, CompareMode mode) {
//        ValueDom v = (ValueDom) o;
//        return mode.compareString(getOuterHtml(), v.getOuterHtml(), false);
//    }

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
    public StringBuilder getSQL(StringBuilder builder) {
        return builder.append("X'").append(toString()).append("::Dom");
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
