package ai.platon.pulsar.jobs.common;

import ai.platon.pulsar.common.ScoreVector;
import ai.platon.pulsar.crawl.scoring.io.ScoreVectorWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.io.WritableComparator;
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

    static {
        WritableComparator.define(SelectorEntry.class, new SelectorEntryComparator());
    }

    private String url;
    private ScoreVector score;
    public SelectorEntry() {

    }

    public SelectorEntry(String url, int score) {
        this.url = url;
        this.score = new ScoreVector("1", score);
    }

    public SelectorEntry(String url, ScoreVector score) {
        this.url = url;
        this.score = score;
    }

    public String getUrl() {
        return url;
    }

    public ScoreVector getScore() {
        return score;
    }

    @Override
    public void readFields(DataInput in) throws IOException {
        url = Text.readString(in);
        ScoreVectorWritable scoreVectorWritable = new ScoreVectorWritable(score);
        scoreVectorWritable.readFields(in);
        score = scoreVectorWritable.get();

//    log.info(url);
//    log.info("readFields : " + score.toString());
    }

    @Override
    public void write(DataOutput out) throws IOException {
        Text.writeString(out, url);
        new ScoreVectorWritable(score).write(out);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + url.hashCode();
        result = prime * result + score.hashCode();
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
        int comp = -score.compareTo(se.score);
        return comp != 0 ? comp : url.compareTo(se.url);
    }

    public static class SelectorEntryComparator extends WritableComparator {
        public SelectorEntryComparator() {
            super(SelectorEntry.class, true);
        }
    }
}
