package com.janboerman.invsee.spigot.internal.inventory;

import com.janboerman.invsee.spigot.api.Scheduler;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Shared snapshot/diff/commit helper for spectator inventories.
 *
 * <p>The viewer thread may mutate only the spectator copy. The commit is scheduled on the
 * target entity owner and applies the copied state only when the target still matches the
 * pre-click snapshot. If the target retired or diverged, the caller receives a deterministic
 * rollback/resync callback instead of touching the live player inventory from the viewer tick.</p>
 */
public final class SpectatorInventoryTransactionService<S> {

    private final Plugin plugin;
    private final Scheduler scheduler;
    private final UUID targetPlayerId;
    private final String inventoryDescription;

    public SpectatorInventoryTransactionService(Plugin plugin, Scheduler scheduler, UUID targetPlayerId, String inventoryDescription) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.targetPlayerId = Objects.requireNonNull(targetPlayerId, "targetPlayerId");
        this.inventoryDescription = Objects.requireNonNull(inventoryDescription, "inventoryDescription");
    }

    public void commit(S before,
                       S after,
                       Supplier<S> liveSnapshot,
                       Consumer<S> liveCommit,
                       Consumer<S> snapshotResync,
                       Runnable committed,
                       Runnable retiredOfflineFlow) {
        scheduler.runEntity(targetPlayerId, () -> {
            S liveBefore = liveSnapshot.get();
            if (liveBefore == null) {
                scheduler.runAsync(retiredOfflineFlow);
                return;
            }

            if (!Objects.equals(liveBefore, before)) {
                snapshotResync.accept(liveBefore);
                plugin.getLogger().fine(() -> "Aborted " + inventoryDescription + " spectator transaction for " + targetPlayerId + ": target inventory changed before commit.");
                return;
            }

            liveCommit.accept(after);
            committed.run();
        }, () -> {
            plugin.getLogger().fine(() -> "Target " + targetPlayerId + " retired before " + inventoryDescription + " spectator transaction commit; switching to offline flow.");
            scheduler.runAsync(retiredOfflineFlow);
        });
    }

    public static boolean sameItemSnapshot(List<org.bukkit.inventory.ItemStack> left, List<org.bukkit.inventory.ItemStack> right) {
        return Objects.equals(left, right);
    }
}
