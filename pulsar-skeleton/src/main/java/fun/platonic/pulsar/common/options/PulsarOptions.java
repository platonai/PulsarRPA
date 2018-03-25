package fun.platonic.pulsar.common.options;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import fun.platonic.pulsar.common.StringUtil;
import fun.platonic.pulsar.common.config.Parameterized;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by vincent on 17-4-12.
 * Copyright @ 2013-2017 Warpspeed Information. All rights reserved
 */
public class PulsarOptions implements Parameterized {
    public static final Logger LOG = LoggerFactory.getLogger(PulsarOptions.class);

    public static final String DEFAULT_DELIMETER = " ";

    protected boolean expandAtSign = true;
    protected String[] argv;
    protected Set<Object> objects = new HashSet<>();
    protected JCommander jc;

    public PulsarOptions() {
        this.argv = new String[]{};
    }

    public PulsarOptions(String args) {
        this(split(args));
    }

    public PulsarOptions(String[] argv) {
        this.argv = argv;
        for (int i = 0; i < this.argv.length; ++i) {
            // Since space can not appear in dynamic parameters in command line, we use % instead
            this.argv[i] = this.argv[i].replaceAll("%", " ");
        }
    }

    public PulsarOptions(Map<String, String> argv) {
        this(argv.entrySet().stream()
                .map(e -> e.getKey() + DEFAULT_DELIMETER + e.getValue())
                .collect(Collectors.joining(DEFAULT_DELIMETER)));
    }

    @Nonnull
    public static String normalize(String args) {
        return normalize(args, ",");
    }

    @Nonnull
    public static String normalize(String args, String seps) {
        return StringUtils.replaceChars(args, seps, StringUtils.repeat(' ', seps.length()));
    }

    @Nonnull
    public static String[] split(String args) {
        return normalize(args).split("\\s+");
    }

    public boolean getExpandAtSign() {
        return expandAtSign;
    }

    public void setExpandAtSign(boolean expandAtSign) {
        this.expandAtSign = expandAtSign;
    }

    public void setObjects(Object... objects) {
        this.objects.clear();
        this.objects.addAll(Lists.newArrayList(objects));
    }

    public void addObjects(Object... objects) {
        this.objects.addAll(Lists.newArrayList(objects));
    }

    public boolean parse() {
        try {
            doParse();
            return true;
        } catch (Throwable e) {
            LOG.warn(StringUtil.stringifyException(e));
        }

        return false;
    }

    public void parseOrExit() {
        parseOrExit(Sets.newHashSet());
    }

    protected void parseOrExit(Set<Object> objects) {
        try {
            addObjects(objects);
            doParse();

            if (isHelp()) {
                jc.usage();
                System.exit(0);
            }
        } catch (ParameterException e) {
            System.out.println(e.toString());
            System.exit(0);
        }
    }

    private void doParse() {
        if (!objects.contains(this)) {
            objects.add(this);
        }

        if (jc == null) {
            jc = new JCommander(objects);
        }

        jc.setAcceptUnknownOptions(true);
        jc.setAllowParameterOverwriting(true);
//      jc.setAllowAbbreviatedOptions(false);
        jc.setExpandAtSign(expandAtSign);
        // LOG.debug(StringUtils.join(argv, " "));
        jc.parse(argv);
    }

    public boolean isHelp() {
        return false;
    }

    public void usage() {
        jc.usage();
    }

    public String toCmdLine() {
        return getParams().withKVDelimiter(" ").formatAsLine().replaceAll("\\s+", " ");
    }

    public String[] toArgs() {
        return getParams().withKVDelimiter(" ").formatAsLine().replaceAll("\\s+", " ").split("\\s+");
    }

    @Override
    public boolean equals(Object other) {
        return (other instanceof PulsarOptions) && this.toString().equals(other.toString());
    }

    @Override
    public String toString() {
        return StringUtils.join(argv, " ");
    }
}
