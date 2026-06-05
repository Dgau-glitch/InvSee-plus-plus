package com.janboerman.invsee.spigot.internal;

import com.janboerman.invsee.spigot.api.EnderSpectatorInventory;
import com.janboerman.invsee.spigot.api.MainSpectatorInventory;

import java.lang.ref.WeakReference;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class OpenSpectatorsCache /*TODO implement some kind of Cache interface that is api-public?*/ {

    private final ConcurrentMap<UUID, WeakReference<MainSpectatorInventory>> openInventories = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, WeakReference<EnderSpectatorInventory>> openEnderChests = new ConcurrentHashMap<>();

    public void cache(MainSpectatorInventory spectatorInventory) {
        cache(spectatorInventory, false);
    }

    public void cache(MainSpectatorInventory spectatorInventory, boolean force) {
        UUID targetId = spectatorInventory.getSpectatedPlayerId();
        if (force) {
            openInventories.put(targetId, new WeakReference<>(spectatorInventory));
        } else {
            openInventories.compute(targetId, (ignored, ref) -> ref == null || ref.get() == null ? new WeakReference<>(spectatorInventory) : ref);
        }
    }

    public void cache(EnderSpectatorInventory spectatorInventory) {
        cache(spectatorInventory, false);
    }

    public void cache(EnderSpectatorInventory spectatorInventory, boolean force) {
        UUID targetId = spectatorInventory.getSpectatedPlayerId();
        if (force) {
            openEnderChests.put(targetId, new WeakReference<>(spectatorInventory));
        } else {
            openEnderChests.compute(targetId, (ignored, ref) -> ref == null || ref.get() == null ? new WeakReference<>(spectatorInventory) : ref);
        }
    }

    public MainSpectatorInventory getMainSpectatorInventory(UUID targetPlayerId) {
        WeakReference<MainSpectatorInventory> ref = openInventories.get(targetPlayerId);
        if (ref == null) return null;

        MainSpectatorInventory inventory = ref.get();
        if (inventory == null) openInventories.remove(targetPlayerId, ref);
        return inventory;
    }

    public EnderSpectatorInventory getEnderSpectatorInventory(UUID targetPlayerId) {
        WeakReference<EnderSpectatorInventory> ref = openEnderChests.get(targetPlayerId);
        if (ref == null) return null;

        EnderSpectatorInventory inventory = ref.get();
        if (inventory == null) openEnderChests.remove(targetPlayerId, ref);
        return inventory;
    }
}
