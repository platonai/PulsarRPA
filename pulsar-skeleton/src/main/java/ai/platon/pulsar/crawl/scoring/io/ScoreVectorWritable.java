package ai.platon.pulsar.crawl.scoring.io;

import ai.platon.pulsar.crawl.scoring.NamedScoreVector;
import ai.platon.pulsar.common.ScoreVector;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * Created by vincent on 17-4-20.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
public class ScoreVectorWritable implements Writable {
    private ScoreVector scoreVector;

    public ScoreVectorWritable(ScoreVector scoreVector) {
        this.scoreVector = scoreVector;
    }

    public ScoreVector get() {
        return scoreVector;
    }

    public void write(DataOutput out) throws IOException {
        out.writeInt(scoreVector.getArity());
        Text.writeString(out, scoreVector.toString());
    }

    public void readFields(DataInput in) throws IOException {
        int arity = in.readInt();
        scoreVector = NamedScoreVector.parse(Text.readString(in));
    }
}
