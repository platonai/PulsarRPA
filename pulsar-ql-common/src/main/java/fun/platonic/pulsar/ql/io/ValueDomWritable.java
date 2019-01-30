package fun.platonic.pulsar.ql.io;

import com.google.common.cache.*;
import fun.platonic.pulsar.dom.Documents;
import fun.platonic.pulsar.dom.FeaturedDocument;
import fun.platonic.pulsar.ql.types.ValueDom;
import org.apache.hadoop.io.Writable;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class ValueDomWritable implements Writable {

    public static int CACHE_SIZE = 200;
    private static FeaturedDocument NIL_DOC = FeaturedDocument.Companion.getNIL();
    private static String NIL_DOC_HTML = FeaturedDocument.Companion.getNIL_DOC_HTML();
    private static int NIL_DOC_LENGTH = FeaturedDocument.Companion.getNIL_DOC_LENGTH();
    private static int CACHE_EXPIRES = 10;
    private static String CACHED_HINT = "(cached)";

    // TODO: check if this is client side or server side, ensure client side expires after server side
    // server side
    private static LoadingCache<String, String> pageCache = CacheBuilder.newBuilder()
            .maximumSize(CACHE_SIZE)
            .expireAfterAccess(CACHE_EXPIRES, TimeUnit.MINUTES)
            .build(new CacheLoader<String, String>() {
                @Override
                public String load(String key) {
                    return "";
                }
            });

    // client side
    private static Map<String, FeaturedDocument> documentCache = Collections.synchronizedMap(new HashMap<>());

    private ValueDom dom;

    public ValueDomWritable() {}

    public ValueDomWritable(ValueDom dom) {
        this.dom = dom;
    }

    public ValueDom get() {
        return dom;
    }

    @Override
    public void write(DataOutput out) throws IOException {
        Element ele = dom.getElement();
        Document doc = ele.ownerDocument();
        String baseUri = doc.baseUri();

        out.writeBytes(doc.baseUri());
        out.write('\n'); // make a line

        out.writeBytes(ele.cssSelector());
        out.write('\n'); // make a line

        String html = "";
        try {
            html = pageCache.get(baseUri);
            if (!html.isEmpty()) {
                // tell the client it's cached
                html = CACHED_HINT;
            } else {
                // not cached, cache it
                html = doc.outerHtml();
                pageCache.put(baseUri, html);
                System.out.println("cache html: " + html.length());
            }
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        out.writeInt(html.length());
        out.write(html.getBytes());

        if (html.length() > 8) {
            System.out.println("written: " + html.length());
        }
    }

    /**
     * TODO: The local cache might not sync with the server side which lead to no data in client side, we need a better solution
     * */
    @Override
    public void readFields(DataInput in) throws IOException {
        String baseUri = in.readLine();
        String selector = in.readLine();
        int htmlLen = in.readInt();

        String html = "";
        FeaturedDocument doc;
        if (htmlLen == CACHED_HINT.length()) {
            // cached
            doc = documentCache.get(baseUri);
            in.skipBytes(htmlLen);
        } else {
            // not cached
            byte[] bytes = new byte[htmlLen];
            in.readFully(bytes);
            html = new String(bytes);

            System.out.println("Cache it");

            doc = Documents.INSTANCE.parse(html, baseUri);
            documentCache.put(baseUri, doc);
        }

        if (doc == null) {
            doc = NIL_DOC;
        }

        Element ele;
        if (selector.equals("#root")) {
            ele = doc.unbox();
        } else {
            ele = doc.first(selector);
        }

        if (ele == null) {
            ele = NIL_DOC.getBody().clone();
        }

        if (doc == NIL_DOC) {
            ele = ele.clone();
        }

        dom = ValueDom.get(ele);
    }
}
