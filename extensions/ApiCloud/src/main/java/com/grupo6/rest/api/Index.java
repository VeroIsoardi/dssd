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
 * Controller class
 */
public class Index extends AbstractIndex {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(Index.class.getName());
    private static final String PAGE_PARAM = "page";
    private static final String LIMIT_PARAM = "limit";
    private static final String PROJECT_ID_PARAM = "projectId";
    private static final String API_BASE_URL = "https://dssd-api.makitech.com.ar/api/v1";

    // Thread-safe storage for request-scoped parameters
    private static final ThreadLocal<Integer> PAGE = new ThreadLocal<>();
    private static final ThreadLocal<Integer> LIMIT = new ThreadLocal<>();
    private static final ThreadLocal<String> PROJECT_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> EMAIL = new ThreadLocal<>();
    private static final ThreadLocal<String> PASSWORD = new ThreadLocal<>();

    /**
     * Ensure request is valid
     *
     * @param request the HttpRequest
     */
    @Override
    public void validateInputParameters(HttpServletRequest request) {
        // Params are optional and independent: if provided and valid (>0 integers), store them; otherwise ignore.
        final String pageRaw = request.getParameter(PAGE_PARAM);
        final String limitRaw = request.getParameter(LIMIT_PARAM);
        final String projectIdRaw = request.getParameter(PROJECT_ID_PARAM);

        if (pageRaw != null) {
            try {
                int page = Integer.parseInt(pageRaw);
                if (page > 0) {
                    PAGE.set(page);
                } // else ignore invalid value
            } catch (NumberFormatException ex) {
                // ignore invalid value
            }
        }

        if (limitRaw != null) {
            try {
                int limit = Integer.parseInt(limitRaw);
                if (limit > 0) {
                    LIMIT.set(limit);
                } // else ignore invalid value
            } catch (NumberFormatException ex) {
                // ignore invalid value
            }
        }

        if (projectIdRaw != null && !projectIdRaw.isBlank()) {
            PROJECT_ID.set(projectIdRaw);
        }

        // Read email and password from request body
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
                if (bodyJson.has("password")) {
                    PASSWORD.set(bodyJson.get("password").asText());
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Could not read email/password from request body: {}", e.getMessage());
        }
    }

    /**
     * Login to the external API and obtain a bearer token
     *
     * @param email User email
     * @param password User password
     * @return Bearer token
     * @throws Exception if login fails
     */
    private String login(String email, String password) throws Exception {
        LOGGER.info("Attempting to login with email: {}", email);
        
        // Create JSON body for login request
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
        
        // Execute login request
        HttpResponse<String> response = client.send(loginRequest, HttpResponse.BodyHandlers.ofString());
        int status = response.statusCode();
        String body = response.body();
        
        LOGGER.info("Login API response - Status: {}", status);
        
        if (status < 200 || status >= 300) {
            throw new RuntimeException(format("Login failed with status %d: %s", status, body));
        }
        
        // Parse response to get token
        JsonNode responseData = getMapper().readTree(body);
        String token = responseData.get("token").asText();
        
        LOGGER.info("Login successful, token obtained");
        return token;
    }

    /**
     * Execute business logic
     *
     * @param context
     * @return Result
     */
    @Override
    protected Result execute(RestAPIContext context) {
        // Get email and password from request body (stored in ThreadLocal)
        String email = EMAIL.get();
        String password = PASSWORD.get();
        String bearerToken = null;

        // Obtain bearer token via login
        if (email != null && !email.isBlank() && password != null && !password.isBlank()) {
            try {
                bearerToken = login(email, password);
            } catch (Exception e) {
                LOGGER.error("Failed to login and obtain bearer token", e);
                throw new RuntimeException("Failed to authenticate with external API", e);
            }
        } else {
            LOGGER.error("Email or password not configured in configuration.properties");
            throw new RuntimeException("API credentials not configured");
        }

        Integer page = PAGE.get();
        Integer limit = LIMIT.get();
        String projectId = PROJECT_ID.get();
        LOGGER.info("Page: {}", page);
        LOGGER.info("Limit: {}", limit);
        LOGGER.info("Project ID: {}", projectId);

        
        try {
            StringBuilder url = new StringBuilder(API_BASE_URL + "/tasks");
            String sep = "?";
            if (page != null) {
                url.append(sep).append("page=").append(page);
                sep = "&";
            }
            

            if (limit != null) {
                url.append(sep).append("limit=").append(limit);
                sep = "&";
            }



            if (projectId != null && !projectId.isBlank()) {
                url.append(sep).append("projectId=").append(projectId);
            }

            URI uri = URI.create(url.toString());

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();

            // Build request with Authorization header
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(Duration.ofSeconds(10))
                    .GET();
            
            if (bearerToken != null && !bearerToken.isBlank()) {
                reqBuilder.header("Authorization", "Bearer " + bearerToken);
            }
            
            HttpRequest request = reqBuilder.build();

            // Execute request
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            String body = response.body();
            
            LOGGER.info("External API response - Status: {}, URL: {}", status, uri);

            if (status < 200 || status >= 300) {
                throw new RuntimeException(format("External API returned status %d: %s", status, body));
            }

            // Parse response as JSON
            JsonNode data = getMapper().readTree(body);
            
            return Result.builder()
                    .data(data.get("data"))
                    .total(data.get("total").asInt())
                    .page(data.get("page").asInt())
                    .limit(data.get("limit").asInt())
                    .build();
                    
        } catch (Exception e) {
            LOGGER.error("Failed to fetch projects from external API", e);
            throw new RuntimeException("Failed to fetch projects from external API", e);
        } finally {
            // Clean up ThreadLocals
            PAGE.remove();
            LIMIT.remove();
            EMAIL.remove();
            PASSWORD.remove();
        }



    }
}