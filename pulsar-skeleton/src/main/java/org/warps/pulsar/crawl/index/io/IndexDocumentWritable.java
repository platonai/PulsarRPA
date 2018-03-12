package org.warps.pulsar.crawl.index.io;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.VersionMismatchException;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableUtils;
import org.warps.pulsar.crawl.index.IndexDocument;
import org.warps.pulsar.crawl.index.IndexField;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Map;

/**
 * Created by vincent on 17-3-17.
 * Copyright @ 2013-2017 Warpspeed Information. All rights reserved
 */
public class IndexDocumentWritable implements Writable {
    public static final byte VERSION = 2;

    private IndexDocument doc;

    public IndexDocumentWritable() {
        this.doc = new IndexDocument();
    }

    public IndexDocumentWritable(IndexDocument doc) {
        this.doc = doc;
    }

    public IndexDocument get() {
        return doc;
    }

    @Override
    public void readFields(DataInput in) throws IOException {
        byte version = in.readByte();
        if (version != VERSION) {
            throw new VersionMismatchException(VERSION, version);
        }
        int size = WritableUtils.readVInt(in);
        for (int i = 0; i < size; i++) {
            String name = Text.readString(in);
            IndexField field = new IndexField();
            field.readFields(in);
            doc.add(name, field);
            // fields.put(name, field);
        }
        doc.setWeight(in.readFloat());
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeByte(VERSION);
        Map<CharSequence, IndexField> fields = doc.getFields();
        WritableUtils.writeVInt(out, fields.size());
        for (Map.Entry<CharSequence, IndexField> entry : fields.entrySet()) {
            Text.writeString(out, entry.getKey().toString());
            IndexField field = entry.getValue();
            field.write(out);
        }
        out.writeFloat(doc.getWeight());
    }
}
