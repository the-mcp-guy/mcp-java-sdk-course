package com.themcpguy.tools;

import io.modelcontextprotocol.spec.McpSchema.CallToolResult;

/** Small helper so every tool reports failures the same way. */
public final class Results {

    private Results() {
    }

    public static CallToolResult error(String message) {
        return CallToolResult.builder()
                .isError(true)
                .addTextContent(message)
                .build();
    }
}