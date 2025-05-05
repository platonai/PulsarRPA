package ai.platon.pulsar.skeleton.common.proxy

import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.proxy.ProxyParser
import ai.platon.pulsar.common.proxy.impl.ProxyVendorLoader

class UniversalProxyVendorLoader(conf: ImmutableConfig): ProxyVendorLoader(conf) {
    override var parser: ProxyParser? = UniversalProxyParser()
}
