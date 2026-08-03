package com.themcpguy;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The Class 3 server, running as a real subprocess and answering over the same stdio
 * transport Claude Desktop uses.
 * <p>
 * Named *IT so Failsafe runs it after package, when the JAR exists.
 */
class ToolsServerIT {

    private McpSyncClient client;

    @BeforeEach
    void startServer() {
        client = McpTestServer.start("com.themcpguy.tools.ToolsMcpServer");
    }

    @AfterEach
    void stopServer() {
        if (client != null) {
            client.closeGracefully();
        }
    }

    @Test
    @DisplayName("the packaged JAR advertises all three tools")
    void shouldListAllThreeTools() {
        assertThat(client.listTools().tools())
                .extracting(McpSchema.Tool::name)
                .contains("calculate", "search_customers", "add_contact");
    }

    @Test
    @DisplayName("a real tool call goes out and comes back over stdio")
    void shouldCallSearchCustomersOverStdio() {
        CallToolResult result = client.callTool(
                CallToolRequest.builder("search_customers")
                        .arguments(Map.of("query", "globex"))
                        .build());

        assertThat(result.isError()).isFalse();
        assertThat(((TextContent) result.content().getFirst()).text()).contains("Globex Industries");
    }

    @Test
    @DisplayName("the SDK rejects arguments that do not match the schema")
    void shouldEnforceSchemaValidationOverTheWire() {
        CallToolResult result = client.callTool(
                CallToolRequest.builder("search_customers")
                        .arguments(Map.of())
                        .build());

        assertThat(result.isError()).isTrue();
        assertThat(((TextContent) result.content().getFirst()).text())
                .contains("required property 'query' not found");
    }

    @Test
    @DisplayName("adding a contact is visible to a later search, in the same process")
    void shouldSeeAddedContactInLaterSearch() {
        client.callTool(CallToolRequest.builder("add_contact")
                .arguments(Map.of("customerId", "CUST-2",
                        "name", "Dana Wu", "email", "dana@globex.example"))
                .build());

        CallToolResult found = client.callTool(
                CallToolRequest.builder("search_customers")
                        .arguments(Map.of("query", "dana"))
                        .build());

        assertThat(((TextContent) found.content().getFirst()).text()).contains("Globex Industries");
    }
}
