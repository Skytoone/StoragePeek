package fr.skynex.storagepeek.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Provider class for accessing the registered {@link StoragePeekAPI} instance.
 */
public final class StoragePeekProvider {

    private static StoragePeekAPI instance;

    private StoragePeekProvider() {
        throw new UnsupportedOperationException("StoragePeekProvider cannot be instantiated.");
    }

    /**
     * Gets the active {@link StoragePeekAPI} instance.
     *
     * @return Registered API instance
     * @throws IllegalStateException if API instance has not been initialized yet
     */
    @NotNull
    public static StoragePeekAPI get() {
        if (instance == null) {
            throw new IllegalStateException("StoragePeekAPI is not initialized yet. Ensure StoragePeek plugin is enabled!");
        }
        return instance;
    }

    /**
     * Internal method used by StoragePeek core to register the API implementation.
     *
     * @param api API instance implementation
     */
    public static void setInstance(@Nullable StoragePeekAPI api) {
        StoragePeekProvider.instance = api;
    }
}
