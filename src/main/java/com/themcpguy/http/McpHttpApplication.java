package com.themcpguy.http;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The same customer tools from Class 3, reachable over HTTP instead of stdio.
 */
@SpringBootApplication
public class McpHttpApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpHttpApplication.class, args);
    }
}
