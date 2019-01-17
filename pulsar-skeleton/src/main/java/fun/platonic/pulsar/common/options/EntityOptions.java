package fun.platonic.pulsar.common.options;

import com.beust.jcommander.DynamicParameter;
import com.beust.jcommander.Parameter;
import fun.platonic.pulsar.common.config.Params;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by vincent on 17-3-18.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
/**
 * @deprecated User Web SQL instead
 * */
@Deprecated
public class EntityOptions extends PulsarOptions {
    @Parameter(names = {"-en", "--entity-name"}, description = "The entity name.")
    private String name;
    @Parameter(names = {"-er", "--entity-root"}, description = "The entity's container path.")
    private String root;
    @DynamicParameter(names = {"-F"}, description = "Pulsar field extractors to extract sub entity fields")
    private Map<String, String> cssRules = new HashMap<>();
    @DynamicParameter(names = {"-X"}, description = "XPath selectors")
    private Map<String, String> xpathRules = new HashMap<>();
    @DynamicParameter(names = {"-R"}, description = "Regex selectors")
    private Map<String, String> regexRules = new HashMap<>();

    private CollectionOptions collectionOptions = new CollectionOptions();

    public EntityOptions() {
        this.addObjects(this, collectionOptions);
    }

    public EntityOptions(String args) {
        super(args);
        this.addObjects(this, collectionOptions);
    }

    public static EntityOptions parse(String args) {
        EntityOptions options = new EntityOptions(args);
        options.parse();
        return options;
    }

    @Nonnull
    public static Builder newBuilder() {
        return new Builder();
    }

    public String getName() {
        return name == null ? "" : name;
    }

    public String getRoot() {
        return root == null ? ":root" : root;
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
        return !cssRules.isEmpty() || !xpathRules.isEmpty() || !regexRules.isEmpty() || collectionOptions.hasRules();
    }

    public CollectionOptions getCollectionOptions() {
        return collectionOptions;
    }

    public Params getParams() {
        Map<String, Object> fieldsParams = cssRules.entrySet().stream()
                .map(e -> "-F" + e.getKey() + "=" + e.getValue())
                .collect(Collectors.toMap(Object::toString, v -> ""));

        fieldsParams.putAll(xpathRules.entrySet().stream()
                .map(e -> "-X" + e.getKey() + "=" + e.getValue())
                .collect(Collectors.toMap(Object::toString, v -> "")));

        fieldsParams.putAll(regexRules.entrySet().stream()
                .map(e -> "-R" + e.getKey() + "=" + e.getValue())
                .collect(Collectors.toMap(Object::toString, v -> "")));

        return Params.of(
                "-en", name,
                "-er", root
        )
                .filter(p -> p.getValue() != null)
                .filter(p -> !p.getValue().toString().isEmpty())
                .merge(Params.of(fieldsParams))
                .merge(collectionOptions.getParams());
    }

    @Override
    public String toString() {
        return getParams().withKVDelimiter(" ").formatAsLine().replaceAll("\\s+", " ");
    }

    public static class Builder {
        private int i = 1;
        private EntityOptions options = new EntityOptions();

        public Builder name(String name) {
            options.name = name;
            return this;
        }

        public Builder root(String root) {
            options.root = root;
            return this;
        }

        public Builder css(String css) {
            return css("_" + i++, css);
        }

        public Builder css(String... csss) {
            for (String css : csss) {
                css(css);
            }
            return this;
        }

        public Builder css(String name, String css) {
            options.cssRules.put(name, css);
            return this;
        }

        public Builder xpath(String xpath) {
            return xpath("_" + i++, xpath);
        }

        public Builder xpath(String... xpaths) {
            for (String xpath : xpaths) {
                xpath(xpath);
            }
            return this;
        }

        public Builder xpath(String name, String xpath) {
            options.xpathRules.put(name, xpath);
            return this;
        }

        public Builder re(String regex) {
            return re("_" + i++, regex);
        }

        public Builder re(String... regexes) {
            for (String regex : regexes) {
                re(regex);
            }
            return this;
        }

        public Builder re(String name, String regex) {
            options.regexRules.put(name, regex);
            return this;
        }

        public Builder c_name(String name) {
            options.collectionOptions.setName(name);
            return this;
        }

        public Builder c_root(String css) {
            options.collectionOptions.setRoot(css);
            return this;
        }

        public Builder c_item(String css) {
            options.collectionOptions.setItem(css);
            return this;
        }

        public Builder c_css(String css) {
            return c_css("_" + i++, css);
        }

        public Builder c_css(String... csss) {
            for (String css : csss) {
                c_css(css);
            }
            return this;
        }

        public Builder c_css(String name, String css) {
            options.collectionOptions.getCssRules().put(name, css);
            return this;
        }

        public Builder c_xpath(String xpath) {
            return c_xpath(xpath, xpath);
        }

        public Builder c_xpath(String... xpaths) {
            for (String xpath : xpaths) {
                c_xpath(xpath);
            }
            return this;
        }

        public Builder cxpath(String name, String xpath) {
            options.collectionOptions.getXpathRules().put(name, xpath);
            return this;
        }

        public Builder c_re(String regex) {
            return c_re("_" + i++, regex);
        }

        public Builder c_re(String... regexes) {
            for (String regex : regexes) {
                c_re(regex);
            }
            return this;
        }

        public Builder c_re(String name, String regex) {
            options.collectionOptions.getRegexRules().put(name, regex);
            return this;
        }

        public EntityOptions build() {
            return options;
        }

        public <T extends Builder> T as(T o) {
            if (o == this) return o;
            throw new ClassCastException();
        }
    }
}
