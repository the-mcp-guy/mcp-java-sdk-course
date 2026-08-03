package com.themcpguy.http;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Everything under {@code mcp:} in application.yml, bound once when the application starts.
 * Spring maps the kebab-case keys in the file onto these components, so {@code allowed-origins}
 * becomes {@code allowedOrigins}.
 */
@ConfigurationProperties("mcp")
public record McpHttpProperties(String endpoint, List<String> allowedOrigins, List<String> allowedHosts) {
}
