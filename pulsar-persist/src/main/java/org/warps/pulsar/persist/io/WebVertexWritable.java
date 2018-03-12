package org.warps.pulsar.persist.io;

import org.apache.gora.util.IOUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.BooleanWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.warps.pulsar.persist.WebPage;
import org.warps.pulsar.persist.gora.generated.GWebPage;
import org.warps.pulsar.persist.graph.WebVertex;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * Created by vincent on 16-12-30.
 * Copyright @ 2013-2016 Warpspeed Information. All rights reserved
 */
public class WebVertexWritable implements Writable {
    private Configuration conf;
    private WebVertex vertex = new WebVertex();

    public WebVertexWritable() {
    }

    public WebVertexWritable(WebVertex vertex, Configuration conf) {
        this.conf = conf;
        this.vertex = vertex;
    }

    public WebVertex get() {
        return vertex;
    }

    @Override
    public void write(DataOutput output) throws IOException {
        Text.writeString(output, vertex.getUrl());
        BooleanWritable hasWebPage = new BooleanWritable(vertex.hasWebPage());
        hasWebPage.write(output);
        if (hasWebPage.get()) {
            IOUtils.serialize(conf, output, vertex.getWebPage().unbox(), GWebPage.class);
        }
    }

    @Override
    public void readFields(DataInput input) throws IOException {
        String url = Text.readString(input);
        BooleanWritable hasWebPage = new BooleanWritable();
        hasWebPage.readFields(input);

        WebPage page;
        if (hasWebPage.get()) {
            page = WebPage.box(url, IOUtils.deserialize(conf, input, null, GWebPage.class));
        } else {
            // TODO: is it right to create a new web page?
            page = WebPage.newWebPage(url);
        }

        this.vertex = new WebVertex(page);
    }
}
