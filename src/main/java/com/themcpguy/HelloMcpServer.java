package com.themcpguy;

import io.modelcontextprotocol.server.McpServer;

public class HelloMcpServer {
    public static void main(String[] args) {
        System.out.println("MCP SDK available: " + McpServer.class.getName());
    }
}