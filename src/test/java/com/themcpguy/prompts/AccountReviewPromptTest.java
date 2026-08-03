package com.themcpguy.prompts;

import com.themcpguy.tools.CustomerRepository;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema.ErrorCodes;
import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.Role;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Prompts are worth testing precisely because the SDK does not validate their
 * arguments: every required-argument check is yours, so every one of them can be wrong.
 */
class AccountReviewPromptTest {

    private final AccountReviewPrompt prompt =
            new AccountReviewPrompt(CustomerRepository.inMemory());

    private static String firstMessage(GetPromptResult result) {
        return ((TextContent) result.messages().getFirst().content()).text();
    }

    @Test
    @DisplayName("the account data is inlined into the message, not left for the host")
    void shouldInlineAccountData() {
        GetPromptResult result = prompt.get(Map.of("customerId", "CUST-3"));

        assertThat(result.messages()).hasSize(1);
        assertThat(result.messages().getFirst().role()).isEqualTo(Role.USER);
        assertThat(firstMessage(result))
                .contains("Stark Holdings Ltd")
                .contains("SUSPENDED")
                .contains("nobody on file");
    }

    @Test
    @DisplayName("tone changes the closing instruction, and the account data stays")
    void shouldChangeOnlyTheClosingInstructionWhenToneChanges() {
        String brief = firstMessage(prompt.get(Map.of("customerId", "CUST-3")));
        String formal = firstMessage(prompt.get(Map.of("customerId", "CUST-3", "tone", "formal")));

        assertThat(brief).contains("short summary");
        assertThat(formal).contains("written handover");
        assertThat(formal).contains("Stark Holdings Ltd");
    }

    @Test
    @DisplayName("a missing required argument is rejected with -32602")
    void shouldFailWithInvalidParamsWhenCustomerIdMissing() {
        assertThatThrownBy(() -> prompt.get(Map.of()))
                .isInstanceOf(McpError.class)
                .satisfies(error -> assertThat(((McpError) error).getJsonRpcError().code())
                        .isEqualTo(ErrorCodes.INVALID_PARAMS));
    }

    @Test
    @DisplayName("a null argument map produces an McpError, not a NullPointerException")
    void shouldRejectNullArgumentsWithMcpError() {
        assertThatThrownBy(() -> prompt.get(null))
                .isInstanceOf(McpError.class);
    }

    @Test
    @DisplayName("a customerId sent as a number reports not-found, not a ClassCastException")
    void shouldReportNotFoundForNumericCustomerId() {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("customerId", 42);

        assertThatThrownBy(() -> prompt.get(arguments))
                .isInstanceOf(McpError.class)
                .satisfies(error -> assertThat(((McpError) error).getJsonRpcError().code())
                        .isEqualTo(ErrorCodes.RESOURCE_NOT_FOUND));
    }
}
