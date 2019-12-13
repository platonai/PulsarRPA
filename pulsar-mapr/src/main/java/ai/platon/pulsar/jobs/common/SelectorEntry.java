package ai.platon.pulsar.jobs.common;

import ai.platon.pulsar.common.ScoreVector;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableComparable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * Created by vincent on 17-4-11.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
public class SelectorEntry implements WritableComparable<SelectorEntry> {

    public static final Logger LOG = LoggerFactory.getLogger(SelectorEntry.class);

    private String url;
    private String sortScore;

    public SelectorEntry() {

    }

    public SelectorEntry(String url, int sortScore) {
        this.url = url;
        this.sortScore = new ScoreVector("1", sortScore).toString();
    }

    public SelectorEntry(String url, ScoreVector sortScore) {
        this.url = url;
        this.sortScore = sortScore.toString();
    }

    public SelectorEntry(String url, String sortScore) {
        this.url = url;
        this.sortScore = sortScore;
    }

    public String getUrl() {
        return url;
    }

    public String getSortScore() {
        return sortScore;
    }

    @Override
    public void readFields(DataInput in) throws IOException {
        url = Text.readString(in);
        sortScore = Text.readString(in);
    }

    @Override
    public void write(DataOutput out) throws IOException {
        Text.writeString(out, url);
        Text.writeString(out, sortScore);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + url.hashCode();
        result = prime * result + sortScore.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SelectorEntry)) {
            return false;
        }

        SelectorEntry other = (SelectorEntry) obj;
        return compareTo(other) == 0;
    }

    /**
     * Items with higher score comes first in reduce phrase
     */
    @Override
    public int compareTo(SelectorEntry se) {
        int comp = -sortScore.compareTo(se.sortScore);
        return comp != 0 ? comp : url.compareTo(se.url);
    }
}
