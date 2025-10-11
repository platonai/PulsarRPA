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

public class ProtocolHeaders implements HttpHeaders {

    static Perl5Pattern[] patterns = {null, null};

    static {
        Perl5Compiler compiler = new Perl5Compiler();
        try {
            patterns[0] = (Perl5Pattern) compiler.compile("\\bfilename=['\"](.+)['\"]");
            patterns[1] = (Perl5Pattern) compiler.compile("\\bfilename=(\\S+)\\b");
        } catch (MalformedPatternException e) {
        }
    }

    private Map<CharSequence, CharSequence> headers;

    private ProtocolHeaders(Map<CharSequence, CharSequence> headers) {
        this.headers = headers;
    }

    public static ProtocolHeaders box(Map<CharSequence, CharSequence> headers) {
        return new ProtocolHeaders(headers);
    }

    public Map<CharSequence, CharSequence> unbox() {
        return headers;
    }

    public String get(String name) {
        CharSequence value = headers.get(JPersistUtils.u8(name));
        return value == null ? null : value.toString();
    }

    public String getOrDefault(String name, String defaultValue) {
        CharSequence value = headers.get(JPersistUtils.u8(name));
        return value == null ? defaultValue : value.toString();
    }

    public void put(String name, String value) {
        headers.put(JPersistUtils.u8(name), JPersistUtils.u8(value));
    }

    public void putAll(Map<String, String> map) {
        for (Map.Entry<String, String> entry : map.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    public void putAll(Multimap<String, String> map) {
        for (Map.Entry<String, String> entry : map.entries()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    public void remove(String name) {
        headers.remove(JPersistUtils.u8(name));
    }

    public Instant getLastModified() {
        CharSequence lastModified = get(HttpHeaders.LAST_MODIFIED);
        if (lastModified != null) {
            return DateTimes.parseHttpDateTime(lastModified.toString(), Instant.EPOCH);
        }

        return Instant.EPOCH;
    }

    public int getContentLength() {
        String length = get(HttpHeaders.CONTENT_LENGTH);
        if (length == null) {
            return -1;
        }

        return SParser.wrap(length.trim()).getInt(-1);
    }

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

    public String getDecodedDispositionFilename() {
        try {
            return getDecodedDispositionFilename(StandardCharsets.UTF_8);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Unexpected unsupported encoding `UTF-8`");
        }
    }

    public String getDecodedDispositionFilename(Charset charset) throws UnsupportedEncodingException {
        String filename = getDispositionFilename();

        if (filename != null) {
            return URLDecoder.decode(filename, charset);
        }

        return null;
    }

    public void clear() {
        headers.clear();
    }

    public Map<String, String> asStringMap() {
        return headers.entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey().toString(), e -> e.getValue().toString(), (e, e2) -> e));
    }

    @Override
    public String toString() {
        return headers.entrySet().stream()
                .map(e -> e.getKey() + ": " + e.getValue())
                .collect(Collectors.joining("\n"));
    }
}
