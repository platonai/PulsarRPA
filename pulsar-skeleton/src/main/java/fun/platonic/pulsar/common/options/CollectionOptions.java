package fun.platonic.pulsar.common.options;

import com.beust.jcommander.DynamicParameter;
import com.beust.jcommander.Parameter;
import fun.platonic.pulsar.common.config.Params;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by vincent on 17-3-18.
 * Copyright @ 2013-2017 Warpspeed Information. All rights reserved
 */
public class CollectionOptions extends PulsarOptions {
    @Parameter(names = {"-cn", "--collection-name"}, description = "The name of the collection")
    private String name;
    @Parameter(names = {"-cr", "--collection-root"}, description = "The path of the collection")
    private String root;
    @Parameter(names = {"-ci", "--collection-item"}, description = "The path of entity fields")
    private String item;
    @DynamicParameter(names = {"-FF"}, description = "Pulsar field extractors to extract sub entity fields")
    private Map<String, String> cssRules = new HashMap<>();
    @DynamicParameter(names = {"-XX"}, description = "XPath selectors")
    private Map<String, String> xpathRules = new HashMap<>();
    @DynamicParameter(names = {"-RR"}, description = "Regex selectors")
    private Map<String, String> regexRules = new HashMap<>();

    public CollectionOptions() {
    }

    public CollectionOptions(String[] args) {
        super(args);
    }

    public String getName() {
        return name == null ? "" : name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRoot() {
        return root == null ? ":root" : root;
    }

    public void setRoot(String css) {
        this.root = css;
    }

    public String getItem() {
        return item == null ? "" : item;
    }

    public void setItem(String css) {
        this.item = css;
    }

    public Map<String, String> getCssRules() {
        return cssRules;
    }

    public Map<String, String> getXpathRules() {
        return xpathRules;
    }

    public Map<String, String> getRegexRules() {
        return regexRules;
    }

    public boolean hasRules() {
        return !cssRules.isEmpty() || !xpathRules.isEmpty() || !regexRules.isEmpty();
    }

    public Params getParams() {
        Map<String, Object> fieldsParams = cssRules.entrySet().stream()
                .map(e -> "-FF" + e.getKey() + "=" + e.getValue())
                .collect(Collectors.toMap(Object::toString, v -> ""));

        fieldsParams.putAll(xpathRules.entrySet().stream()
                .map(e -> "-XX" + e.getKey() + "=" + e.getValue())
                .collect(Collectors.toMap(Object::toString, v -> "")));

        fieldsParams.putAll(regexRules.entrySet().stream()
                .map(e -> "-RR" + e.getKey() + "=" + e.getValue())
                .collect(Collectors.toMap(Object::toString, v -> "")));

        return Params.of(
                "-cn", name,
                "-cr", root,
                "-ci", item
        )
                .filter(p -> p.getValue() != null)
                .filter(p -> !p.getValue().toString().isEmpty())
                .merge(Params.of(fieldsParams));
    }

    @Override
    public String toString() {
        return getParams().withKVDelimiter(" ").formatAsLine().replaceAll("\\s+", " ");
    }
}
