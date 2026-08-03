package com.themcpguy;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.GetPromptRequest;
import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.ResourceLink;
import io.modelcontextprotocol.spec.McpSchema.Role;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The Class 5 server. Over the protocol we can check the two things a unit test cannot:
 * what a host is offered in its menu, and what a prompt's messages look like on the wire.
 */
class PromptsServerIT {

    private McpSyncClient client;

    @BeforeEach
    void startServer() {
        client = McpTestServer.start("com.themcpguy.prompts.PromptsMcpServer");
    }

    @AfterEach
    void stopServer() {
        if (client != null) {
            client.closeGracefully();
        }
    }

    @Test
    @DisplayName("prompts/list offers both prompts, with their titles and arguments")
    void shouldListBothPromptsWithArguments() {
        var prompts = client.listPrompts().prompts();

        assertThat(prompts).extracting(McpSchema.Prompt::name)
                .containsExactly("account_review", "escalation_note");
        assertThat(prompts.getFirst().title()).isEqualTo("Account review before a call");
        assertThat(prompts.getFirst().arguments())
                .extracting(McpSchema.PromptArgument::name)
                .containsExactly("customerId", "tone");
    }

    @Test
    @DisplayName("account_review comes back with the account data already in the message")
    void shouldInlineAccountDataOverTheWire() {
        GetPromptResult result = client.getPrompt(GetPromptRequest.builder("account_review")
                .arguments(Map.of("customerId", "CUST-3"))
                .build());

        assertThat(result.messages()).hasSize(1);
        assertThat(((TextContent) result.messages().getFirst().content()).text())
                .contains("Stark Holdings Ltd")
                .contains("SUSPENDED");
    }

    @Test
    @DisplayName("escalation_note sends a resource link and an assistant turn, not account data")
    void shouldSendLinkAndAssistantTurn() {
        GetPromptResult result = client.getPrompt(GetPromptRequest.builder("escalation_note")
                .arguments(Map.of("customerId", "CUST-2"))
                .build());

        assertThat(result.messages()).hasSize(3);

        ResourceLink link = (ResourceLink) result.messages().get(1).content();
        assertThat(link.uri()).isEqualTo("customers://CUST-2");

        assertThat(result.messages().getLast().role()).isEqualTo(Role.ASSISTANT);
        assertThat(((TextContent) result.messages().getLast().content()).text())
                .startsWith("ESCALATION NOTE");
    }
}
