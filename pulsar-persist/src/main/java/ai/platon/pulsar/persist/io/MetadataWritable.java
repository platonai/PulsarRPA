package ai.platon.pulsar.persist.io;

import ai.platon.pulsar.persist.metadata.MultiMetadata;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collection;

/**
 * Created by vincent on 17-3-15.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
public class MetadataWritable implements Writable {
    private MultiMetadata metadata;

    public MetadataWritable() {
        this.metadata = new MultiMetadata();
    }

    public MetadataWritable(MultiMetadata metadata) {
        this.metadata = metadata;
    }

    public MultiMetadata get() {
        return metadata;
    }

    public final void write(DataOutput out) throws IOException {
        Collection<String> names = metadata.names();

        out.writeInt(names.size());
        for (String name : names) {
            Collection<String> values = metadata.getNonNullValues(name);

            Text.writeString(out, name);
            out.writeInt(values.size());
            for (String value : values) {
                Text.writeString(out, value);
            }
        }
    }

    public final void readFields(DataInput in) throws IOException {
        int nameCount = in.readInt();
        for (int i = 0; i < nameCount; i++) {
            String name = Text.readString(in);
            int valueCount = in.readInt();
            for (int j = 0; j < valueCount; j++) {
                metadata.put(name, Text.readString(in));
            }
        }
    }
}
