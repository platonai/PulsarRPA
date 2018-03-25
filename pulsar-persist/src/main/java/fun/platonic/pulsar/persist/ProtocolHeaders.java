package fun.platonic.pulsar.persist;

import fun.platonic.pulsar.common.DateTimeUtil;
import fun.platonic.pulsar.common.HttpHeaders;
import fun.platonic.pulsar.common.SParser;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by vincent on 17-7-26.
 * Copyright @ 2013-2017 Warpspeed Information. All rights reserved
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
 */
public class ProtocolHeaders implements HttpHeaders {
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
        CharSequence value = headers.get(WebPage.u8(name));
        return value == null ? null : value.toString();
    }

    public String getOrDefault(String name, String defaultValue) {
        CharSequence value = headers.get(WebPage.u8(name));
        return value == null ? defaultValue : value.toString();
    }

    public void put(String name, String value) {
        headers.put(WebPage.u8(name), WebPage.u8(value));
    }

    public void remove(String name) {
        headers.remove(WebPage.u8(name));
    }

    /**
     * @return Get LAST_MODIFIED in protocol header, Instant.EPOCH if not specified
     */
    public Instant getLastModified() {
        CharSequence lastModified = get(HttpHeaders.LAST_MODIFIED);
        if (lastModified != null) {
            return DateTimeUtil.parseHttpDateTime(lastModified.toString(), Instant.EPOCH);
        }

        return Instant.EPOCH;
    }

    /**
     * @return Get CONTENT_LENGTH in protocol header, -1 if not specified
     */
    public int getContentLength() {
        String length = get(HttpHeaders.CONTENT_LENGTH);
        if (length == null) {
            return -1;
        }

        return SParser.wrap(length.trim()).getInt(-1);
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
