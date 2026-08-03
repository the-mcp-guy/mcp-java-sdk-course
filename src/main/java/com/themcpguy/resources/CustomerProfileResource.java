package com.themcpguy.resources;

import com.themcpguy.tools.CustomerRepository;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpServerFeatures.AsyncResourceTemplateSpecification;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceResult;
import io.modelcontextprotocol.spec.McpSchema.ResourceTemplate;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import io.modelcontextprotocol.spec.McpSchema.TextResourceContents;
import io.modelcontextprotocol.util.DefaultMcpUriTemplateManager;
import io.modelcontextprotocol.util.McpUriTemplateManager;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A resource template: one pattern that answers for every customer, instead of one
 * registration per customer.
 */
public final class CustomerProfileResource {

    public static final String TEMPLATE = "customers://{customerId}";

    /** The SDK's RFC 6570 parser, which turns a matched URI back into its variables. */
    private static final McpUriTemplateManager URI_TEMPLATE = new DefaultMcpUriTemplateManager(TEMPLATE);

    private final McpJsonMapper jsonMapper;
    private final CustomerRepository repository;

    public CustomerProfileResource(McpJsonMapper jsonMapper, CustomerRepository repository) {
        this.jsonMapper = jsonMapper;
        this.repository = repository;
    }

    /** The concrete URI for one customer, used when firing update notifications. */
    public static String uriFor(String customerId) {
        return "customers://" + customerId;
    }

    public AsyncResourceTemplateSpecification spec() {
        ResourceTemplate definition = ResourceTemplate.builder(TEMPLATE, "Customer profile")
                .description("One customer with the people who work there. Ids look like CUST-2.")
                .mimeType("application/json")
                .build();

        return new AsyncResourceTemplateSpecification(definition, (exchange, request) ->
                Mono.fromCallable(() -> read(request.uri()))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    ReadResourceResult read(String uri) {
        String customerId = URI_TEMPLATE.extractVariableValues(uri).get("customerId");
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("no customer id in: " + uri);
        }

        CustomerRepository.Customer customer = repository.searchAsync(customerId, 1).join().stream()
                .filter(c -> c.id().equalsIgnoreCase(customerId))
                .findFirst()
                .orElseThrow(() -> McpError.RESOURCE_NOT_FOUND.apply(uri));

        List<CustomerRepository.Contact> contacts = repository.contactsForAsync(customerId).join();

        // LinkedHashMap, not Map.of: Map.of has no defined iteration order, so the fields
        // would come out shuffled, and differently on each JVM run.
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("id", customer.id());
        profile.put("name", customer.name());
        profile.put("email", customer.email());
        profile.put("accountStatus", customer.accountStatus());
        profile.put("contacts", contacts);

        String json = toJson(profile);

        return ReadResourceResult.builder(List.of(
                TextResourceContents.builder(uri, json)
                        .mimeType("application/json")
                        .build())).build();
    }

    private String toJson(Object value) {
        try {
            return jsonMapper.writeValueAsString(value);
        } catch (IOException e) {
            throw new IllegalStateException("Could not serialise the profile: " + e.getMessage(), e);
        }
    }
}
