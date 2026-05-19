package com.example.calendar;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.servlet.ServletContainer;

public class Application {
    
    public static void main(String[] args) throws Exception {
        
        Server server = new Server(9080);
        
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        
        // Configure Jersey servlet for REST endpoints
        // The CalendarApplication class will register the AuthorizationFilter
        ServletHolder jerseyServlet = context.addServlet(ServletContainer.class, "/api/*");
        jerseyServlet.setInitOrder(0);
        jerseyServlet.setInitParameter("jakarta.ws.rs.Application", "com.example.calendar.CalendarApplication");
        
        server.setHandler(context);
        
        System.out.println("Starting Calendar Service on http://localhost:9080");
        
        server.start();
        server.join();
    }
}
