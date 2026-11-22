package com.supportticket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.supportticket.support.ApiTestBase;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;
import org.yaml.snakeyaml.Yaml;

/** Guards against the running API drifting from the design contract in specs/003-.../contracts. */
class OpenApiContractTest extends ApiTestBase {

    private static final Path CONTRACT =
            Path.of("..", "specs", "003-ticket-management-workflow", "contracts", "openapi.yaml");
    private static final Set<String> HTTP_METHODS = Set.of("get", "put", "post", "delete", "patch");

    @Test
    void everyOperationInTheContractIsServed() throws Exception {
        MvcResult result = mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn();
        JsonNode generated = read(result).get("paths");

        Set<String> served = new TreeSet<>();
        generated.fieldNames().forEachRemaining(path -> generated.get(path).fieldNames().forEachRemaining(method -> {
            if (HTTP_METHODS.contains(method)) {
                served.add(operation(method, path));
            }
        }));

        assertThat(contractOperations()).isNotEmpty().allSatisfy(op -> assertThat(served).contains(op));
    }

    @SuppressWarnings("unchecked")
    private Set<String> contractOperations() throws IOException {
        Map<String, Object> contract = new Yaml().load(Files.readString(CONTRACT));
        Map<String, Object> paths = (Map<String, Object>) contract.get("paths");
        Set<String> operations = new TreeSet<>();
        paths.forEach((path, definition) -> ((Map<String, Object>) definition).keySet().stream()
                .filter(HTTP_METHODS::contains)
                .forEach(method -> operations.add(operation(method, "/api/v1" + path))));
        return operations;
    }

    /** Path variable names are not part of the wire contract, so {id} and {ticketId} are equivalent. */
    private static String operation(String method, String path) {
        return method.toUpperCase() + " " + path.replaceAll("\\{[^}]+}", "{}");
    }
}
