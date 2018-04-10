package org.h2.ext.pulsar;

import org.h2.expression.Expression;

/**
 * Created by vincent on 17-10-10.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
public interface ExternalTableInterface {

    void setBaseUri(Expression baseUri);

    void setRestrictCss(Expression restrictCss);

    void setOffset(Expression offset);

    void setLimit(Expression limit);

    void setWindowSize(Expression windowSize);

    void setWindowInterval(Expression windowInterval);

    void setAction(Expression action);

    void addUri(String uri);
}
