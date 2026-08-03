package com.themcpguy.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.themcpguy.tools.AddContactTool;
import com.themcpguy.tools.CustomerRepository;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson2.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema.ResourcesUpdatedNotification;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicReference;

public class ResourcesMcpServer {

    private static final Logger log = LoggerFactory.getLogger(ResourcesMcpServer.class);

    public static void main(String[] args) throws Exception {

        McpJsonMapper jsonMapper = new JacksonMcpJsonMapper(new ObjectMapper());
        var transportProvider = new StdioServerTransportProvider(jsonMapper);

        // The repository needs to notify a server that does not exist yet. Hold a slot
        // now and fill it in after build(). Nothing reads the slot until a client calls
        // the tool, by which time it is set.
        AtomicReference<McpAsyncServer> serverRef = new AtomicReference<>();

        CustomerRepository customers = new NotifyingCustomerRepository(
                CustomerRepository.inMemory(),
                customerId -> notifyChanged(serverRef.get(), customerId));

        var directory = new CustomerDirectoryResource(jsonMapper, customers);
        var profile = new CustomerProfileResource(jsonMapper, customers);
        var badge = new CustomerBadgeResource(customers);
        var addContact = new AddContactTool(jsonMapper, customers);

        McpAsyncServer server = McpServer.async(transportProvider)
                .serverInfo("acme-resources", "1.0.0")
                .capabilities(ServerCapabilities.builder()
                        .resources(true, true)   // subscribe, listChanged
                        .tools(true)
                        .build())
                .resources(directory.spec())
                .resourceTemplates(profile.spec(), badge.spec())
                .tools(addContact.spec())
                .build();

        serverRef.set(server);

        log.info("acme-resources started (stdio); awaiting messages on stdin");

        Thread.currentThread().join();
    }

    /** Announces that one customer's profile changed, and that the directory did too. */
    private static void notifyChanged(McpAsyncServer server, String customerId) {
        if (server == null) {
            return;
        }
        server.notifyResourcesUpdated(
                new ResourcesUpdatedNotification(CustomerProfileResource.uriFor(customerId))).subscribe();
        server.notifyResourcesUpdated(
                new ResourcesUpdatedNotification(CustomerDirectoryResource.URI)).subscribe();
        log.info("notified subscribers that {} and the directory changed", customerId);
    }
}
