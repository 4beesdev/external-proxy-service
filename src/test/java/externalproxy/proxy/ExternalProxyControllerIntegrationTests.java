package externalproxy.proxy;

import externalproxy.service.ExternalProxyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ExternalProxyControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExternalProxyService externalProxyService;

    @Test
    void locationsEndpointIsPublicAndReturnsProxiedResponse() throws Exception {
        String responseBody = """
                [{"id":"123","name":"Location A"}]
                """;

        given(externalProxyService.getPudoLocations())
                .willReturn(ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(responseBody));

        mockMvc.perform(get("/api/locations"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json(responseBody));

        then(externalProxyService).should().getPudoLocations();
    }
}
