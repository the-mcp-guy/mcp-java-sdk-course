package com.themcpguy.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.themcpguy.prompts.AccountReviewPrompt;
import com.themcpguy.prompts.EscalationNotePrompt;
import com.themcpguy.resources.CustomerBadgeResource;
import com.themcpguy.resources.CustomerDirectoryResource;
import com.themcpguy.resources.CustomerProfileResource;
import com.themcpguy.tools.AddContactTool;
import com.themcpguy.tools.AsyncSpecs;
import com.themcpguy.tools.CalculateTool;
import com.themcpguy.tools.CustomerRepository;
import com.themcpguy.tools.SearchCustomersTool;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson2.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.transport.DefaultServerTransportSecurityValidator;
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;
import jakarta.servlet.Servlet;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(McpHttpProperties.class)
public class McpHttpConfig {

    /**
     * Spring Boot 4 auto-configures a Jackson 3 mapper, which lives in {@code tools.jackson}
     * and is a different type from the {@code com.fasterxml} one the MCP SDK is built on.
     * So the SDK's mapper is declared here, and this is the single place to configure it.
     */
    @Bean
    public ObjectMapper mcpObjectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public McpJsonMapper mcpJsonMapper(ObjectMapper objectMapper) {
        return new JacksonMcpJsonMapper(objectMapper);
    }

    @Bean
    public CustomerRepository customerRepository() {
        return CustomerRepository.inMemory();
    }

    /**
     * The Streamable HTTP transport. One endpoint answers both POST and GET, which is
     * what the specification asks for.
     */
    @Bean
    public HttpServletStreamableServerTransportProvider mcpTransport(McpJsonMapper jsonMapper,
                                                                     McpHttpProperties properties) {

        return HttpServletStreamableServerTransportProvider.builder()
                .jsonMapper(jsonMapper)
                .mcpEndpoint(properties.endpoint())
                // Without this the transport accepts any Origin, which is what makes a
                // local server reachable from a web page the user happens to visit.
                .securityValidator(DefaultServerTransportSecurityValidator.builder()
                        .allowedOrigins(properties.allowedOrigins())
                        .allowedHosts(properties.allowedHosts())
                        .build())
                .build();
    }

    /**
     * The transport is an HttpServlet, but Spring does not route to it until it is
     * registered. Without this bean the server starts and answers nothing.
     */
    @Bean
    public ServletRegistrationBean<Servlet> mcpServlet(
            HttpServletStreamableServerTransportProvider transport,
            McpHttpProperties properties) {

        ServletRegistrationBean<Servlet> registration =
                new ServletRegistrationBean<>(transport, properties.endpoint());
        registration.setName("mcp");
        registration.setAsyncSupported(true);
        return registration;
    }

    @Bean
    public McpAsyncServer mcpServer(HttpServletStreamableServerTransportProvider transport,
                                    McpJsonMapper jsonMapper,
                                    CustomerRepository customers) {

        var calculate = new CalculateTool(jsonMapper);
        var search = new SearchCustomersTool(jsonMapper, customers);
        var addContact = new AddContactTool(jsonMapper, customers);

        var directory = new CustomerDirectoryResource(jsonMapper, customers);
        var profile = new CustomerProfileResource(jsonMapper, customers);
        var badge = new CustomerBadgeResource(customers);

        var accountReview = new AccountReviewPrompt(customers);
        var escalationNote = new EscalationNotePrompt();

        return McpServer.async(transport)
                .serverInfo("acme-http", "1.0.0")
                .capabilities(ServerCapabilities.builder()
                        .tools(true)
                        // No subscriptions here: this server reads a plain repository,
                        // so there is nothing to send an update notification about.
                        .resources(false, true)
                        .prompts(true)
                        .build())
                .tools(AsyncSpecs.asAsync(calculate.spec()), search.spec(), addContact.spec())
                .resources(directory.spec())
                .resourceTemplates(profile.spec(), badge.spec())
                .prompts(AsyncSpecs.asAsync(accountReview.spec()),
                        AsyncSpecs.asAsync(escalationNote.spec()))
                .build();
    }
}
