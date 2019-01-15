package fun.platonic.pulsar.ql;

import fun.platonic.pulsar.ql.io.ValueDomWritable;
import fun.platonic.pulsar.ql.types.ValueDom;
import fun.platonic.pulsar.dom.nodes.NodesKt;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.hadoop.io.DataInputBuffer;
import org.apache.hadoop.io.DataOutputBuffer;
import org.h2.api.ErrorCode;
import org.h2.api.JavaObjectSerializer;
import org.h2.message.DbException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PulsarObjectSerializer implements JavaObjectSerializer {

    public static final Logger LOG = LoggerFactory.getLogger(PulsarObjectSerializer.class);

    private static final DescriptiveStatistics bufferSizeSummarizer = new DescriptiveStatistics();

    @Override
    public byte[] serialize(Object obj) throws Exception {
        if (obj instanceof org.jsoup.nodes.Element) {
            org.jsoup.nodes.Element ele = (org.jsoup.nodes.Element)obj;
            obj = ValueDom.get(ele);

            if (LOG.isDebugEnabled()) {
                LOG.debug("Element {} converted to ValueDom", NodesKt.getUniqueName(ele));
            }
        }

        if (obj instanceof ValueDom) {
            ValueDom dom = (ValueDom) obj;

            double len;
            synchronized (bufferSizeSummarizer) {
                len = bufferSizeSummarizer.getN() > 5 ? bufferSizeSummarizer.getPercentile(80) : 2048;
            }

            if (LOG.isDebugEnabled() && bufferSizeSummarizer.getN() > 5) {
                LOG.debug("DOM summery at 80% quantile: " + len);
            }

            DataOutputBuffer buffer = new DataOutputBuffer((int) len);
            buffer.writeInt(ValueDom.type);
            new ValueDomWritable(dom).write(buffer);

            synchronized (bufferSizeSummarizer) {
                bufferSizeSummarizer.addValue(buffer.size());
            }

            return buffer.getData();
        } else {
            throw DbException.get(ErrorCode.SERIALIZATION_FAILED_1);
        }
    }

    @Override
    public Object deserialize(byte[] bytes) throws Exception {
        DataInputBuffer in = new DataInputBuffer();
        in.reset(bytes, bytes.length);

        int type = in.readInt();

        System.out.println("Deserializing " + type);

        if (type == ValueDom.type) {
            ValueDomWritable writable = new ValueDomWritable();
            writable.readFields(in);
            return writable.get();
        } else {
            throw DbException.get(ErrorCode.DESERIALIZATION_FAILED_1, "Unknown custom type #" + type);
        }
    }
}
