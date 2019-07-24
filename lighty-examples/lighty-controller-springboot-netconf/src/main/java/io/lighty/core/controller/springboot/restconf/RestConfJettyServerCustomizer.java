package io.lighty.core.controller.springboot.restconf;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.opendaylight.restconf.nb.rfc8040.RestconfApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.jetty.JettyServerCustomizer;
import org.springframework.stereotype.Component;

@Component
public class RestConfJettyServerCustomizer implements JettyServerCustomizer {
    private static final Logger LOG = LoggerFactory.getLogger(RestConfJettyServerCustomizer.class);

    @Value("${restconf.contextPath}")
    private String contextPath;

    @Autowired
    private RestconfApplication restconfApplication;

    @Override
    public void customize(Server server) {
        LOG.info("Creating restconf application handler");
        ServletContainer container = new ServletContainer(ResourceConfig.forApplication(restconfApplication));
        ServletHolder holder = new ServletHolder(container);
        ServletContextHandler restConfHandler = new ServletContextHandler(null, contextPath, true, false);
        restConfHandler.addServlet(holder, "/*");

        LOG.info("Append restconf application handler to jetty handler collection.");
        HandlerCollection handlerCollection = new HandlerCollection();
        handlerCollection.setHandlers(new Handler[]{restConfHandler, server.getHandler()});
        server.setHandler(handlerCollection);
    }

}
