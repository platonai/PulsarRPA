package ai.platon.pulsar.ql.io;

import ai.platon.pulsar.common.ConcurrentLRUCache;
import ai.platon.pulsar.dom.Documents;
import ai.platon.pulsar.dom.FeaturedDocument;
import ai.platon.pulsar.ql.types.ValueDom;
import org.apache.hadoop.io.Writable;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ValueDomWritable implements Writable {

    public static int CACHE_SIZE = 200;
    private static FeaturedDocument NIL_DOC = FeaturedDocument.Companion.getNIL();
    private static String NIL_DOC_HTML = FeaturedDocument.Companion.getNIL_DOC_HTML();
    private static int NIL_DOC_LENGTH = FeaturedDocument.Companion.getNIL_DOC_LENGTH();
    private static Duration CACHE_EXPIRES = Duration.ofMinutes(10);
    private static String CACHED_HINT = "(cached)";

    // server side
    // TODO: check if this is client side or server side, ensure items in client side lives longer than that in server side
    private static ConcurrentLRUCache<String, String> pageCache = new ConcurrentLRUCache<>(CACHE_EXPIRES, CACHE_SIZE);

    // client side
    private static Map<String, FeaturedDocument> documentCache = new ConcurrentHashMap<>();

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

        String html = pageCache.get(baseUri);
        if (html != null && !html.isEmpty()) {
            // tell the client it's cached
            html = CACHED_HINT;
        } else {
            // not cached, cache it
            html = doc.outerHtml();
            pageCache.put(baseUri, html);
        }

        out.writeInt(html.length());
        out.write(html.getBytes());
    }

    /**
     * TODO: The local cache might not sync with the server side which lead to no data in client side, we need a better solution
     * */
    @Override
    public void readFields(DataInput in) throws IOException {
        String baseUri = in.readLine();
        String selector = in.readLine();
        int htmlLen = in.readInt();

        String html;
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
            ele = NIL_DOC.getBody();
        }

        dom = ValueDom.get(ele);
    }
}
