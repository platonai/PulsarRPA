package org.jsoup.parser;

import org.jsoup.parser.HtmlTreeBuilderState;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertArrayEquals;

public class HtmlTreeBuilderStateTest {
    @Test
    public void ensureArraysAreSorted() {
        String[][] arrays = {
            HtmlTreeBuilderState.Constants.InBodyStartToHead,
            HtmlTreeBuilderState.Constants.InBodyStartPClosers,
            HtmlTreeBuilderState.Constants.Headings,
            HtmlTreeBuilderState.Constants.InBodyStartPreListing,
            HtmlTreeBuilderState.Constants.InBodyStartLiBreakers,
            HtmlTreeBuilderState.Constants.DdDt,
            HtmlTreeBuilderState.Constants.Formatters,
            HtmlTreeBuilderState.Constants.InBodyStartApplets,
            HtmlTreeBuilderState.Constants.InBodyStartEmptyFormatters,
            HtmlTreeBuilderState.Constants.InBodyStartMedia,
            HtmlTreeBuilderState.Constants.InBodyStartInputAttribs,
            HtmlTreeBuilderState.Constants.InBodyStartOptions,
            HtmlTreeBuilderState.Constants.InBodyStartRuby,
            HtmlTreeBuilderState.Constants.InBodyStartDrop,
            HtmlTreeBuilderState.Constants.InBodyEndClosers,
            HtmlTreeBuilderState.Constants.InBodyEndAdoptionFormatters,
            HtmlTreeBuilderState.Constants.InBodyEndTableFosters
        };

        for (String[] array : arrays) {
            String[] copy = Arrays.copyOf(array, array.length);
            Arrays.sort(array);
            assertArrayEquals(array, copy);
        }
    }
}
