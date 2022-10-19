package ai.platon.pulsar.persist.gora;

import ai.platon.pulsar.persist.gora.generated.GWebPage;
import org.jetbrains.annotations.NotNull;

public interface GWebPageAware {
    @NotNull
    GWebPage getGWebPage();
}
