package fun.platonic.pulsar.crawl.scoring.io;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import fun.platonic.pulsar.crawl.scoring.NamedScoreVector;
import fun.platonic.pulsar.crawl.scoring.ScoreVector;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * Created by vincent on 17-4-20.
 * Copyright @ 2013-2017 Warpspeed Information. All rights reserved
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
