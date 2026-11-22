package com.supportticket.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;

import com.supportticket.support.ApiTestBase;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

class GlobalExceptionHandlerTest extends ApiTestBase {

    @RestController
    static class ExplodingController {
        @GetMapping("/boom")
        String boom() {
            throw new IllegalStateException("secret internal detail: jdbc:h2:file:/var/data");
        }
    }

    @Test
    void anUnexpectedExceptionReturnsAGenericInternalErrorWithoutInternals() throws Exception {
        MockMvc standalone = MockMvcBuilders.standaloneSetup(new ExplodingController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        MvcResult result = standalone.perform(get("/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code", is("INTERNAL_ERROR")))
                .andExpect(jsonPath("$.status", is(500)))
                .andExpect(jsonPath("$.path", is("/boom")))
                .andExpect(jsonPath("$.message", not(containsString("secret"))))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).doesNotContain("IllegalStateException").doesNotContain("jdbc").doesNotContain("at com.");
    }

    @Test
    void malformedJsonIsAValidationErrorNotAServerError() throws Exception {
        mockMvc.perform(post("/api/v1/tickets").contentType(MediaType.APPLICATION_JSON).content("{ not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.fieldErrors[0].field", is("body")));
    }

    @Test
    void aWrongTypeForAFieldIsAValidationErrorNamingTheField() throws Exception {
        mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"T\",\"description\":\"D\",\"priority\":123}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.fieldErrors[0].field", is("priority")));
    }

    @Test
    void aNonNumericTicketIdIsAValidationError() throws Exception {
        mockMvc.perform(get("/api/v1/tickets/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.fieldErrors[0].field", is("id")));
    }

    @Test
    void anUnknownRouteIsAPlain404InTheSameShape() throws Exception {
        mockMvc.perform(get("/api/v1/nothing-here"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("REQUEST_ERROR")))
                .andExpect(jsonPath("$.message", is("The requested resource was not found.")));
    }

    @Test
    void anUnsupportedMethodIsA405InTheSameShape() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/v1/tickets/1"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code", is("REQUEST_ERROR")));
    }

    @Test
    void everyErrorCarriesTheStandardFields() throws Exception {
        mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("title", "", "description", ""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.code").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }
}
