package com.themcpguy;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.jackson2.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema.Implementation;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.stream.Stream;

/**
 * Starts one of the course servers as a subprocess and hands back a connected client.
 * <p>
 * The JAR is located rather than hard coded, so renaming the project or changing its
 * version does not break the tests, and no build tool or IDE setting is required.
 */
final class McpTestServer {

    private McpTestServer() {
    }

    static McpSyncClient start(String mainClass) {
        ServerParameters parameters = ServerParameters.builder("java")
                .args("-cp", locateJar().toString(), mainClass)
                .build();

        McpSyncClient client = McpClient
                .sync(new StdioClientTransport(parameters, new JacksonMcpJsonMapper(new ObjectMapper())))
                .clientInfo(Implementation.builder("integration-test", "1.0.0").build())
                .requestTimeout(Duration.ofSeconds(20))
                .build();

        client.initialize();
        return client;
    }

    /**
     * Finds the packaged JAR by asking the classloader where this test class came from.
     * That is target/test-classes, so its parent is target, which is where the JAR lands.
     * The shade plugin also leaves original-*.jar behind, which is the unshaded one.
     */
    private static Path locateJar() {
        Path target = testClassesDirectory().getParent();
        try (Stream<Path> files = Files.list(target)) {
            return files
                    .filter(file -> file.getFileName().toString().endsWith(".jar"))
                    .filter(file -> !file.getFileName().toString().startsWith("original-"))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "No packaged JAR in " + target + ". These are integration tests: "
                                    + "run 'mvn verify', or 'mvn package' first if you are "
                                    + "starting them from an IDE."));
        } catch (IOException e) {
            throw new IllegalStateException("Could not read " + target, e);
        }
    }

    private static Path testClassesDirectory() {
        try {
            return Path.of(McpTestServer.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Could not work out where the test classes are", e);
        }
    }
}
