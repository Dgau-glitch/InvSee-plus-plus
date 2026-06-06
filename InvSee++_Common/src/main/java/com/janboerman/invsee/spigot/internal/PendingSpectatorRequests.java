package com.janboerman.invsee.spigot.internal;

import com.janboerman.invsee.spigot.api.SpectatorInventory;
import com.janboerman.invsee.spigot.api.response.SpectateResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Global plugin-state registry for in-flight spectator inventory creations.
 *
 * <p>Requests are keyed by target name/UUID and use {@code computeIfAbsent} so concurrent
 * viewers share the same offline load instead of racing each other. Entries remove themselves
 * only if the completing future is still the registered one, which prevents stale completions
 * from clearing a newer request for the same target.</p>
 */
public final class PendingSpectatorRequests<I extends SpectatorInventory<?>> {

    private final ConcurrentMap<String, CompletableFuture<SpectateResponse<I>>> byName = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, CompletableFuture<SpectateResponse<I>>> byUuid = new ConcurrentHashMap<>();

    public CompletableFuture<SpectateResponse<I>> byName(String targetName, Supplier<CompletableFuture<SpectateResponse<I>>> factory) {
        String key = normalize(targetName);
        return byName.computeIfAbsent(key, ignored -> track(byName, key, factory.get()));
    }

    public CompletableFuture<SpectateResponse<I>> byUuid(UUID targetId, Supplier<CompletableFuture<SpectateResponse<I>>> factory) {
        return byUuid.computeIfAbsent(targetId, ignored -> track(byUuid, targetId, factory.get()));
    }

    public CompletableFuture<SpectateResponse<I>> removeName(String targetName) {
        return byName.remove(normalize(targetName));
    }

    public CompletableFuture<SpectateResponse<I>> removeUuid(UUID targetId) {
        return byUuid.remove(targetId);
    }

    public void shutdown(Logger logger) {
        List<CompletableFuture<SpectateResponse<I>>> futures = new ArrayList<>(byName.values());
        futures.addAll(byUuid.values());
        byName.clear();
        byUuid.clear();

        for (CompletableFuture<SpectateResponse<I>> future : futures) {
            if (!future.isDone()) {
                future.cancel(false);
            } else {
                try {
                    future.getNow(null);
                } catch (Throwable throwable) {
                    logger.log(Level.FINE, "Pending spectator request completed exceptionally during shutdown.", throwable);
                }
            }
        }
    }

    private static <K, I extends SpectatorInventory<?>> CompletableFuture<SpectateResponse<I>> track(
            ConcurrentMap<K, CompletableFuture<SpectateResponse<I>>> map,
            K key,
            CompletableFuture<SpectateResponse<I>> future) {
        future.whenComplete((result, error) -> map.remove(key, future));
        return future;
    }

    private static String normalize(String targetName) {
        return targetName.toLowerCase(Locale.ROOT);
    }
}
