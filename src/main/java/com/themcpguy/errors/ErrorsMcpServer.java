package com.themcpguy.errors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.themcpguy.tools.CustomerRepository;
import com.themcpguy.tools.CustomerRepository.Failure;
import com.themcpguy.tools.SearchCustomersTool;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson2.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class 3's search tool wired to a backend that misbehaves on purpose, plus a lookup
 * registered twice to show what a silent handler costs.
 * <p>
 * Pass NONE, SLOW or BROKEN as the first argument. Default is BROKEN, because that is
 * the interesting one.
 */
public class ErrorsMcpServer {

    private static final Logger log = LoggerFactory.getLogger(ErrorsMcpServer.class);

    public static void main(String[] args) throws Exception {

        Failure failure = args.length > 0 ? Failure.valueOf(args[0].toUpperCase()) : Failure.BROKEN;

        McpJsonMapper jsonMapper = new JacksonMcpJsonMapper(new ObjectMapper());
        var transportProvider = new StdioServerTransportProvider(jsonMapper);

        // The only line that differs from Class 3's server.
        CustomerRepository customers = CustomerRepository.inMemory(failure);

        var search = new SearchCustomersTool(jsonMapper, customers);
        var findOk = new FindCustomerTool(jsonMapper, CustomerRepository.inMemory(), true);
        var findBroken = new FindCustomerTool(jsonMapper, CustomerRepository.inMemory(), false);

        McpServer.async(transportProvider)
                .serverInfo("acme-errors", "1.0.0")
                .capabilities(ServerCapabilities.builder().tools(true).build())
                .tools(search.spec(), findOk.spec(), findBroken.spec())
                .build();

        log.info("acme-errors started (stdio) with backend failure mode {}", failure);

        Thread.currentThread().join();
    }
}
