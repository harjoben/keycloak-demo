package com.example.calendar;

import com.example.calendar.filter.AuthorizationFilter;
import com.example.calendar.resource.CalendarResource;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;

import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;

public class CalendarApplication extends ResourceConfig {

    public CalendarApplication() {
        
        // Register resources
        register(CalendarResource.class);
        
        // Register filters
        register(AuthorizationFilter.class);
        
        // Register Jackson for JSON processing
        register(JacksonFeature.class);
        register(JacksonObjectMapperProvider.class);
        
        System.out.println("Calendar Application initialized");
    }

    @Provider
    public static class JacksonObjectMapperProvider implements ContextResolver<ObjectMapper> {
        
        private final ObjectMapper mapper;

        public JacksonObjectMapperProvider() {
           
            mapper = new ObjectMapper();
            
            // Register module for Java 8 date/time support
            mapper.registerModule(new JavaTimeModule());
            
            // Configure to write dates as strings
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            
            // Pretty print JSON
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            
        }

        @Override
        public ObjectMapper getContext(Class<?> type) {
            return mapper;
        }
    }
}
