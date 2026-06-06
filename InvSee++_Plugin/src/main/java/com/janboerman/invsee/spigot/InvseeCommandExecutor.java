package com.janboerman.invsee.spigot;

import com.janboerman.invsee.spigot.api.CreationOptions;
import com.janboerman.invsee.spigot.api.Exempt;
import com.janboerman.invsee.spigot.api.InvseeAPI;
import com.janboerman.invsee.spigot.api.MainSpectatorInventoryView;
import com.janboerman.invsee.spigot.api.response.*;
import com.janboerman.invsee.spigot.api.template.PlayerInventorySlot;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class InvseeCommandExecutor implements CommandExecutor {

    private final InvseePlusPlus plugin;

    public InvseeCommandExecutor(InvseePlusPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) return false;
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players!");
            return true;
        }

        Player player = (Player) sender;

        String playerNameOrUUID = args[0];
        boolean isUuid;
        UUID uuid = null;
        try {
            uuid = UUID.fromString(playerNameOrUUID);
            isUuid = true;
        } catch (IllegalArgumentException e) {
            isUuid = false;
        }
        final InvseeAPI api = plugin.getApi();
        //TODO why not just: plugin.getInventoryCreationOptions() ?
        final CreationOptions<PlayerInventorySlot> creationOptions = CreationOptions.defaultMainInventory(plugin)
                .withTitle(plugin.getTitleForInventory())
                .withMirror(plugin.getInventoryMirror())
                .withOfflinePlayerSupport(plugin.offlinePlayerSupport())
                .withUnknownPlayerSupport(plugin.unknownPlayerSupport())
                .withBypassExemptedPlayers(player.hasPermission(Exempt.BYPASS_EXEMPT_INVENTORY))
                .withLogOptions(plugin.getLogOptions())
                .withPlaceholderPalette(plugin.getPlaceholderPalette());

        CompletableFuture<OpenResponse<MainSpectatorInventoryView>> fut;
        if (isUuid) {
            final UUID finalUuid = uuid;
            fut = api.fetchUserName(uuid).thenApply(o -> o.orElse("InvSee++ Player")).exceptionally(t -> "InvSee++ Player")
                    .thenCompose(userName -> api.spectateInventory(player, finalUuid, userName, creationOptions));
        } else {
            fut = api.spectateInventory(player, playerNameOrUUID, creationOptions);
        }

        //Gracefully handle failure and faults.
        fut.whenComplete((response, throwable) -> {
            if (throwable != null) {
                send(player, ChatColor.RED + "An error occurred while trying to open " + playerNameOrUUID + "'s inventory.");
                plugin.getLogger().log(Level.SEVERE, "Error while trying to create main-inventory spectator inventory", throwable);
            } else {
                if (!response.isOpen()) {
                    NotOpenedReason notOpenedReason = response.getReason();
                    if (notOpenedReason instanceof InventoryOpenEventCancelled) {
                        send(player, ChatColor.RED + "Another plugin prevented you from spectating " + playerNameOrUUID + "'s inventory");
                    } else if (notOpenedReason instanceof InventoryNotCreated) {
                        NotCreatedReason notCreatedReason = ((InventoryNotCreated) notOpenedReason).getNotCreatedReason();
                        if (notCreatedReason instanceof TargetDoesNotExist) {
                            send(player, ChatColor.RED + "Player " + playerNameOrUUID + " does not exist.");
                        } else if (notCreatedReason instanceof UnknownTarget) {
                            send(player, ChatColor.RED + "Player " + playerNameOrUUID + " has not logged onto the server yet.");
                        }  else if (notCreatedReason instanceof TargetHasExemptPermission) {
                            send(player, ChatColor.RED + "Player " + playerNameOrUUID + " is exempted from being spectated.");
                        } else if (notCreatedReason instanceof ImplementationFault) {
                            send(player, ChatColor.RED + "An internal fault occurred when trying to load " + playerNameOrUUID + "'s inventory.");
                        } else if (notCreatedReason instanceof OfflineSupportDisabled) {
                            send(player, ChatColor.RED + "Spectating offline players' inventories is disabled.");
                        } else {
                            send(player, ChatColor.RED + "Could not create " + playerNameOrUUID + "'s inventory for an unknown reason.");
                        }
                    } else {
                        send(player, ChatColor.RED + "Could not open " + playerNameOrUUID + "'s inventory for an unknown reason.");
                    }
                } //else: it opened successfully: nothing to do there!
            }
        });

        return true;
    }

    private void send(Player player, String message) {
        plugin.getApi().getScheduler().runEntity(player, () -> player.sendMessage(message), null);
    }

}
