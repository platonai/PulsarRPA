package fun.platonic.pulsar.ql.io;

import fun.platonic.pulsar.dom.Documents;
import fun.platonic.pulsar.dom.FeaturedDocument;
import fun.platonic.pulsar.ql.types.ValueDom;
import org.apache.hadoop.io.Writable;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class ValueDomWritable implements Writable {

    public static int CACHE_SIZE = 1000;
    private static FeaturedDocument NIL_DOC = FeaturedDocument.Companion.getNIL();
    private static int NIL_DOC_LENGTH = FeaturedDocument.Companion.getNIL_DOC_LENGTH();

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

        out.writeBytes(doc.baseUri());
        out.write('\n'); // make a line

        out.writeBytes(ele.cssSelector());
        out.write('\n'); // make a line

        String html = doc.outerHtml();
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

        // System.out.println(selector);

        FeaturedDocument doc;

        int htmlLen = in.readInt();
        byte[] html = new byte[htmlLen];
        in.readFully(html);
        doc = Documents.INSTANCE.parse(new String(html), baseUri);

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
