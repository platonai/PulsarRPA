package ai.platon.pulsar.rest.filters;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;

/**
 * Cross domain support
 * See: http://www.codingpedia.org/ama/how-to-add-cors-support-on-the-server-side-in-java-with-jersey/
 * */
public class CORSResponseFilter implements ContainerResponseFilter {

  public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
    MultivaluedMap<String, Object> headers = responseContext.getHeaders();

    headers.add("Access-Control-Allow-Origin", "*");
    //headers.add("Access-Control-Allow-Origin", "http://podcastpedia.org"); //allows CORS requests only coming from podcastpedia.org
    headers.add("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT, HEAD, OPTIONS");
    headers.add("Access-Control-Allow-Headers", "X-Requested-With, Content-Type, X-Codingpedia");
  }
}
