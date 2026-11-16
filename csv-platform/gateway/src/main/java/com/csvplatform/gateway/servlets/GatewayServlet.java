package com.csvplatform.gateway.servlets;

import com.csvplatform.gateway.config.AppStartupListener;
import com.csvplatform.gateway.services.GatewayService;
import com.csvplatform.gateway.services.helpers.ServiceClient;
import com.csvplatform.common.jobs.JobStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@WebServlet(name = "GatewayServlet", urlPatterns = "/api/*")
@MultipartConfig(maxFileSize = 10 * 1024 * 1024, maxRequestSize = 11 * 1024 * 1024)
public class GatewayServlet extends HttpServlet {
    private GatewayService gateway;
    private final ObjectMapper JSON = new ObjectMapper();

    @Override
    public void init() {
        gateway = new GatewayService(
                (JobStore) getServletContext().getAttribute(AppStartupListener.JOB_STORE),
                (ServiceClient) getServletContext().getAttribute(AppStartupListener.SERVICE_CLIENT));
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = request.getPathInfo();
        if ("/download".equals(path)) {
            download(request, response);
        } else if ("/health".equals(path)) {
            sendJson(response, gateway.health());
        } else {
            sendText(response, 404, "Unknown endpoint");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = request.getPathInfo();
        if ("/upload".equals(path)) {
            upload(request, response);
        } else if ("/clear".equals(path)) {
            clear(request, response);
        } else {
            sendText(response, 404, "Unknown endpoint");
        }
    }

    private void upload(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Part file;
        try {
            file = request.getPart("file");   // fails if the request is not multipart or the file is over 10 MB
        } catch (ServletException | IllegalStateException e) {
            sendText(response, 400, "Expected a multipart upload with a 'file' field of at most 10 MB");
            return;
        }
        if (file == null) {
            sendText(response, 400, "Missing 'file' field");
            return;
        }

        try (InputStream in = file.getInputStream()) {
            sendJson(response, gateway.upload(file.getSubmittedFileName(), in));
        } catch (IllegalArgumentException e) {
            sendText(response, 400, e.getMessage());
        } catch (IllegalStateException e) {
            sendText(response, 500, e.getMessage());   // "Analysis service is unavailable or failed", etc.
        } catch (Exception e) {
            sendText(response, 500, "Internal server error");
        }
    }

    private void download(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Path csv;
        try {
            csv = gateway.cleanedCsv(request.getParameter("jobId"));
        } catch (IllegalArgumentException e) {
            sendText(response, 400, e.getMessage());
            return;
        }

        response.setStatus(200);
        response.setContentType("text/csv");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"cleaned.csv\"");
        Files.copy(csv, response.getOutputStream());
    }

    private void clear(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            gateway.clear(request.getParameter("jobId"));
            sendText(response, 200, "Cleared");
        } catch (IllegalArgumentException e) {
            sendText(response, 400, e.getMessage());
        } catch (Exception e) {
            sendText(response, 500, "Internal server error");
        }
    }

    private void sendJson(HttpServletResponse response, Object body) throws IOException {
        response.setStatus(200);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        JSON.writeValue(response.getWriter(), body);
    }

    private void sendText(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("text/plain");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().println(message);
    }
}