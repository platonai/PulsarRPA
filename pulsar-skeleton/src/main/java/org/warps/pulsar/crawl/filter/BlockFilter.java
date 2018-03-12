package org.warps.pulsar.crawl.filter;

import com.google.gson.annotations.Expose;
import org.apache.commons.collections4.CollectionUtils;
import org.w3c.dom.Node;
import org.warps.pulsar.common.DomUtil;

import java.util.Set;

public class BlockFilter {
    @Expose
    private Set<String> allow;

    @Expose
    private Set<String> disallow;

    public BlockFilter() {

    }

    public boolean isDisallowed(Node node) {
        // TODO : use real css selector
        Set<String> simpleSelectors = DomUtil.getSimpleSelectors(node);

        // System.out.println("simpleSelectors : " + simpleSelectors);

        return !CollectionUtils.isEmpty(disallow)
                && CollectionUtils.containsAny(disallow, simpleSelectors);

    }

    public boolean isAllowed(Node node) {
        Set<String> simpleSelectors = DomUtil.getSimpleSelectors(node);

        return CollectionUtils.isEmpty(allow)
                || CollectionUtils.containsAny(allow, simpleSelectors);

    }

    public Set<String> getAllow() {
        return allow;
    }

    public void setAllow(Set<String> allow) {
        this.allow = allow;
    }

    public Set<String> getDisallow() {
        return disallow;
    }

    public void setDisallow(Set<String> disallow) {
        this.disallow = disallow;
    }

    public String toString() {
        return "\n\tallow" + allow + "\n\tdisallow" + disallow;
    }
}
