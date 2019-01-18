package fun.platonic.pulsar.ql.io;

import fun.platonic.pulsar.common.ConcurrentLRUCache;
import fun.platonic.pulsar.common.config.PulsarConstants;
import fun.platonic.pulsar.ql.types.ValueDom;
import fun.platonic.pulsar.dom.Documents;
import fun.platonic.pulsar.dom.FeaturedDocument;
import org.apache.hadoop.io.Writable;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.time.Duration;

public class ValueDomWritable implements Writable {

    public static int CACHE_SIZE = 1000;
    private static ConcurrentLRUCache<String, FeaturedDocument> cachedDocuments;
    private static FeaturedDocument NIL_DOC = FeaturedDocument.Companion.getNIL();
    private static int NIL_DOC_LENGTH = FeaturedDocument.Companion.getNIL_DOC_LENGTH();

    private ValueDom dom;

    public ValueDomWritable() {
        createDocumentCache();
    }

    public ValueDomWritable(ValueDom dom) {
        this.dom = dom;
        createDocumentCache();
    }

    private static synchronized void createDocumentCache() {
        if (cachedDocuments == null) {
            cachedDocuments = new ConcurrentLRUCache<>(Duration.ofMinutes(2), CACHE_SIZE);
        }
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
        FeaturedDocument cachedDocument = cachedDocuments.get(baseUri);
        if (cachedDocument == null) {
            html = doc.outerHtml();
            cachedDocuments.put(baseUri, new FeaturedDocument(doc));
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

        FeaturedDocument doc = cachedDocuments.get(baseUri);

        int htmlLen = in.readInt();
        if (doc == null) {
            byte[] html = new byte[htmlLen];
            in.readFully(html);
            if (html.length < NIL_DOC_LENGTH) {
                doc = NIL_DOC;
            } else {
                doc = Documents.INSTANCE.parse(new String(html), baseUri);
                cachedDocuments.put(baseUri, doc);
            }
        } else {
            in.skipBytes(htmlLen);
        }

        Element ele;
        if (selector.equals("#root")) {
            ele = doc.unbox();
        } else {
            ele = doc.first(selector);
        }
        if (ele == null) {
            ele = NIL_DOC.unbox();
        }

        dom = ValueDom.get(ele);
    }
}
