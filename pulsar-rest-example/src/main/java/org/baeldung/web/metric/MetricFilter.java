package org.baeldung.web.metric;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class MetricFilter implements Filter {

    @Autowired
    private IMetricService metricService;

    @Autowired
    private ICustomActuatorMetricService actMetricService;

    @Override
    public void init(final FilterConfig config) throws ServletException {
        if (metricService == null || actMetricService == null) {
            metricService = (IMetricService) WebApplicationContextUtils.getRequiredWebApplicationContext(config.getServletContext()).getBean("metricService");
            actMetricService = WebApplicationContextUtils.getRequiredWebApplicationContext(config.getServletContext()).getBean(CustomActuatorMetricService.class);
        }
    }

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws java.io.IOException, ServletException {
        final HttpServletRequest httpRequest = ((HttpServletRequest) request);
        final String req = httpRequest.getMethod() + " " + httpRequest.getRequestURI();

        chain.doFilter(request, response);

        final int status = ((HttpServletResponse) response).getStatus();
        metricService.increaseCount(req, status);
        actMetricService.increaseCount(status);
    }

    @Override
    public void destroy() {

    }
}
