package ba.unsa.etf.pnwt.routingservice.controller;

import ba.unsa.etf.pnwt.routingservice.dto.LineResponse;
import ba.unsa.etf.pnwt.routingservice.exception.GlobalExceptionHandler;
import ba.unsa.etf.pnwt.routingservice.exception.ResourceNotFoundException;
import ba.unsa.etf.pnwt.routingservice.service.RoutingCrudService;
<<<<<<< HEAD
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
=======
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
>>>>>>> main

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

<<<<<<< HEAD
@WebMvcTest(RoutingController.class)
@Import(GlobalExceptionHandler.class)
class RoutingControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RoutingCrudService routingCrudService;

=======
@ExtendWith(MockitoExtension.class)
class RoutingControllerWebMvcTest {

    private MockMvc mockMvc;

    @Mock
    private RoutingCrudService routingCrudService;

    @InjectMocks
    private RoutingController routingController;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(routingController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

>>>>>>> main
    @Test
    void getLinesReturnsOk() throws Exception {
        LineResponse response = new LineResponse();
        response.setId(1);
        response.setName("Line 1");

        when(routingCrudService.getLines(null, null)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/lines"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Line 1"));
    }

    @Test
    void createLineWithInvalidPayloadReturnsValidationError() throws Exception {
        String payload = "{\"vehicleTypeId\":2,\"isActive\":true}";

        mockMvc.perform(post("/api/v1/lines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation"))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void getLineWhenMissingReturnsNotFoundError() throws Exception {
        when(routingCrudService.getLine(999)).thenThrow(new ResourceNotFoundException("Line not found: id=999"));

        mockMvc.perform(get("/api/v1/lines/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("not_found"))
                .andExpect(jsonPath("$.message").value("Line not found: id=999"));
    }
}
