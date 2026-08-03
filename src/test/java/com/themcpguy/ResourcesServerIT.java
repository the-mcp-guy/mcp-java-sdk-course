package com.themcpguy;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.BlobResourceContents;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceRequest;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceResult;
import io.modelcontextprotocol.spec.McpSchema.TextResourceContents;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


import static org.assertj.core.api.Assertions.assertThat;

/**
 * The Class 4 server, over the same stdio transport. Resources are the primitive where
 * the two list calls answer different questions, which only shows up over the protocol.
 */
class ResourcesServerIT {

    private McpSyncClient client;

    @BeforeEach
    void startServer() {
        client = McpTestServer.start("com.themcpguy.resources.ResourcesMcpServer");
    }

    @AfterEach
    void stopServer() {
        if (client != null) {
            client.closeGracefully();
        }
    }

    @Test
    @DisplayName("resources/list returns the static resource only")
    void shouldListOnlyTheStaticResource() {
        assertThat(client.listResources().resources())
                .extracting(McpSchema.Resource::uri)
                .containsExactly("customers://directory");
    }

    @Test
    @DisplayName("resources/templates/list returns the two templates")
    void shouldListBothTemplates() {
        assertThat(client.listResourceTemplates().resourceTemplates())
                .extracting(McpSchema.ResourceTemplate::uriTemplate)
                .containsExactly("customers://{customerId}", "customers://{customerId}/badge.png");
    }

    @Test
    @DisplayName("reading a concrete URI is routed to the template handler")
    void shouldReadOneCustomerThroughTheTemplate() {
        ReadResourceResult result = client.readResource(
                ReadResourceRequest.builder("customers://CUST-2").build());

        TextResourceContents contents = (TextResourceContents) result.contents().getFirst();
        assertThat(contents.mimeType()).isEqualTo("application/json");
        assertThat(contents.text()).contains("Globex Industries");
    }

    @Test
    @DisplayName("the badge template answers with base64 rather than text")
    void shouldReadTheBadgeAsBinary() {
        ReadResourceResult result = client.readResource(
                ReadResourceRequest.builder("customers://CUST-2/badge.png").build());

        BlobResourceContents contents = (BlobResourceContents) result.contents().getFirst();
        assertThat(contents.mimeType()).isEqualTo("image/png");
        assertThat(contents.blob()).startsWith("iVBORw0KGgo");
    }
}
