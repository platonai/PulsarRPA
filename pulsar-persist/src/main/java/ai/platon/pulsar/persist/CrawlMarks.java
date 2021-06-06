package ai.platon.pulsar.persist;

import ai.platon.pulsar.persist.metadata.Mark;
import org.apache.avro.util.Utf8;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.stream.Collectors;

import static ai.platon.pulsar.persist.WebPage.wrapKey;

/**
 * Created by vincent on 17-7-26.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 *
 * @author vincent
 * @version $Id: $Id
 */
public class CrawlMarks {

    private Map<CharSequence, CharSequence> marks;

    private CrawlMarks(Map<CharSequence, CharSequence> marks) {
        this.marks = marks;
    }

    /**
     * <p>box.</p>
     *
     * @param marks a {@link java.util.Map} object.
     * @return a {@link ai.platon.pulsar.persist.CrawlMarks} object.
     */
    @Nonnull
    public static CrawlMarks box(Map<CharSequence, CharSequence> marks) {
        return new CrawlMarks(marks);
    }

    /**
     * <p>unbox.</p>
     *
     * @return a {@link java.util.Map} object.
     */
    public Map<CharSequence, CharSequence> unbox() {
        return marks;
    }

    /**
     * <p>get.</p>
     *
     * @param mark a {@link ai.platon.pulsar.persist.metadata.Mark} object.
     * @return a {@link org.apache.avro.util.Utf8} object.
     */
    public Utf8 get(Mark mark) {
        return (Utf8) marks.get(wrapKey(mark));
    }

    /**
     * <p>contains.</p>
     *
     * @param mark a {@link ai.platon.pulsar.persist.metadata.Mark} object.
     * @return a boolean.
     */
    public boolean contains(Mark mark) {
        return get(mark) != null;
    }

    /**
     * <p>isInactive.</p>
     *
     * @return a boolean.
     */
    public boolean isInactive() {
        return contains(Mark.INACTIVE) || contains(Mark.SEMI_INACTIVE);
    }

    /**
     * <p>put.</p>
     *
     * @param mark  a {@link ai.platon.pulsar.persist.metadata.Mark} object.
     * @param value a {@link java.lang.String} object.
     */
    public void put(Mark mark, String value) {
        put(mark, WebPage.u8(value));
    }

    /**
     * <p>put.</p>
     *
     * @param mark  a {@link ai.platon.pulsar.persist.metadata.Mark} object.
     * @param value a {@link org.apache.avro.util.Utf8} object.
     */
    public void put(Mark mark, Utf8 value) {
        marks.put(wrapKey(mark), value);
    }

    /**
     * <p>putIfNotNull.</p>
     *
     * @param mark  a {@link ai.platon.pulsar.persist.metadata.Mark} object.
     * @param value a {@link org.apache.avro.util.Utf8} object.
     */
    public void putIfNotNull(Mark mark, Utf8 value) {
        if (value != null) {
            put(mark, value);
        }
    }

    public void remove(Mark mark) {
        if (contains(mark)) {
            marks.put(wrapKey(mark), null);
        }
    }

    public void removeAll(Iterable<Mark> marks) {
        marks.forEach(this::remove);
    }

    public void clear() {
        marks.clear();
    }

    public Map<String, String> asStringMap() {
        return marks.entrySet().stream()
                .filter(e -> e.getValue() != null)
                .collect(Collectors.toMap(e -> e.getKey().toString(), e -> e.getValue().toString(), (e, e2) -> e));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return marks.entrySet().stream()
                .map(e -> e.getKey() + ": " + e.getValue()).collect(Collectors.joining(", "));
    }
}
