package ai.platon.pulsar.persist;

import ai.platon.pulsar.common.DateTimes;
import ai.platon.pulsar.common.HttpHeaders;
import ai.platon.pulsar.common.SParser;
import com.google.common.collect.Multimap;
import org.apache.oro.text.regex.*;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by vincent on 17-7-26.
 * Copyright @ 2013-2023 Platon AI. All rights reserved
 * <p>
 * Header information returned from the web server used to serve the content which is subsequently fetched from.
 * This includes keys such as
 * TRANSFER_ENCODING,
 * CONTENT_ENCODING,
 * CONTENT_LANGUAGE,
 * CONTENT_LENGTH,
 * CONTENT_LOCATION,
 * CONTENT_DISPOSITION,
 * CONTENT_MD5,
 * CONTENT_TYPE,
 * LAST_MODIFIED
 * and LOCATION.
 *
 * @author vincent
 * @version $Id: $Id
 */
public class ProtocolHeaders implements HttpHeaders {

    static Perl5Pattern patterns[] = {null, null};

    static {
        Perl5Compiler compiler = new Perl5Compiler();
        try {
            // order here is important
            patterns[0] = (Perl5Pattern) compiler.compile("\\bfilename=['\"](.+)['\"]");
            patterns[1] = (Perl5Pattern) compiler.compile("\\bfilename=(\\S+)\\b");
        } catch (MalformedPatternException e) {
            // just ignore
        }
    }

    private Map<CharSequence, CharSequence> headers;

    private ProtocolHeaders(Map<CharSequence, CharSequence> headers) {
        this.headers = headers;
    }

    /**
     * <p>box.</p>
     *
     * @param headers a {@link java.util.Map} object.
     * @return a {@link ai.platon.pulsar.persist.ProtocolHeaders} object.
     */
    public static ProtocolHeaders box(Map<CharSequence, CharSequence> headers) {
        return new ProtocolHeaders(headers);
    }

    /**
     * <p>unbox.</p>
     *
     * @return a {@link java.util.Map} object.
     */
    public Map<CharSequence, CharSequence> unbox() {
        return headers;
    }

    /**
     * <p>get.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public String get(String name) {
        CharSequence value = headers.get(JPersistUtils.u8(name));
        return value == null ? null : value.toString();
    }

    /**
     * <p>getOrDefault.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @param defaultValue a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public String getOrDefault(String name, String defaultValue) {
        CharSequence value = headers.get(JPersistUtils.u8(name));
        return value == null ? defaultValue : value.toString();
    }

    /**
     * <p>put.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @param value a {@link java.lang.String} object.
     */
    public void put(String name, String value) {
        headers.put(JPersistUtils.u8(name), JPersistUtils.u8(value));
    }

    /**
     * <p>putAll.</p>
     *
     * @param map a {@link java.util.Map} object.
     */
    public void putAll(Map<String, String> map) {
        for (Map.Entry<String, String> entry : map.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * <p>putAll.</p>
     *
     * @param map a {@link com.google.common.collect.Multimap} object.
     */
    public void putAll(Multimap<String, String> map) {
        for (Map.Entry<String, String> entry : map.entries()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * <p>remove.</p>
     *
     * @param name a {@link java.lang.String} object.
     */
    public void remove(String name) {
        headers.remove(JPersistUtils.u8(name));
    }

    /**
     * <p>getLastModified.</p>
     *
     * @return Get LAST_MODIFIED in protocol header, Instant.EPOCH if not specified
     */
    public Instant getLastModified() {
        CharSequence lastModified = get(HttpHeaders.LAST_MODIFIED);
        if (lastModified != null) {
            return DateTimes.parseHttpDateTime(lastModified.toString(), Instant.EPOCH);
        }

        return Instant.EPOCH;
    }

    /**
     * <p>getContentLength.</p>
     *
     * @return Get CONTENT_LENGTH in protocol header, -1 if not specified
     */
    public int getContentLength() {
        String length = get(HttpHeaders.CONTENT_LENGTH);
        if (length == null) {
            return -1;
        }

        return SParser.wrap(length.trim()).getInt(-1);
    }

    /**
     * Get attachement filename if we see non-standard HTTP header "Content-Disposition".
     * It's a good indication that content provider wants filename therein
     * be used as the title of this url.
     * Patterns used to extract filename from possible non-standard
     * HTTP header "Content-Disposition". Typically it looks like:
     * Content-Disposition: inline; filename="foo.ppt"
     *
     * @return a {@link java.lang.String} object.
     */
    public String getDispositionFilename() {
        CharSequence contentDisposition = get(HttpHeaders.CONTENT_DISPOSITION);
        if (contentDisposition == null) {
            return null;
        }

        PatternMatcher matcher = new Perl5Matcher();
        for (Perl5Pattern pattern : patterns) {
            if (matcher.contains(contentDisposition.toString(), pattern)) {
                return matcher.getMatch().group(1);
            }
        }

        return null;
    }

    /**
     * <p>getDecodedDispositionFilename.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getDecodedDispositionFilename() {
        try {
            return getDecodedDispositionFilename(StandardCharsets.UTF_8);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Unexpected unsupported encoding `UTF-8`");
        }
    }

    /**
     * <p>getDecodedDispositionFilename.</p>
     *
     * @param charset a {@link java.nio.charset.Charset} object.
     * @return a {@link java.lang.String} object.
     * @throws java.io.UnsupportedEncodingException if any.
     */
    public String getDecodedDispositionFilename(Charset charset) throws UnsupportedEncodingException {
        String filename = getDispositionFilename();

        if (filename != null) {
            return URLDecoder.decode(filename, charset.toString());
        }

        return null;
    }

    /**
     * <p>clear.</p>
     */
    public void clear() {
        headers.clear();
    }

    /**
     * <p>asStringMap.</p>
     *
     * @return a {@link java.util.Map} object.
     */
    public Map<String, String> asStringMap() {
        return headers.entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey().toString(), e -> e.getValue().toString(), (e, e2) -> e));
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return headers.entrySet().stream()
                .map(e -> e.getKey() + ": " + e.getValue())
                .collect(Collectors.joining("\n"));
    }
}
