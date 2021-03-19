package org.apache.gora.mongodb.utils;

import org.bson.Document;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

/**
 * <p>Utility for building complex objects. For example:</p>
 * <pre>
 *   {@code DocumentBuilder.start().add( "name" , "eliot").add("number" , 17).get()}
 * </pre>
 */
@SuppressWarnings("rawtypes")
public class DocumentBuilder {

    /**
     * Creates a builder intialized with an empty document.
     */
    public DocumentBuilder() {
        _stack = new LinkedList<>();
        _stack.add(new Document());
    }

    /**
     * Creates a builder intialized with an empty document.
     *
     * @return The new empty builder
     */
    public static DocumentBuilder start() {
        return new DocumentBuilder();
    }

    /**
     * Creates a builder initialized with the given key/value.
     *
     * @param key The field name
     * @param val The value
     * @return the new builder
     */
    public static DocumentBuilder start(final String key, final Object val) {
        return (new DocumentBuilder()).add(key, val);
    }

    /**
     * Creates an object builder from an existing map of key value pairs.
     *
     * @param documentAsMap a document in Map form.
     * @return the new builder
     */
    @SuppressWarnings("unchecked")
    public static DocumentBuilder start(final Map documentAsMap) {
        DocumentBuilder builder = new DocumentBuilder();
        Iterator<Map.Entry> i = documentAsMap.entrySet().iterator();
        while (i.hasNext()) {
            Map.Entry entry = i.next();
            builder.add(entry.getKey().toString(), entry.getValue());
        }
        return builder;
    }

    /**
     * Appends the key/value to the active object
     *
     * @param key the field name
     * @param val the value of the field
     * @return {@code this} so calls can be chained
     */
    public DocumentBuilder append(final String key, final Object val) {
        _cur().put(key, val);
        return this;
    }

    /**
     * Same as append
     *
     * @param key the field name
     * @param val the value of the field
     * @return {@code this} so calls can be chained
     * @see #append(String, Object)
     */
    public DocumentBuilder add(final String key, final Object val) {
        return append(key, val);
    }

    /**
     * Creates an new empty object and inserts it into the current object with the given key. The new child object becomes the active one.
     *
     * @param key the field name
     * @return {@code this} so calls can be chained
     */
    public DocumentBuilder push(final String key) {
        Document o = new Document();
        _cur().put(key, o);
        _stack.addLast(o);
        return this;
    }

    /**
     * Pops the active object, which means that the parent object becomes active
     *
     * @return {@code this} so calls can be chained
     */
    public DocumentBuilder pop() {
        if (_stack.size() <= 1) {
            throw new IllegalArgumentException("can't pop last element");
        }
        _stack.removeLast();
        return this;
    }

    /**
     * Gets the top level document.
     *
     * @return The base object
     */
    public Document get() {
        return _stack.getFirst();
    }

    /**
     * Returns true if no key/value was inserted into the top level document.
     *
     * @return true if empty
     */
    public boolean isEmpty() {
        return ((Document) _stack.getFirst()).size() == 0;
    }

    private Document _cur() {
        return _stack.getLast();
    }

    private final LinkedList<Document> _stack;

}
