package com.csvplatform.analysis.config;

import com.csvplatform.common.config.PropertiesLoader;
import com.csvplatform.common.jobs.JobStore;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.nio.file.Path;
import java.util.Properties;

@WebListener
public class AppStartupListener implements ServletContextListener {
    public static final String JOB_STORE = "jobStore";

    @Override
    public void contextInitialized(ServletContextEvent event) {
        Properties p = PropertiesLoader.load("application.properties");
        JobStore store = new JobStore(Path.of(p.getProperty("data.dir")).resolve("jobs"));
        event.getServletContext().setAttribute("jobStore", store);
    }
}