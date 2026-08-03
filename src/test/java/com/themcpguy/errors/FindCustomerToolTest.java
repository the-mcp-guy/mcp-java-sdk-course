package com.themcpguy.errors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.themcpguy.tools.CustomerRepository;
import io.modelcontextprotocol.json.jackson2.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Both tests assert that a result comes back. The second one is what fails if the
 * defaultIfEmpty line is ever removed from the handler.
 */
class FindCustomerToolTest {

    private static final JacksonMcpJsonMapper JSON =
            new JacksonMcpJsonMapper(new ObjectMapper());

    /** The third argument is Class 6's switch, left on, which is how real code would have it. */
    private FindCustomerTool tool() {
        return new FindCustomerTool(JSON, CustomerRepository.inMemory(), true);
    }

    private static String textOf(CallToolResult result) {
        return ((TextContent) result.content().getFirst()).text();
    }

    @Test
    @DisplayName("a customer that exists comes back as JSON")
    void shouldFindExistingCustomer() {
        StepVerifier.create(tool().find(Map.of("customerId", "CUST-2")))
                .assertNext(result -> {
                    assertThat(result.isError()).isFalse();
                    assertThat(textOf(result)).contains("Globex Industries");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("a customer that does not exist still produces a result")
    void shouldStillEmitWhenCustomerMissing() {
        StepVerifier.create(tool().find(Map.of("customerId", "NOPE")))
                .assertNext(result -> {
                    assertThat(result.isError()).isTrue();
                    assertThat(textOf(result)).contains("No customer with id 'NOPE'");
                })
                .verifyComplete();
    }
}
