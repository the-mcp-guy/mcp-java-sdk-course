package com.themcpguy.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson2.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ToolsMcpServer {

    private static final Logger log = LoggerFactory.getLogger(ToolsMcpServer.class);

    public static void main(String[] args) throws Exception {

        McpJsonMapper jsonMapper = new JacksonMcpJsonMapper(new ObjectMapper());
        var transportProvider = new StdioServerTransportProvider(jsonMapper);
        var customers = CustomerRepository.inMemory();

        var calculate = new CalculateTool(jsonMapper);
        var searchCustomers = new SearchCustomersTool(jsonMapper, customers);
        var addContact = new AddContactTool(jsonMapper, customers);

        McpServer.async(transportProvider)
                .serverInfo("acme-tools", "1.0.0")
                .capabilities(ServerCapabilities.builder().tools(true).build())
                .tools(
                        AsyncSpecs.asAsync(calculate.spec()),
                        searchCustomers.spec(),
                        addContact.spec())
                .build();

        log.info("acme-tools started (stdio); awaiting messages on stdin");

        Thread.currentThread().join();
    }
}
