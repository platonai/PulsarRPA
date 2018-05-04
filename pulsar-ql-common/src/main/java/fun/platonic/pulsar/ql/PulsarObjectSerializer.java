package fun.platonic.pulsar.ql;

import fun.platonic.pulsar.ql.types.ValueDom;
import fun.platonic.pulsar.ql.types.ValueURI;
import org.apache.commons.lang3.StringUtils;
import org.h2.api.ErrorCode;
import org.h2.api.JavaObjectSerializer;
import org.h2.message.DbException;
import org.h2.value.Value;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;

import java.nio.ByteBuffer;

public class PulsarObjectSerializer implements JavaObjectSerializer {
    @Override
    public byte[] serialize(Object obj) throws Exception {
        if (obj instanceof ValueDom) {
            Value value = (Value) obj;
            byte[] source = value.getBytesNoCopy();
            ByteBuffer buffer = ByteBuffer.allocate(Integer.SIZE + Integer.SIZE + source.length + 1);
            buffer.putInt(value.getType());
            buffer.putInt(source.length);
            buffer.put(source);
            return buffer.array();
        } else {
            throw DbException.get(ErrorCode.SERIALIZATION_FAILED_1);
        }
    }

    @Override
    public Object deserialize(byte[] bytes) throws Exception {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);

        int type = buffer.getInt();
        int len = buffer.getInt();
        String str = new String(buffer.array(), buffer.arrayOffset() + buffer.position(), len);
        if (type == ValueDom.type) {
            str = StringUtils.stripStart(str, " \t\n");
            if (StringUtils.startsWithIgnoreCase(str, "<html")) {
                return ValueDom.getOrNil(Jsoup.parse(str));
            }

            Elements children = Jsoup.parseBodyFragment(str).body().children();
            return ValueDom.getOrNil(children.first());
        } else if (type == ValueURI.type) {
            return ValueURI.get(str);
        } else {
            throw DbException.get(ErrorCode.DESERIALIZATION_FAILED_1, "Unknown custom type");
        }
    }
}
