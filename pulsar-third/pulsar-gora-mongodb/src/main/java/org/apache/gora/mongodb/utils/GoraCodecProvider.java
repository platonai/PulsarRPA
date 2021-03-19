package org.apache.gora.mongodb.utils;

import org.apache.avro.util.Utf8;
import org.bson.BsonBinary;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class GoraCodecProvider implements CodecProvider {
    private final Map<Class<?>, Codec<?>> codecs = new HashMap();

    public GoraCodecProvider() {
        this.addCodecs();
    }

    public <T> Codec<T> get(Class<T> clazz, CodecRegistry registry) {
        return (Codec)this.codecs.get(clazz);
    }

    private void addCodecs() {
        addCodec(new Utf8Codec());
        addCodec(new ByteBufferCodec());
    }

    private <T> void addCodec(Codec<T> codec) {
        this.codecs.put(codec.getEncoderClass(), codec);
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else {
            return o != null && this.getClass() == o.getClass();
        }
    }

    public int hashCode() {
        return 0;
    }

    public static class Utf8Codec implements Codec<Utf8> {
        public Utf8Codec() {
        }

        @Override
        public void encode(BsonWriter writer, Utf8 value, EncoderContext encoderContext) {
            writer.writeString(value.toString());
        }

        @Override
        public Utf8 decode(BsonReader reader, DecoderContext decoderContext) {
            return new Utf8(reader.readString());
        }

        @Override
        public Class<Utf8> getEncoderClass() {
            return Utf8.class;
        }
    }

    public static class ByteBufferCodec implements Codec<ByteBuffer> {
        public ByteBufferCodec() {
        }

        public void encode(BsonWriter writer, ByteBuffer value, EncoderContext encoderContext) {
            writer.writeBinaryData(new BsonBinary(value.array()));
        }

        public ByteBuffer decode(BsonReader reader, DecoderContext decoderContext) {
            return ByteBuffer.wrap(reader.readBinaryData().getData());
        }

        public Class<ByteBuffer> getEncoderClass() {
            return ByteBuffer.class;
        }
    }
}
