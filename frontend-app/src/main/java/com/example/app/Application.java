package com.example.app;

import com.example.app.filters.AuthorizationFilter;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.util.resource.Resource;
import org.glassfish.jersey.servlet.ServletContainer;

import jakarta.servlet.DispatcherType;
import java.util.EnumSet;

public class Application {
    
    public static void main(String[] args) throws Exception {
        Server server = new Server(9000);
        
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        
        // Add authorization filter to protect resources
        FilterHolder authFilter = new FilterHolder(new AuthorizationFilter());
        context.addFilter(authFilter, "/*", EnumSet.of(DispatcherType.REQUEST));
        
        // Configure Jersey servlet for REST endpoints
        ServletHolder jerseyServlet = context.addServlet(ServletContainer.class, "/api/*");
        jerseyServlet.setInitOrder(0);
        jerseyServlet.setInitParameter("jersey.config.server.provider.packages", "com.example.app.resources");
        
        // Serve static content from classpath (webapp resources packaged in JAR)
        Resource webappResource = Resource.newClassPathResource("webapp");
        if (webappResource != null && webappResource.exists()) {
            context.setBaseResource(webappResource);
            ServletHolder defaultServlet = context.addServlet(org.eclipse.jetty.servlet.DefaultServlet.class, "/");
            defaultServlet.setInitParameter("dirAllowed", "false");
        } else {
            System.err.println("WARNING: webapp resources not found in classpath");
        }
        
        server.setHandler(context);
        
        System.out.println("Starting server on http://localhost:9000");
        
        server.start();
        server.join();
    }
}

