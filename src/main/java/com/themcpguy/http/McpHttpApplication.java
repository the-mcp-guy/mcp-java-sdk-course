package com.themcpguy.http;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The tools, resources and prompts from Classes 3 to 5, reachable over HTTP instead of stdio.
 */
@SpringBootApplication
public class McpHttpApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpHttpApplication.class, args);
    }
}
