package com.janboerman.invsee.spigot.internal;

import com.janboerman.invsee.spigot.api.Scheduler;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Utility methods for sender-owned command feedback on Folia. */
public final class CommandSenderHelper {

    private CommandSenderHelper() {
    }

    public static void send(Scheduler scheduler, CommandSender sender, String message) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            scheduler.runEntity(player, () -> player.sendMessage(message), null);
        } else {
            sender.sendMessage(message);
        }
    }

    public static void run(Scheduler scheduler, CommandSender sender, Runnable task) {
        if (sender instanceof Player) {
            scheduler.runEntity((Player) sender, task, null);
        } else {
            scheduler.runGlobal(task);
        }
    }
}
