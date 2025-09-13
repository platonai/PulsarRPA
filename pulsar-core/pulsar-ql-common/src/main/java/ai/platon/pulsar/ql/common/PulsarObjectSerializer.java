package ai.platon.pulsar.ql.common;

import ai.platon.pulsar.ql.common.io.ValueDomWritable;
import ai.platon.pulsar.ql.common.types.ValueDom;
import ai.platon.pulsar.ql.common.types.ValueStringJSON;
import org.apache.hadoop.io.DataInputBuffer;
import org.apache.hadoop.io.DataOutputBuffer;
import org.h2.api.ErrorCode;
import org.h2.api.JavaObjectSerializer;
import org.h2.message.DbException;

public class PulsarObjectSerializer implements JavaObjectSerializer {

    @Override
    public byte[] serialize(Object obj) throws Exception {
        if (obj instanceof org.jsoup.nodes.Element ele) {
            obj = ValueDom.get(ele);
        }

        if (obj instanceof ValueDom dom) {
            try(DataOutputBuffer buffer = new DataOutputBuffer(1024)) {
                buffer.writeInt(ValueDom.type);
                new ValueDomWritable(dom).write(buffer);

                return buffer.getData();
            }
        } if (obj instanceof ValueStringJSON json) {
            try(DataOutputBuffer buffer = new DataOutputBuffer(1024)) {
                buffer.writeInt(ValueStringJSON.type);
                buffer.writeUTF(json.getString());
                buffer.writeUTF(json.getTargetClassName());

                return buffer.getData();
            }
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
        } if (type == ValueStringJSON.type) {
            String jsonText = in.readUTF();
            String className = in.readUTF();
            return ValueStringJSON.get(jsonText, className);
        } else {
            throw DbException.get(ErrorCode.DESERIALIZATION_FAILED_1, "Unknown custom type #" + type);
        }
    }
}
