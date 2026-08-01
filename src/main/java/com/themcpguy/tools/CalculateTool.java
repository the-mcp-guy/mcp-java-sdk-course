package com.themcpguy.tools;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import io.modelcontextprotocol.spec.McpSchema.ToolAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

public final class CalculateTool {

    private static final Logger log = LoggerFactory.getLogger(CalculateTool.class);

    private static final String SCHEMA = """
            {
              "type": "object",
              "properties": {
                "expression": {
                  "type": "string",
                  "description": "The mathematical expression to evaluate"
                }
              },
              "required": ["expression"]
            }
            """;

    private final McpJsonMapper jsonMapper;

    public CalculateTool(McpJsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public SyncToolSpecification spec() {
        Tool definition = Tool.builder("calculate", jsonMapper, SCHEMA)
                .description("""
                        Evaluate a mathematical expression and return the result.
                        Supports +, -, *, /, ^ (power), parentheses, and the functions
                        sqrt, abs, floor, ceil, and round(value, places).
                        Examples: "2 + 2", "sqrt(144)", "2^10", "round(3.14159, 2)".
                        Use for arithmetic and maths. Do not use for currency conversion,
                        because there are no live exchange rates here.
                        """)
                .annotations(ToolAnnotations.builder()
                        .readOnlyHint(true)
                        .openWorldHint(false)
                        .build())
                .build();

        return SyncToolSpecification.builder()
                .tool(definition)
                .callHandler((exchange, request) -> evaluate(request.arguments()))
                .build();
    }

    CallToolResult evaluate(Map<String, Object> arguments) {
        Object raw = arguments.get("expression");
        if (!(raw instanceof String expression) || expression.isBlank()) {
            return Results.error("'expression' is required and must be a non-blank string");
        }
        try {
            double value = ExpressionEvaluator.evaluate(expression);
            log.debug("calculate '{}' -> {}", expression, value);
            return CallToolResult.builder()
                    .addTextContent(jsonMapper.writeValueAsString(
                            Map.of("expression", expression, "result", value)))
                    .build();
        } catch (IllegalArgumentException e) {
            return Results.error("Invalid expression: " + e.getMessage());
        } catch (IOException e) {
            return Results.error("Could not serialise the result: " + e.getMessage());
        }
    }
}
