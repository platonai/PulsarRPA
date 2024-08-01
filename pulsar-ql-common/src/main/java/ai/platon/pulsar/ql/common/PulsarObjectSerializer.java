package ai.platon.pulsar.ql.common;

import ai.platon.pulsar.ql.common.io.ValueDomWritable;
import ai.platon.pulsar.ql.common.types.ValueDom;
import org.apache.hadoop.io.DataInputBuffer;
import org.apache.hadoop.io.DataOutputBuffer;
import org.h2.api.ErrorCode;
import org.h2.api.JavaObjectSerializer;
import org.h2.message.DbException;

public class PulsarObjectSerializer implements JavaObjectSerializer {

    @Override
    public byte[] serialize(Object obj) throws Exception {
        if (obj instanceof org.jsoup.nodes.Element) {
            org.jsoup.nodes.Element ele = (org.jsoup.nodes.Element) obj;
            obj = ValueDom.get(ele);
        }

        if (obj instanceof ValueDom) {
            ValueDom dom = (ValueDom) obj;

            DataOutputBuffer buffer = new DataOutputBuffer(1024);
            buffer.writeInt(ValueDom.type);
            new ValueDomWritable(dom).write(buffer);

            // Make a trace who is calling this method
//            if (buffer.size() > 0) throw new RuntimeException("Throw from here");

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

        if (type == ValueDom.type) {
            ValueDomWritable writable = new ValueDomWritable();
            writable.readFields(in);
            return writable.get();
        } else {
            throw DbException.get(ErrorCode.DESERIALIZATION_FAILED_1, "Unknown custom type #" + type);
        }
    }
}
