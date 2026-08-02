package com.themcpguy.prompts;

import com.themcpguy.tools.CustomerRepository;
import io.modelcontextprotocol.server.McpServerFeatures.SyncPromptSpecification;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema.ErrorCodes;
import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.Prompt;
import io.modelcontextprotocol.spec.McpSchema.PromptArgument;
import io.modelcontextprotocol.spec.McpSchema.PromptMessage;
import io.modelcontextprotocol.spec.McpSchema.Role;
import io.modelcontextprotocol.spec.McpSchema.TextContent;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The prompt a support agent runs before phoning a customer.
 * <p>
 * The account data is read here, on the server, and inlined into the message text.
 * That is the version that works in every client, because nothing is left for the
 * host to fetch.
 */
public final class AccountReviewPrompt {

    public static final String NAME = "account_review";

    private final CustomerRepository repository;

    public AccountReviewPrompt(CustomerRepository repository) {
        this.repository = repository;
    }

    public SyncPromptSpecification spec() {
        Prompt definition = Prompt.builder(NAME)
                .title("Account review before a call")
                .description("Summarise one customer's account and list what to check before contacting them.")
                .arguments(List.of(
                        PromptArgument.builder("customerId")
                                .description("Which customer to review, for example CUST-2.")
                                .required(true)
                                .build(),
                        PromptArgument.builder("tone")
                                .description("Optional: 'brief' for a quick summary before a phone call, 'formal' for a written handover.")
                                .required(false)
                                .build()))
                .build();

        return new SyncPromptSpecification(definition, (exchange, request) -> get(request.arguments()));
    }

    GetPromptResult get(Map<String, Object> rawArguments) {
        // arguments() is null, not an empty map, when a client sends no arguments at all.
        Map<String, Object> arguments = rawArguments == null ? Map.of() : rawArguments;

        // Marking an argument required does not make the SDK enforce it. prompts/get
        // performs no validation, unlike tools/call, so the check belongs here.
        Object rawCustomerId = arguments.get("customerId");
        if (rawCustomerId == null || rawCustomerId.toString().isBlank()) {
            throw McpError.builder(ErrorCodes.INVALID_PARAMS)
                    .message("'customerId' is required, for example CUST-2")
                    .build();
        }

        // .toString() rather than a (String) cast: arguments arrive as parsed JSON, so a
        // client that sends 42 hands you an Integer and the cast would throw an exception.
        String customerId = rawCustomerId.toString();
        String tone = arguments.getOrDefault("tone", "brief").toString();

        CustomerRepository.Customer customer = repository.searchAsync(customerId, 1).join().stream()
                .filter(c -> c.id().equalsIgnoreCase(customerId))
                .findFirst()
                .orElseThrow(() -> McpError.RESOURCE_NOT_FOUND.apply("customers://" + customerId));

        List<CustomerRepository.Contact> contacts = repository.contactsForAsync(customerId).join();

        return GetPromptResult.builder(List.of(
                        new PromptMessage(Role.USER, TextContent.builder(instruction(customer, contacts, tone)).build())))
                .description("Account review for " + customer.name())
                .build();
    }

    private static String instruction(CustomerRepository.Customer customer,
                                      List<CustomerRepository.Contact> contacts,
                                      String tone) {

        String people = contacts.isEmpty()
                ? "  (nobody on file, which is itself worth raising)"
                : contacts.stream()
                        .map(c -> "  - " + c.name() + " <" + c.email() + ">")
                        .collect(Collectors.joining("\n"));

        String shape = "formal".equals(tone)
                ? """
                    Write it as a written handover for a colleague taking over the account.
                    Full sentences, no abbreviations, and state anything you are unsure about."""
                : """
                    Write it as a short summary, to be read in the thirty seconds before the call.
                    Short lines, no introduction, most important thing first.""";

        return """
                You are briefing a support agent who is about to contact this customer.

                Account on file:
                  Company:  %s
                  Id:       %s
                  Billing:  %s
                  Status:   %s

                People we know there:
                %s

                Produce:
                1. One line on where this account stands.
                2. Anything that should be raised on the call, and why.
                3. Anything missing from our records that the agent should confirm.

                %s

                Work only from the account data above. If something is not there, say it is
                not there rather than guessing, and never invent a contact or a payment.
                """.formatted(
                customer.name(),
                customer.id(),
                customer.email(),
                customer.accountStatus(),
                people,
                shape);
    }
}
