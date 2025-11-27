package com.grupo6.rest.api;

import com.grupo6.rest.api.dto.Result;

import org.bonitasoft.web.extension.rest.RestAPIContext;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;

/**
 * List tasks by user (collaborator) using cloud API.
 * Expects in request body: { "email": "collaborator@example.com", "emailCloud": "apiUser", "password": "apiPass" }
 * The `email` value is sent as query param to `/tasks/collaborator?email=...`.
 * `emailCloud` and `password` are used to login to the external API.
 */
public class Index extends AbstractIndex {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(Index.class.getName());
    private static final String API_BASE_URL = "https://dssd-api.makitech.com.ar/api/v1";

    private static final String PAGE_PARAM = "page";
    private static final String LIMIT_PARAM = "limit";
    private static final ThreadLocal<String> EMAIL = new ThreadLocal<>();
    private static final ThreadLocal<String> EMAIL_CLOUD = new ThreadLocal<>();
    private static final ThreadLocal<String> PASSWORD = new ThreadLocal<>();
    private static final ThreadLocal<Integer> PAGE = new ThreadLocal<>();
    private static final ThreadLocal<Integer> LIMIT = new ThreadLocal<>();

    @Override
    public void validateInputParameters(HttpServletRequest request) {
        // Read optional page/limit from query params
        final String pageRaw = request.getParameter(PAGE_PARAM);
        final String limitRaw = request.getParameter(LIMIT_PARAM);

        if (pageRaw != null) {
            try {
                int page = Integer.parseInt(pageRaw);
                if (page > 0) {
                    PAGE.set(page);
                }
            } catch (NumberFormatException ex) {
                // ignore invalid
            }
        }

        if (limitRaw != null) {
            try {
                int limit = Integer.parseInt(limitRaw);
                if (limit > 0) {
                    LIMIT.set(limit);
                }
            } catch (NumberFormatException ex) {
                // ignore invalid
            }
        }

        // Read email (collaborator) and cloud credentials from request body
        try {
            StringBuilder bodyBuilder = new StringBuilder();
            String line;
            java.io.BufferedReader reader = request.getReader();
            while ((line = reader.readLine()) != null) {
                bodyBuilder.append(line);
            }
            String body = bodyBuilder.toString();
            if (body != null && !body.isEmpty()) {
                JsonNode bodyJson = getMapper().readTree(body);
                if (bodyJson.has("email")) {
                    EMAIL.set(bodyJson.get("email").asText());
                }
                if (bodyJson.has("emailCloud")) {
                    EMAIL_CLOUD.set(bodyJson.get("emailCloud").asText());
                }
                if (bodyJson.has("password")) {
                    PASSWORD.set(bodyJson.get("password").asText());
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Could not read input from request body: {}", e.getMessage());
        }
    }

    private String login(String email, String password) throws Exception {
        LOGGER.info("Attempting to login with email: {}", email);
        String loginBody = format("{\"email\":\"%s\",\"password\":\"%s\"}", email, password);
        URI loginUri = URI.create(API_BASE_URL + "/auth/admin/login");

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        HttpRequest loginRequest = HttpRequest.newBuilder()
                .uri(loginUri)
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(loginBody))
                .build();

        HttpResponse<String> response = client.send(loginRequest, HttpResponse.BodyHandlers.ofString());
        int status = response.statusCode();
        String body = response.body();

        LOGGER.info("Login API response - Status: {}", status);

        if (status < 200 || status >= 300) {
            throw new RuntimeException(format("Login failed with status %d: %s", status, body));
        }

        JsonNode responseData = getMapper().readTree(body);
        String token = responseData.get("token").asText();
        LOGGER.info("Login successful, token obtained");
        return token;
    }

    @Override
    protected Result execute(RestAPIContext context) {
        String email = EMAIL.get(); // collaborator email to query
        String emailCloud = EMAIL_CLOUD.get(); // credentials for login
        String password = PASSWORD.get();

        if (email == null || email.isBlank()) {
            throw new RuntimeException("Missing email in request body");
        }

        String bearerToken = null;
        if (emailCloud != null && !emailCloud.isBlank() && password != null && !password.isBlank()) {
            try {
                bearerToken = login(emailCloud, password);
            } catch (Exception e) {
                LOGGER.error("Failed to login and obtain bearer token", e);
                throw new RuntimeException("Failed to authenticate with external API", e);
            }
        } else {
            LOGGER.error("emailCloud or password not provided");
            throw new RuntimeException("API credentials not configured");
        }

        try {
            String encodedEmail = java.net.URLEncoder.encode(email, java.nio.charset.StandardCharsets.UTF_8);
            StringBuilder url = new StringBuilder(API_BASE_URL + "/tasks/collaborator?email=" + encodedEmail);
            Integer page = PAGE.get();
            Integer limit = LIMIT.get();
            if (page != null) {
                url.append("&page=").append(page);
            }
            if (limit != null) {
                url.append("&limit=").append(limit);
            }
            URI uri = URI.create(url.toString());

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();

            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(Duration.ofSeconds(10))
                    .GET();

            if (bearerToken != null && !bearerToken.isBlank()) {
                reqBuilder.header("Authorization", "Bearer " + bearerToken);
            }

            HttpRequest request = reqBuilder.build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            String body = response.body();

            LOGGER.info("External API response - Status: {}, URL: {}", status, uri);

            if (status < 200 || status >= 300) {
                throw new RuntimeException(format("External API returned status %d: %s", status, body));
            }

            JsonNode data = getMapper().readTree(body);

            return Result.builder()
                    .data(data.get("data"))
                    .total(data.get("total").asInt())
                    .page(data.get("page").asInt())
                    .limit(data.get("limit").asInt())
                    .build();

        } catch (Exception e) {
            LOGGER.error("Failed to fetch tasks from external API", e);
            throw new RuntimeException("Failed to fetch tasks from external API", e);
        } finally {
            EMAIL.remove();
            EMAIL_CLOUD.remove();
            PASSWORD.remove();
            PAGE.remove();
            LIMIT.remove();
        }
    }
}