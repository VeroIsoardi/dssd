package com.grupo6.rest.api;

import org.bonitasoft.web.extension.rest.RestAPIContext;
import com.grupo6.rest.api.dto.Result;
import org.bonitasoft.web.extension.ResourceProvider;
import org.bonitasoft.web.extension.rest.RestApiResponse;
import org.bonitasoft.web.extension.rest.RestApiResponseBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import javax.servlet.http.HttpServletRequest;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class IndexTest {

    // Declare mocks here
    // Mocks are used to simulate external dependencies behavior
    @Mock
    private HttpServletRequest httpRequest;
    @Mock
    private ResourceProvider resourceProvider;
    @Mock
    private RestAPIContext context;

    // The controller to test
    private Index index;

    /**
     * You can configure mocks before each tests in the setup method
     */
    /* @BeforeEach
    void setUp() throws FileNotFoundException {
        // Create a new instance under test
        index = new Index();

        // Simulate access to configuration.properties resource
        when(context.getResourceProvider()).thenReturn(resourceProvider);
        when(resourceProvider.getResourceAsStream("configuration.properties"))
                .thenReturn(IndexTest.class.getResourceAsStream("/testConfiguration.properties"));
    }


    @Test
    void should_get_result_when_params_ok() {

        // Given

        // When
        Result result = index.execute(context);

        // Then
        assertThat(result.getCurrentDate()).isEqualTo(LocalDate.now());
    }

    @Test
    void should_return_a_json_representation_as_result() throws IOException {
        // Given "a RestAPIController"

        // Simulate a request with a value for each parameter

        // When "Invoking the REST API"
        RestApiResponse apiResponse = index.doHandle(httpRequest, new RestApiResponseBuilder(), context);

        // Then "A JSON representation is returned in response body"
        Result jsonResponse = index.getMapper().readValue((String) apiResponse.getResponse(), Result.class);

        // Validate returned response
        assertThat(apiResponse.getHttpStatus()).isEqualTo(200);
        assertThat(jsonResponse).isNotNull();
    } */

}
