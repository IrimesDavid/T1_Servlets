package com.csvplatform.cleaning.servlets;

import com.csvplatform.cleaning.config.AppStartupListener;
import com.csvplatform.cleaning.services.Cleaner;
import com.csvplatform.common.jobs.JobStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet(name = "CleaningServlet", urlPatterns = "/clean")
public class CleaningServlet extends HttpServlet {
    private Cleaner cleaner;
    private final ObjectMapper JSON = new ObjectMapper();

    @Override
    public void init(){
        cleaner = new Cleaner((JobStore) getServletContext().getAttribute(AppStartupListener.JOB_STORE));
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String jobId;
        try {
            jobId = JSON.readTree(request.getInputStream()).path("jobId").asText(null);
        } catch (IOException e) {
            sendResponse(response, 400, "Invalid JSON request");
            return;
        }

        try {
            cleaner.clean(jobId);
            sendResponse(response, 200, "Cleaning completed");
        } catch (IllegalArgumentException e) {
            sendResponse(response, 400, e.getMessage());
        } catch (Exception e) {
            sendResponse(response, 500, "Internal server error");
        }
    }

    private void sendResponse(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("text/plain");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().println(message);
    }
}
