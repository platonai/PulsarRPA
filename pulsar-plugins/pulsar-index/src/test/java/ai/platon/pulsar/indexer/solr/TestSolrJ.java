package ai.platon.pulsar.indexer.solr;

/**
 * Created by vincent on 16-8-1.
 */

import ai.platon.pulsar.common.config.ImmutableConfig;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.NamedList;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * This should include tests against the example solr config
 * <p>
 * This lets us try various SolrServer implementations with the same tests.
 *
 * @since solr 6.1.0
 */
@Ignore("Should start solr server before this test")
public class TestSolrJ {
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private ImmutableConfig conf = new ImmutableConfig();
    private String serverUrl;

    @Before
    public void setup() {
        serverUrl = conf.get("solr.server.url", "http://localhost:8983/solr");
        serverUrl = "http://localhost:8983/solr/gettingstarted";
        LOG.info("Solr server url : " + serverUrl);
    }

    /**
     * query the example
     */
    @Test
    public void testDelete() throws Exception {
        SolrClient client = new HttpSolrClient.Builder(serverUrl).build();

        // Empty the database...
        client.deleteByQuery("*:*");// delete everything!
    }

    /**
     * query the example
     */
    @Test
    public void testQuery() throws Exception {
        SolrClient client = new HttpSolrClient.Builder(serverUrl).build();

        // Empty the database...
        client.deleteByQuery("*:*");// delete everything!

        // Now add something...
        SolrInputDocument doc = new SolrInputDocument();
        String docID = "1112211111";
        doc.addField("id", docID, 1.0f);
        doc.addField("name", "my name!", 1.0f);

        assertEquals(null, doc.getField("foo"));
        assertTrue(doc.getField("name").getValue() != null);

        UpdateResponse upres = client.add(doc);
        // System.out.println( "ADD:"+upres.getResponse() );
        assertEquals(0, upres.getStatus());

        upres = client.commit(true, true);
        // System.out.println( "COMMIT:"+upres.getResponse() );
        assertEquals(0, upres.getStatus());

        upres = client.optimize(true, true);
        // System.out.println( "OPTIMIZE:"+upres.getResponse() );
        assertEquals(0, upres.getStatus());

        SolrQuery query = new SolrQuery();
        query.setQuery("id:" + docID);
        QueryResponse response = client.query(query);

        assertEquals(docID, response.getResults().get(0).getFieldValue("id"));

        // Now add a few docs for facet testing...
        List<SolrInputDocument> docs = new ArrayList<>();
        SolrInputDocument doc2 = new SolrInputDocument();
        doc2.addField("id", "2", 1.0f);
        doc2.addField("inStock", true, 1.0f);
        doc2.addField("price", 2, 1.0f);
        doc2.addField("timestamp_dt", new java.util.Date(), 1.0f);
        docs.add(doc2);
        SolrInputDocument doc3 = new SolrInputDocument();
        doc3.addField("id", "3", 1.0f);
        doc3.addField("inStock", false, 1.0f);
        doc3.addField("price", 3, 1.0f);
        doc3.addField("timestamp_dt", new java.util.Date(), 1.0f);
        docs.add(doc3);
        SolrInputDocument doc4 = new SolrInputDocument();
        doc4.addField("id", "4", 1.0f);
        doc4.addField("inStock", true, 1.0f);
        doc4.addField("price", 4, 1.0f);
        doc4.addField("timestamp_dt", new java.util.Date(), 1.0f);
        docs.add(doc4);
        SolrInputDocument doc5 = new SolrInputDocument();
        doc5.addField("id", "5", 1.0f);
        doc5.addField("inStock", false, 1.0f);
        doc5.addField("price", 5, 1.0f);
        doc5.addField("timestamp_dt", new java.util.Date(), 1.0f);
        docs.add(doc5);

        upres = client.add(docs);
        // System.out.println( "ADD:"+upres.getResponse() );
        assertEquals(0, upres.getStatus());

        upres = client.commit(true, true);
        // System.out.println( "COMMIT:"+upres.getResponse() );
        assertEquals(0, upres.getStatus());

        upres = client.optimize(true, true);
        // System.out.println( "OPTIMIZE:"+upres.getResponse() );
        assertEquals(0, upres.getStatus());

        query = new SolrQuery("*:*");
        query.addFacetQuery("price:[* TO 2]");
        query.addFacetQuery("price:[2 TO 4]");
        query.addFacetQuery("price:[5 TO *]");
        query.addFacetField("inStock");
        query.addFacetField("price");
        query.addFacetField("timestamp_dt");
        query.removeFilterQuery("inStock:true");

        response = client.query(query);
        assertEquals(0, response.getStatus());
        assertEquals(5, response.getResults().getNumFound());
        assertEquals(3, response.getFacetQuery().size());
        assertEquals(2, response.getFacetField("inStock").getValueCount());
        assertEquals(4, response.getFacetField("price").getValueCount());

        // test a second query, test making a copy of the main query
        SolrQuery query2 = query.getCopy();
        query2.addFilterQuery("inStock:true");
        response = client.query(query2);
        assertEquals(1, query2.getFilterQueries().length);
        assertEquals(0, response.getStatus());
        assertEquals(2, response.getResults().getNumFound());
        assertFalse(query.getFilterQueries() == query2.getFilterQueries());

        // sanity check round tripping of params...
        query = new SolrQuery("foo");
        query.addFilterQuery("{!field f=inStock}true");
        query.addFilterQuery("{!term f=name}hoss");
        query.addFacetQuery("price:[* TO 2]");
        query.addFacetQuery("price:[2 TO 4]");

        response = client.query(query);
        assertTrue("echoed params are not a NamedList: " + response.getResponseHeader().get("params").getClass(),
                response.getResponseHeader().get("params") instanceof NamedList);
        NamedList echo = (NamedList) response.getResponseHeader().get("params");
        List values = null;
        assertEquals("foo", echo.get("q"));
        assertTrue("echoed fq is not a List: " + echo.get("fq").getClass(), echo.get("fq") instanceof List);
        values = (List) echo.get("fq");
        assertEquals(2, values.size());
        assertEquals("{!field f=inStock}true", values.get(0));
        assertEquals("{!term f=name}hoss", values.get(1));
        assertTrue("echoed facet.query is not a List: " + echo.get("facet.query").getClass(),
                echo.get("facet.query") instanceof List);
        values = (List) echo.get("facet.query");
        assertEquals(2, values.size());
        assertEquals("price:[* TO 2]", values.get(0));
        assertEquals("price:[2 TO 4]", values.get(1));
    }
}
