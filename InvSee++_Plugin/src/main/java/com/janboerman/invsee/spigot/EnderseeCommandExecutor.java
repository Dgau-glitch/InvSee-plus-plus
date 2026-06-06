package com.janboerman.invsee.spigot;

import com.janboerman.invsee.spigot.api.CreationOptions;
import com.janboerman.invsee.spigot.api.EnderSpectatorInventoryView;
import com.janboerman.invsee.spigot.api.Exempt;
import com.janboerman.invsee.spigot.api.InvseeAPI;
import com.janboerman.invsee.spigot.api.response.ImplementationFault;
import com.janboerman.invsee.spigot.api.response.InventoryNotCreated;
import com.janboerman.invsee.spigot.api.response.InventoryOpenEventCancelled;
import com.janboerman.invsee.spigot.api.response.NotCreatedReason;
import com.janboerman.invsee.spigot.api.response.NotOpenedReason;
import com.janboerman.invsee.spigot.api.response.OfflineSupportDisabled;
import com.janboerman.invsee.spigot.api.response.OpenResponse;
import com.janboerman.invsee.spigot.api.response.SpectateResponse;
import com.janboerman.invsee.spigot.api.response.TargetDoesNotExist;
import com.janboerman.invsee.spigot.api.response.TargetHasExemptPermission;
import com.janboerman.invsee.spigot.api.response.UnknownTarget;
import com.janboerman.invsee.spigot.api.template.EnderChestSlot;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class EnderseeCommandExecutor implements CommandExecutor {

    private final InvseePlusPlus plugin;

    public EnderseeCommandExecutor(InvseePlusPlus plugin) {
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
        UUID uuid;
        boolean isUuid;
        try {
            uuid = UUID.fromString(playerNameOrUUID);
            isUuid = true;
        } catch (IllegalArgumentException e) {
            isUuid = false;
            uuid = null;
        }

        final InvseeAPI api = plugin.getApi();
        //TODO why not just: plugin.getEnderChestCreationOptions() ?
        final CreationOptions<EnderChestSlot> creationOptions = CreationOptions.defaultEnderInventory(plugin)
                .withTitle(plugin.getTitleForEnderChest())
                .withMirror(plugin.getEnderChestMirror())
                .withOfflinePlayerSupport(plugin.offlinePlayerSupport())
                .withUnknownPlayerSupport(plugin.unknownPlayerSupport())
                .withBypassExemptedPlayers(player.hasPermission(Exempt.BYPASS_EXEMPT_ENDERCHEST))
                .withLogOptions(plugin.getLogOptions())
                .withPlaceholderPalette(plugin.getPlaceholderPalette());

        CompletableFuture<OpenResponse<EnderSpectatorInventoryView>> fut;
        if (isUuid) {
            final UUID finalUuid = uuid;
            fut = api.fetchUserName(uuid).thenApply(o -> o.orElse("InvSee++ Player")).exceptionally(t -> "InvSee++ Player")
                    .thenCompose(userName -> api.spectateEnderChest(player, finalUuid, userName, creationOptions));
        } else {
            fut = api.spectateEnderChest(player, playerNameOrUUID, creationOptions);
        }

        //Gracefully handle failure and faults
        fut.whenComplete((openResponse, throwable) -> {
            if (throwable != null) {
                send(player, ChatColor.RED + "An error occurred while trying to open " + playerNameOrUUID + "'s enderchest.");
                plugin.getLogger().log(Level.SEVERE, "Error while trying to create ender-chest spectator inventory", throwable);
            } else {
                if (!openResponse.isOpen()) {
                    NotOpenedReason notOpenedReason = openResponse.getReason();
                    if (notOpenedReason instanceof InventoryOpenEventCancelled) {
                        send(player, ChatColor.RED + "Another plugin prevented you from spectating " + playerNameOrUUID + "'s ender chest.");
                    } else if (notOpenedReason instanceof InventoryNotCreated) {
                        NotCreatedReason reason = ((InventoryNotCreated) notOpenedReason).getNotCreatedReason();
                        if (reason instanceof TargetDoesNotExist) {
                            send(player, ChatColor.RED + "Player " + playerNameOrUUID + " does not exist.");
                        } else if (reason instanceof UnknownTarget) {
                            send(player, ChatColor.RED + "Player " + playerNameOrUUID + " has not logged onto the server yet.");
                        } else if (reason instanceof TargetHasExemptPermission) {
                            send(player, ChatColor.RED + "Player " + playerNameOrUUID + " is exempted from being spectated.");
                        } else if (reason instanceof ImplementationFault) {
                            send(player, ChatColor.RED + "An internal fault occurred when trying to load " + playerNameOrUUID + "'s enderchest.");
                        } else if (reason instanceof OfflineSupportDisabled) {
                            send(player, ChatColor.RED + "Spectating offline players' enderchests is disabled.");
                        } else {
                            send(player, ChatColor.RED + "Could not create " + playerNameOrUUID + "'s enderchest for an unknown reason.");
                        }
                    } else {
                        send(player, ChatColor.RED + "Could not open " + playerNameOrUUID + "'s enderchest for an unknown reason.");
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
