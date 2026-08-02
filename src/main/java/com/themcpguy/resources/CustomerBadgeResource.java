package com.themcpguy.resources;

import com.themcpguy.tools.CustomerRepository;
import io.modelcontextprotocol.server.McpServerFeatures.AsyncResourceTemplateSpecification;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema.BlobResourceContents;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceResult;
import io.modelcontextprotocol.spec.McpSchema.ResourceTemplate;
import io.modelcontextprotocol.util.DefaultMcpUriTemplateManager;
import io.modelcontextprotocol.util.McpUriTemplateManager;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.List;

/**
 * A binary resource template. Everything is the same as a text resource except the
 * contents type: BlobResourceContents carries base64 instead of text.
 *
 * The badge is drawn on the fly so the example needs no image files on disk.
 */
public final class CustomerBadgeResource {

    public static final String TEMPLATE = "customers://{customerId}/badge.png";
    private static final int SIZE = 128;

    private static final McpUriTemplateManager URI_TEMPLATE = new DefaultMcpUriTemplateManager(TEMPLATE);

    private final CustomerRepository repository;

    public CustomerBadgeResource(CustomerRepository repository) {
        this.repository = repository;
    }

    public AsyncResourceTemplateSpecification spec() {
        ResourceTemplate definition = ResourceTemplate.builder(TEMPLATE, "Customer badge")
                .description("A 128x128 PNG showing the customer's initials, tinted by account status.")
                .mimeType("image/png")
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

        byte[] png = drawBadge(initials(customer.name()), "ACTIVE".equals(customer.accountStatus()));

        return ReadResourceResult.builder(List.of(
                BlobResourceContents.builder(uri, Base64.getEncoder().encodeToString(png))
                        .mimeType("image/png")
                        .build())).build();
    }

    static String initials(String name) {
        String[] words = name.trim().split("\\s+");
        StringBuilder out = new StringBuilder();
        for (String word : words) {
            if (!word.isBlank() && out.length() < 2) {
                out.append(Character.toUpperCase(word.charAt(0)));
            }
        }
        return out.isEmpty() ? "?" : out.toString();
    }

    private static byte[] drawBadge(String initials, boolean active) {
        BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(active ? new Color(0x1F, 0x6F, 0x4A) : new Color(0x7A, 0x2E, 0x2E));
            g.fillRect(0, 0, SIZE, SIZE);

            g.setColor(Color.WHITE);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 56));
            var metrics = g.getFontMetrics();
            int x = (SIZE - metrics.stringWidth(initials)) / 2;
            int y = (SIZE - metrics.getHeight()) / 2 + metrics.getAscent();
            g.drawString(initials, x, y);
        } finally {
            g.dispose();
        }

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Could not encode the badge: " + e.getMessage(), e);
        }
    }

}
