package fun.platonic.pulsar.ql.io;

import fun.platonic.pulsar.common.ConcurrentLRUCache;
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
            cachedDocuments = new ConcurrentLRUCache<>(Duration.ofMinutes(10), CACHE_SIZE);
        }
    }

    public ValueDom get() {
        return dom;
    }

    @Override
    public void write(DataOutput out) throws IOException {
        Element ele = dom.getElement();
        Document doc = ele.ownerDocument();

        out.writeBytes(doc.baseUri());
        out.write('\n');

        out.writeBytes(ele.cssSelector());
        out.write('\n');

        // TODO: Better solution to reduce memory/network/space consumption
        String html = doc.outerHtml();
        out.writeInt(html.length());
        out.write(html.getBytes());
    }

    @Override
    public void readFields(DataInput in) throws IOException {
        String baseUri = in.readLine();
        String selector = in.readLine();

        FeaturedDocument doc = cachedDocuments.get(baseUri);

        int htmlLen = in.readInt();
        if (doc == null) {
            byte[] html = new byte[htmlLen];
            in.readFully(html);
            doc = Documents.INSTANCE.parse(new String(html), baseUri);
            cachedDocuments.put(baseUri, doc);
        } else {
            in.skipBytes(htmlLen);
        }

        Element ele = doc.first(selector);
        dom = ValueDom.get(ele);
    }
}
