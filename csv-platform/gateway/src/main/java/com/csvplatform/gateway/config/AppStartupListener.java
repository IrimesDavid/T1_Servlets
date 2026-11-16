package com.csvplatform.gateway.config;

import com.csvplatform.common.config.PropertiesLoader;
import com.csvplatform.common.jobs.JobStore;
import com.csvplatform.gateway.services.helpers.ServiceClient;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.nio.file.Path;
import java.util.Properties;

@WebListener
public class AppStartupListener implements ServletContextListener {
    public static final String JOB_STORE = "jobStore";
    public static final String SERVICE_CLIENT = "serviceClient";

    @Override
    public void contextInitialized(ServletContextEvent event) {
        Properties props = PropertiesLoader.load("application.properties");
        ServletContext ctx = event.getServletContext();

        JobStore jobStore = new JobStore(Path.of(props.getProperty("data.dir")).resolve("jobs"));
        ServiceClient serviceClient = new ServiceClient(
                props.getProperty("analysis.url"),
                props.getProperty("cleaning.url"));

        ctx.setAttribute(JOB_STORE, jobStore);
        ctx.setAttribute(SERVICE_CLIENT, serviceClient);

        ctx.log("Gateway started. data.dir=" + props.getProperty("data.dir")
                + ", analysis.url=" + props.getProperty("analysis.url")
                + ", cleaning.url=" + props.getProperty("cleaning.url"));
    }
}