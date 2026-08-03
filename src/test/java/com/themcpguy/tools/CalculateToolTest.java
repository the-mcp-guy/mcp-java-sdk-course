package com.themcpguy.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.json.jackson2.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The simplest shape: a synchronous handler, called directly.
 * <p>
 * No server, no transport, no JSON-RPC. evaluate(...) is package-private and this test
 * lives in the same package, which is the whole reason the handler was split out from
 * spec() back in Class 3.
 */
class CalculateToolTest {

    private final CalculateTool tool =
            new CalculateTool(new JacksonMcpJsonMapper(new ObjectMapper()));

    /** Every tool answers with content; this pulls the first block out as text. */
    private static String textOf(CallToolResult result) {
        return ((TextContent) result.content().getFirst()).text();
    }

    @Test
    @DisplayName("evaluates an expression and reports the result as JSON")
    void shouldEvaluateExpression() {
        CallToolResult result = tool.evaluate(Map.of("expression", "2 + 3 * 4"));

        assertThat(result.isError()).isFalse();
        assertThat(textOf(result)).contains("14");
    }

    @Test
    @DisplayName("a missing expression is a tool error, not an exception")
    void shouldReturnToolErrorWhenExpressionMissing() {
        CallToolResult result = tool.evaluate(Map.of());

        assertThat(result.isError()).isTrue();
        assertThat(textOf(result)).contains("expression");
    }

    @Test
    @DisplayName("division by zero produces a tool error, not an exception")
    void shouldReturnToolErrorForDivisionByZero() {
        CallToolResult result = tool.evaluate(Map.of("expression", "1 / 0"));

        assertThat(result.isError()).isTrue();
        assertThat(textOf(result)).containsIgnoringCase("zero");
    }
}
