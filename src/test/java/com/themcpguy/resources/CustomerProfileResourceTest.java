package com.themcpguy.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.themcpguy.tools.CustomerRepository;
import io.modelcontextprotocol.json.jackson2.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema.ErrorCodes;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceResult;
import io.modelcontextprotocol.spec.McpSchema.TextResourceContents;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A resource read is synchronous and returns contents or throws an exception, so the
 * failing case is asserted with assertThatThrownBy rather than by inspecting a flag.
 */
class CustomerProfileResourceTest {

    private final CustomerProfileResource resource = new CustomerProfileResource(
            new JacksonMcpJsonMapper(new ObjectMapper()), CustomerRepository.inMemory());

    @Test
    @DisplayName("reading a concrete URI returns that customer's profile")
    void shouldReadOneCustomer() {
        ReadResourceResult result = resource.read("customers://CUST-2");

        TextResourceContents contents = (TextResourceContents) result.contents().getFirst();
        assertThat(contents.uri()).isEqualTo("customers://CUST-2");
        assertThat(contents.mimeType()).isEqualTo("application/json");
        assertThat(contents.text()).contains("Globex Industries").contains("\"contacts\"");
    }

    @Test
    @DisplayName("the profile fields appear in the order the handler declares them")
    void shouldKeepFieldOrderStable() {
        String json = ((TextResourceContents) resource.read("customers://CUST-2")
                .contents().getFirst()).text();

        assertThat(json).containsSubsequence(
                "\"id\"", "\"name\"", "\"email\"", "\"accountStatus\"", "\"contacts\"");
    }

    @Test
    @DisplayName("an unknown customer fails with -32002, not a generic internal error")
    void shouldFailWithResourceNotFoundForUnknownCustomer() {
        assertThatThrownBy(() -> resource.read("customers://NOPE"))
                .isInstanceOf(McpError.class)
                .satisfies(error -> assertThat(((McpError) error).getJsonRpcError().code())
                        .isEqualTo(ErrorCodes.RESOURCE_NOT_FOUND));
    }
}
