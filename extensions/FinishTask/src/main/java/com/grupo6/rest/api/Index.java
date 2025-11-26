package com.grupo6.rest.api;

import com.grupo6.rest.api.dto.Result;

import org.bonitasoft.web.extension.rest.RestAPIContext;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import javax.servlet.http.HttpServletRequest;

import static java.lang.String.format;

/**
 * Finish task against external cloud API.
 * Body must include: taskId, emailCloud, password. Optional: email, name.
 */
public class Index extends AbstractIndex {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(Index.class.getName());
    private static final String API_BASE_URL = "https://dssd-api.makitech.com.ar/api/v1";

    // Thread-local storage for request-scoped parameters
    private static final ThreadLocal<String> TASK_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> EMAIL_CLOUD = new ThreadLocal<>();
    private static final ThreadLocal<String> PASSWORD = new ThreadLocal<>();
    private static final ThreadLocal<String> EMAIL = new ThreadLocal<>();
    private static final ThreadLocal<String> NAME = new ThreadLocal<>();

    @Override
    public void validateInputParameters(HttpServletRequest request) {
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
                if (bodyJson.has("taskId")) {
                    TASK_ID.set(bodyJson.get("taskId").asText());
                }
                if (bodyJson.has("emailCloud")) {
                    EMAIL_CLOUD.set(bodyJson.get("emailCloud").asText());
                }
                if (bodyJson.has("password")) {
                    PASSWORD.set(bodyJson.get("password").asText());
                }
                if (bodyJson.has("email")) {
                    EMAIL.set(bodyJson.get("email").asText());
                }
                if (bodyJson.has("name")) {
                    NAME.set(bodyJson.get("name").asText());
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
        String taskId = TASK_ID.get();
        String emailCloud = EMAIL_CLOUD.get();
        String password = PASSWORD.get();
        String email = EMAIL.get();
        String name = NAME.get();

        LOGGER.info("Finish task request: taskId={}, emailCloud={}, email={}, name={}", taskId, emailCloud, email, name);

        if (taskId == null || taskId.isBlank()) {
            throw new RuntimeException("Missing taskId in request body");
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
            // Build URL to finish the task. Endpoint: POST /tasks/{taskId}/finish
            URI uri = URI.create(API_BASE_URL + "/tasks/" + taskId + "/finish");

            String requestBody;
            if ((email != null && !email.isBlank()) || (name != null && !name.isBlank())) {
                requestBody = format("{\"email\":\"%s\",\"name\":\"%s\"}",
                        email == null ? "" : email,
                        name == null ? "" : name);
            } else {
                requestBody = "{}";
            }

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();

            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody));

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

            JsonNode responseJson = getMapper().readTree(body);
            String message = responseJson.has("message") ? responseJson.get("message").asText() : "";

            return Result.builder()
                    .message(message)
                    .build();

        } catch (Exception e) {
            LOGGER.error("Failed to finish task in external API", e);
            throw new RuntimeException("Failed to finish task in external API", e);
        } finally {
            TASK_ID.remove();
            EMAIL_CLOUD.remove();
            PASSWORD.remove();
            EMAIL.remove();
            NAME.remove();
        }
    }
}