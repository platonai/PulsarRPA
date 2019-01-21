package fun.platonic.pulsar.persist.storage;

import fun.platonic.pulsar.common.config.MutableConfig;
import fun.platonic.pulsar.persist.EmbedMongoStarter;

import java.io.IOException;
import java.util.Scanner;

import static fun.platonic.pulsar.common.config.CapabilityTypes.GORA_MONGODB_SERVERS;
import static fun.platonic.pulsar.common.config.PulsarConstants.DEFAULT_EMBED_MONGO_SERVER;

public class JvmEmbedMongoStarter {
    public static void main(String[] args) throws IOException {
        MutableConfig conf = new MutableConfig();
        String server = conf.get(GORA_MONGODB_SERVERS, DEFAULT_EMBED_MONGO_SERVER);
        new EmbedMongoStarter().start(server);

        System.out.println("Press any key to exit ...");
        System.in.read();
    }
}
