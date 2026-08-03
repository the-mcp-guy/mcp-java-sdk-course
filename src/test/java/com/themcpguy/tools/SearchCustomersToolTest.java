package com.themcpguy.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.themcpguy.tools.CustomerRepository.Failure;
import io.modelcontextprotocol.json.jackson2.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * An asynchronous handler returns Mono&lt;CallToolResult&gt;, so the assertions go inside
 * a StepVerifier rather than being made on a returned value.
 */
class SearchCustomersToolTest {

    private static final JacksonMcpJsonMapper JSON =
            new JacksonMcpJsonMapper(new ObjectMapper());

    private SearchCustomersTool toolBackedBy(CustomerRepository repository) {
        return new SearchCustomersTool(JSON, repository);
    }

    private static String textOf(CallToolResult result) {
        return ((TextContent) result.content().getFirst()).text();
    }

    @Test
    @DisplayName("a matching query returns the customer as JSON")
    void shouldFindCustomerByName() {
        StepVerifier.create(toolBackedBy(CustomerRepository.inMemory())
                        .search(Map.of("query", "globex")))
                .assertNext(result -> {
                    assertThat(result.isError()).isFalse();
                    assertThat(textOf(result)).contains("Globex Industries").contains("CUST-2");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("a blank query is rejected by the handler, not the schema")
    void shouldReturnToolErrorForBlankQuery() {
        StepVerifier.create(toolBackedBy(CustomerRepository.inMemory())
                        .search(Map.of("query", "   ")))
                .assertNext(result -> {
                    assertThat(result.isError()).isTrue();
                    assertThat(textOf(result)).contains("non-blank");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("limit is capped, so a model asking for 9999 cannot dump the database")
    void shouldCapTheLimit() {
        StepVerifier.create(toolBackedBy(CustomerRepository.inMemory())
                        .search(Map.of("query", "example", "limit", 9999)))
                .assertNext(result -> assertThat(result.isError()).isFalse())
                .verifyComplete();
    }

    @Test
    @DisplayName("a broken backend becomes a tool error, and the Mono still completes")
    void shouldReturnToolErrorWhenBackendFails() {
        StepVerifier.create(toolBackedBy(CustomerRepository.inMemory(Failure.BROKEN))
                        .search(Map.of("query", "globex")))
                .assertNext(result -> {
                    assertThat(result.isError()).isTrue();
                    assertThat(textOf(result)).contains("no connections available");
                })
                .verifyComplete();
    }
}
