package ai.platon.pulsar.ql.common;

import ai.platon.pulsar.ql.common.io.ValueDomWritable;
import ai.platon.pulsar.ql.common.types.ValueDom;
import ai.platon.pulsar.ql.common.types.ValueStringJSON;
import org.h2.api.ErrorCode;
import org.h2.api.JavaObjectSerializer;
import org.h2.message.DbException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

public class PulsarObjectSerializer implements JavaObjectSerializer {

    @Override
    public byte[] serialize(Object obj) throws Exception {
        if (obj instanceof org.jsoup.nodes.Element ele) {
            obj = ValueDom.get(ele);
        }

        if (obj instanceof ValueDom dom) {
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
                 DataOutputStream out = new DataOutputStream(baos)) {
                out.writeInt(ValueDom.type);
                new ValueDomWritable(dom).write(out);
                out.flush();
                return baos.toByteArray();
            }
        }

        if (obj instanceof ValueStringJSON json) {
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
                 DataOutputStream out = new DataOutputStream(baos)) {
                out.writeInt(ValueStringJSON.type);
                out.writeUTF(json.getString());
                out.writeUTF(json.getTargetClassName());
                out.flush();
                return baos.toByteArray();
            }
        }

        throw DbException.get(ErrorCode.SERIALIZATION_FAILED_1);
    }

    @Override
    public Object deserialize(byte[] bytes) throws Exception {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            int type = in.readInt();

            if (type == ValueDom.type) {
                ValueDomWritable writable = new ValueDomWritable();
                writable.readFields(in);
                return writable.get();
            }

            if (type == ValueStringJSON.type) {
                String jsonText = in.readUTF();
                String className = in.readUTF();
                return ValueStringJSON.get(jsonText, className);
            }

            throw DbException.get(ErrorCode.DESERIALIZATION_FAILED_1, "Unknown custom type #" + type);
        }
    }
}
