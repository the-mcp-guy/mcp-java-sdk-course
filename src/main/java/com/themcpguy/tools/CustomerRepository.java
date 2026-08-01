package com.themcpguy.tools;

import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Both methods return CompletableFuture so that non-reactive backends (JDBC, JPA,
 * a REST client) compose cleanly via Mono.fromFuture.
 *
 * In a real application this would be a JPA repository or an HTTP client. Here it is
 * an interface with a small in-memory implementation, so the seam you would replace
 * is obvious and a test can substitute a repository that fails on purpose.
 */
public interface CustomerRepository {

    record Customer(String id, String name, String email, String accountStatus) {
    }

    /** A person at a customer. Customers are companies; contacts are the people there. */
    record Contact(String id, String customerId, String name, String email) {
    }

    /** Matches a customer by its own details, or by any of its contacts. */
    CompletableFuture<List<Customer>> searchAsync(String query, int limit);

    /** Fails with NoSuchElementException if customerId does not exist. */
    CompletableFuture<Contact> addContactAsync(String customerId, String name, String email);

    static CustomerRepository inMemory() {
        return new InMemory();
    }

    /**
     * Seeded with three customers and no contacts. The lists are concurrent because
     * the async server dispatches tool calls on several threads, so a search can
     * genuinely overlap an add.
     */
    final class InMemory implements CustomerRepository {

        private final List<Customer> customers = new CopyOnWriteArrayList<>(List.of(
                new Customer("CUST-1", "Acme Corp", "billing@acme.example", "ACTIVE"),
                new Customer("CUST-2", "Globex Industries", "ar@globex.example", "ACTIVE"),
                new Customer("CUST-3", "Stark Holdings Ltd", "finance@stark.example", "SUSPENDED")));

        private final List<Contact> contacts = new CopyOnWriteArrayList<>();
        private final AtomicInteger nextContactId = new AtomicInteger(1);

        @Override
        public CompletableFuture<List<Customer>> searchAsync(String query, int limit) {
            String needle = query.toLowerCase(Locale.ROOT);
            return CompletableFuture.supplyAsync(() -> customers.stream()
                    .filter(c -> matches(c, needle, query))
                    .limit(limit)
                    .toList());
        }

        private boolean matches(Customer customer, String needle, String rawQuery) {
            if (customer.name().toLowerCase(Locale.ROOT).contains(needle)
                    || customer.email().toLowerCase(Locale.ROOT).contains(needle)
                    || customer.id().equalsIgnoreCase(rawQuery)) {
                return true;
            }
            return contacts.stream()
                    .filter(contact -> contact.customerId().equals(customer.id()))
                    .anyMatch(contact -> contact.name().toLowerCase(Locale.ROOT).contains(needle)
                            || contact.email().toLowerCase(Locale.ROOT).contains(needle));
        }

        @Override
        public CompletableFuture<Contact> addContactAsync(String customerId, String name, String email) {
            return CompletableFuture.supplyAsync(() -> {
                boolean exists = customers.stream().anyMatch(c -> c.id().equalsIgnoreCase(customerId));
                if (!exists) {
                    throw new NoSuchElementException("no customer with id '" + customerId + "'");
                }
                Contact created = new Contact(
                        "CONTACT-" + nextContactId.getAndIncrement(), customerId, name, email);
                contacts.add(created);
                return created;
            });
        }
    }
}