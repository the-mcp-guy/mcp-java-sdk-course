package com.themcpguy.resources;

import com.themcpguy.tools.CustomerRepository;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpServerFeatures.AsyncResourceSpecification;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceResult;
import io.modelcontextprotocol.spec.McpSchema.Resource;
import io.modelcontextprotocol.spec.McpSchema.TextResourceContents;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.util.List;

/**
 * A static resource: one fixed URI, whose contents are read fresh on every request.
 * "Static" describes the address, not the data.
 */
public final class CustomerDirectoryResource {

    public static final String URI = "customers://directory";

    private final McpJsonMapper jsonMapper;
    private final CustomerRepository repository;

    public CustomerDirectoryResource(McpJsonMapper jsonMapper, CustomerRepository repository) {
        this.jsonMapper = jsonMapper;
        this.repository = repository;
    }

    public AsyncResourceSpecification spec() {
        Resource definition = Resource.builder(URI, "Customer directory")
                .description("Every customer on file, with id, name, billing email and account status.")
                .mimeType("application/json")
                .build();

        return new AsyncResourceSpecification(definition, (exchange, request) ->
                Mono.fromCallable(() -> read(request.uri()))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    ReadResourceResult read(String uri) {
        List<CustomerRepository.Customer> customers = repository.allAsync().join();
        return ReadResourceResult.builder(List.of(
                TextResourceContents.builder(uri, toJson(customers))
                        .mimeType("application/json")
                        .build())).build();
    }

    private String toJson(Object value) {
        try {
            return jsonMapper.writeValueAsString(value);
        } catch (IOException e) {
            // Resources have no isError flag. Throwing is how a read fails, and the
            // SDK turns it into a JSON-RPC error the client can report.
            throw new IllegalStateException("Could not serialise the directory: " + e.getMessage(), e);
        }
    }
}
