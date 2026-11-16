package com.csvplatform.analysis.servlets;

import com.csvplatform.analysis.config.AppStartupListener;
import com.csvplatform.analysis.services.Analyzer;
import com.csvplatform.common.jobs.JobStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet(name = "AnalysisServlet", urlPatterns = "/analyze")
public class AnalysisServlet extends HttpServlet {
    private Analyzer analyzer;
    private final ObjectMapper JSON = new ObjectMapper();

    @Override
    public void init(){
        analyzer = new Analyzer((JobStore) getServletContext().getAttribute(AppStartupListener.JOB_STORE));
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException{
        String jobId;
        try {
            jobId = JSON.readTree(request.getInputStream()).path("jobId").asText(null);
        } catch (IOException e) {
            sendResponse(response, 400, "Invalid JSON request");
            return;
        }
        
        try {
            analyzer.analyze(jobId);
            sendResponse(response, 200, "Analysis completed");
        } catch (IllegalArgumentException e) {
            sendResponse(response, 400, e.getMessage());
        } catch (Exception e) {
            sendResponse(response, 500, "Internal server error");
        }
    }

    // we only care about the status this servlet responds with, no relevant content is sent
    private void sendResponse(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("text/plain");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().println(message);
    }
}
