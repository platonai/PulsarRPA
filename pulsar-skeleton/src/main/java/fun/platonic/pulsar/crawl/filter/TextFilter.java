package fun.platonic.pulsar.crawl.filter;

import com.google.gson.annotations.Expose;
import fun.platonic.pulsar.common.StringUtil;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

public class TextFilter {
    public static final String SEPERATORS = ",， ";

    @Expose
    private String contains;
    @Expose
    private String containsAny;
    @Expose
    private String notContains;
    @Expose
    private String containsNone;

    private boolean splitted = false;
    private String[] _contains;
    private String[] _containsAny;
    private String[] _notContains;
    private String[] _containsNone;

    public TextFilter() {

    }

    public boolean test(String text) {
        buildCache();

        if (!ArrayUtils.isEmpty(_contains) && !StringUtil.contains(text, _contains)) {
            System.out.println("!contains");
            return false;
        }

        if (!ArrayUtils.isEmpty(_containsAny) && StringUtil.containsNone(text, _containsAny)) {
            System.out.println("containsNone");
            return false;
        }

        if (!ArrayUtils.isEmpty(_notContains) && StringUtil.contains(text, _notContains)) {
            System.out.println("contains");
            return false;
        }

        if (!ArrayUtils.isEmpty(_containsNone) && StringUtil.containsAny(text, _containsNone)) {
            System.out.println("containsAny");
            return false;
        }

        return true;
    }

    private void buildCache() {
        if (!splitted) {
            if (contains != null) _contains = StringUtils.split(contains, SEPERATORS);
            if (containsAny != null) _containsAny = StringUtils.split(containsAny, SEPERATORS);
            if (notContains != null) _notContains = StringUtils.split(notContains, SEPERATORS);
            if (containsNone != null) _containsNone = StringUtils.split(containsNone, SEPERATORS);
            splitted = true;
        }
    }

    public String getContains() {
        return contains;
    }

    public void setContains(String contains) {
        this.contains = contains;
    }

    public String getContainsAny() {
        return containsAny;
    }

    public void setContainsAny(String containsAny) {
        this.containsAny = containsAny;
    }

    public String getNotContains() {
        return notContains;
    }

    public void setNotContains(String notContains) {
        this.notContains = notContains;
    }

    public String getContainsNone() {
        return containsNone;
    }

    public void setContainsNone(String containsNone) {
        this.containsNone = containsNone;
    }

    public String toString() {
        return "\n\tcontains : " + contains + "\n\tcontainsAny : "
                + containsAny + "\n\tnotContains : " + notContains + "\n\tcontainsNone : " + containsNone;
    }
}
