package com.themcpguy.prompts;

import io.modelcontextprotocol.server.McpServerFeatures.SyncPromptSpecification;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema.ErrorCodes;
import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.Prompt;
import io.modelcontextprotocol.spec.McpSchema.PromptArgument;
import io.modelcontextprotocol.spec.McpSchema.PromptMessage;
import io.modelcontextprotocol.spec.McpSchema.ResourceLink;
import io.modelcontextprotocol.spec.McpSchema.Role;
import io.modelcontextprotocol.spec.McpSchema.TextContent;

import java.util.List;
import java.util.Map;

/**
 * The same idea as AccountReviewPrompt, built the other way round: instead of reading
 * the account here, it hands the host a ResourceLink and lets the host fetch it.
 * <p>
 * Also shows an ASSISTANT message, which fixes the shape of the reply before the
 * model has written a word.
 */
public final class EscalationNotePrompt {

    public static final String NAME = "escalation_note";

    public SyncPromptSpecification spec() {
        Prompt definition = Prompt.builder(NAME)
                .title("Escalation note")
                .description("Draft an escalation note for one customer, for a handover to the account team.")
                .arguments(List.of(
                        PromptArgument.builder("customerId")
                                .description("Which customer the escalation is about, for example CUST-2.")
                                .required(true)
                                .build()))
                .build();

        return new SyncPromptSpecification(definition, (exchange, request) -> get(request.arguments()));
    }

    GetPromptResult get(Map<String, Object> rawArguments) {
        Map<String, Object> arguments = rawArguments == null ? Map.of() : rawArguments;
        Object rawCustomerId = arguments.get("customerId");
        if (rawCustomerId == null || rawCustomerId.toString().isBlank()) {
            throw McpError.builder(ErrorCodes.INVALID_PARAMS)
                    .message("'customerId' is required, for example CUST-2")
                    .build();
        }
        String customerId = rawCustomerId.toString();

        return GetPromptResult.builder(List.of(
                        new PromptMessage(Role.USER, TextContent.builder("""
                                Draft an escalation note for the account attached below, addressed to
                                the account team. State the problem, what support has already tried,
                                and what you are asking the account team to do.""").build()),

                        // A pointer, not the data. Whether this is ever read is the host's decision.
                        new PromptMessage(Role.USER, ResourceLink.builder()
                                .uri("customers://" + customerId)
                                .name("customer-" + customerId)
                                .title("Account record for " + customerId)
                                .description("Read fresh at the moment the host resolves this link.")
                                .mimeType("application/json")
                                .build()),

                        // Putting words in the assistant's mouth. The model continues from here
                        // rather than starting from nothing, which pins the format.
                        new PromptMessage(Role.ASSISTANT, TextContent.builder(
                                "ESCALATION NOTE\nAccount:").build())))
                .description("Escalation note for " + customerId)
                .build();
    }
}
