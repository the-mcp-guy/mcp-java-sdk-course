package com.themcpguy;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson2.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Minimal MCP server: one tool, 'echo', over stdio. The smallest piece of code that
 * exercises the full request lifecycle (initialize, capability negotiation, tools/list,
 * tools/call) and proves the SDK, the transport, and the tool dispatch all wire up
 * correctly.
 * <p>
 * Run as a fat JAR; point Claude Desktop / Cursor / Zed at it via stdio.
 * <p>
 * See course Module 2: <a href="https://themcpguy.com/docs/mcp-java-sdk/your-first-mcp-server">...</a>
 */
public class HelloMcpServer {

    private static final Logger log = LoggerFactory.getLogger(HelloMcpServer.class);

    public static void main(String[] args) throws InterruptedException {

        McpJsonMapper jsonMapper = new JacksonMcpJsonMapper(new ObjectMapper());
        var transportProvider = new StdioServerTransportProvider(jsonMapper);

        Tool echoTool = Tool.builder("echo", jsonMapper, """
                        {
                          "type": "object",
                          "properties": {
                            "message": {
                              "type": "string",
                              "description": "The text to echo back"
                            }
                          },
                          "required": ["message"]
                        }
                        """)
                .description("Returns whatever text you pass in. Useful for testing.")
                .build();

        var echoSpec = McpServerFeatures.SyncToolSpecification.builder()
                .tool(echoTool)
                .callHandler((exchange, request) -> {
                    Object raw = request.arguments().get("message");
                    if (!(raw instanceof String text) || text.isBlank()) {
                        return CallToolResult.builder()
                                .isError(true)
                                .addTextContent("'message' must be a non-blank string")
                                .build();
                    }
                    log.debug("echo called: '{}'", text);
                    return CallToolResult.builder()
                            .addTextContent("Echo: " + text)
                            .build();
                })
                .build();

        McpServer.sync(transportProvider)
                .serverInfo("my-first-server", "1.0.0")
                .capabilities(ServerCapabilities.builder().tools(true).build())
                .tools(echoSpec)
                .build();

        log.info("hello-mcp-server started (stdio); awaiting messages on stdin");

        Thread.currentThread().join();
    }
}