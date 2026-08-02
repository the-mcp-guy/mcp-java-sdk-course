package com.themcpguy.prompts;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.themcpguy.resources.CustomerProfileResource;
import com.themcpguy.tools.AsyncSpecs;
import com.themcpguy.tools.CustomerRepository;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson2.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PromptsMcpServer {

    private static final Logger log = LoggerFactory.getLogger(PromptsMcpServer.class);

    public static void main(String[] args) throws Exception {

        McpJsonMapper jsonMapper = new JacksonMcpJsonMapper(new ObjectMapper());
        var transportProvider = new StdioServerTransportProvider(jsonMapper);

        CustomerRepository customers = CustomerRepository.inMemory();

        var accountReview = new AccountReviewPrompt(customers);
        var escalationNote = new EscalationNotePrompt();

        // The profile resource from Class 4 comes along so that the ResourceLink in
        // escalation_note points at something this server can actually serve.
        var profile = new CustomerProfileResource(jsonMapper, customers);

        McpServer.async(transportProvider)
                .serverInfo("acme-prompts", "1.0.0")
                .capabilities(ServerCapabilities.builder()
                        .prompts(true)            // listChanged
                        .resources(false, false)  // no subscriptions here; Class 4 has those
                        .build())
                .prompts(AsyncSpecs.asAsync(accountReview.spec()),
                        AsyncSpecs.asAsync(escalationNote.spec()))
                .resourceTemplates(profile.spec())
                .build();

        log.info("acme-prompts started (stdio); awaiting messages on stdin");

        Thread.currentThread().join();
    }
}
