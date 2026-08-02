package com.themcpguy.resources;

import com.themcpguy.tools.CustomerRepository;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Wraps a repository so that adding a contact also announces which resources changed.
 * <p>
 * A decorator rather than a change to CustomerRepository itself: Class 3's code does
 * not need to know that anything is subscribing to it.
 */
final class NotifyingCustomerRepository implements CustomerRepository {

    private final CustomerRepository delegate;
    private final Consumer<String> onCustomerChanged;

    NotifyingCustomerRepository(CustomerRepository delegate, Consumer<String> onCustomerChanged) {
        this.delegate = delegate;
        this.onCustomerChanged = onCustomerChanged;
    }

    @Override
    public CompletableFuture<Contact> addContactAsync(String customerId, String name, String email) {
        return delegate.addContactAsync(customerId, name, email)
                .thenApply(created -> {
                    onCustomerChanged.accept(created.customerId());
                    return created;
                });
    }

    @Override
    public CompletableFuture<List<Customer>> searchAsync(String query, int limit) {
        return delegate.searchAsync(query, limit);
    }

    @Override
    public CompletableFuture<List<Customer>> allAsync() {
        return delegate.allAsync();
    }

    @Override
    public CompletableFuture<List<Contact>> contactsForAsync(String customerId) {
        return delegate.contactsForAsync(customerId);
    }
}
