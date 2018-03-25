/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package fun.platonic.pulsar.crawl.parse;

import org.jsoup.nodes.Document;
import fun.platonic.pulsar.persist.HypeLink;
import fun.platonic.pulsar.persist.ParseStatus;

import javax.annotation.Nonnull;
import java.util.ArrayList;

public class ParseResult extends ParseStatus {

    private ArrayList<HypeLink> hypeLinks = new ArrayList<>();
    private Document document;

    public ParseResult() {
        super(ParseStatus.NOTPARSED, ParseStatus.SUCCESS_OK);
    }

    public ParseResult(short majorCode, int minorCode) {
        super(majorCode, minorCode);
    }

    public ParseResult(short majorCode, int minorCode, String message) {
        super(majorCode, minorCode, message);
    }

    @Nonnull
    public static ParseResult failed(int minorCode, String message) {
        return new ParseResult(FAILED, minorCode, message);
    }

    @Nonnull
    public static ParseResult failed(Throwable e) {
        return new ParseResult(FAILED, FAILED_EXCEPTION, e.getMessage());
    }

    public Document getDocument() {
        return document;
    }

    public void setDocument(Document document) {
        this.document = document;
    }

    public ArrayList<HypeLink> getHypeLinks() {
        return hypeLinks;
    }
}
