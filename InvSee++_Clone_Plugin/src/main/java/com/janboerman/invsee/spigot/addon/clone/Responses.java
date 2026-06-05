package com.janboerman.invsee.spigot.addon.clone;

import com.janboerman.invsee.spigot.api.Scheduler;
import com.janboerman.invsee.spigot.api.response.ImplementationFault;
import com.janboerman.invsee.spigot.api.response.NotCreatedReason;
import com.janboerman.invsee.spigot.api.response.OfflineSupportDisabled;
import com.janboerman.invsee.spigot.api.response.TargetDoesNotExist;
import com.janboerman.invsee.spigot.api.response.TargetHasExemptPermission;
import com.janboerman.invsee.spigot.api.response.UnknownTarget;
import com.janboerman.invsee.spigot.api.target.Target;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import com.janboerman.invsee.spigot.internal.CommandSenderHelper;

final class Responses {

    private Responses() {
    }

    static void sendInventoryError(Scheduler scheduler, CommandSender to, Target target, NotCreatedReason reason) {
        if (reason instanceof TargetDoesNotExist) {
            CommandSenderHelper.send(scheduler, to, ChatColor.RED + "Player " + target + " does not exist.");
        } else if (reason instanceof UnknownTarget) {
            CommandSenderHelper.send(scheduler, to, ChatColor.RED + "Player " + target + " has not logged onto the server yet.");
        }  else if (reason instanceof TargetHasExemptPermission) {
            CommandSenderHelper.send(scheduler, to, ChatColor.RED + "Player " + target + " is exempted from being spectated.");
        } else if (reason instanceof ImplementationFault) {
            CommandSenderHelper.send(scheduler, to, ChatColor.RED + "An internal fault occurred when trying to load " + target + "'s inventory.");
        } else if (reason instanceof OfflineSupportDisabled) {
            CommandSenderHelper.send(scheduler, to, ChatColor.RED + "Spectating offline players' inventories is disabled.");
        } else {
            CommandSenderHelper.send(scheduler, to, ChatColor.RED + "Could not create " + target + "'s inventory for an unknown reason.");
        }
    }

    static void sendEnderChestError(Scheduler scheduler, CommandSender to, Target target, NotCreatedReason reason) {
        if (reason instanceof TargetDoesNotExist) {
            CommandSenderHelper.send(scheduler, to, ChatColor.RED + "Player " + target + " does not exist.");
        } else if (reason instanceof UnknownTarget) {
            CommandSenderHelper.send(scheduler, to, ChatColor.RED + "Player " + target + " has not logged onto the server yet.");
        }  else if (reason instanceof TargetHasExemptPermission) {
            CommandSenderHelper.send(scheduler, to, ChatColor.RED + "Player " + target + " is exempted from being spectated.");
        } else if (reason instanceof ImplementationFault) {
            CommandSenderHelper.send(scheduler, to, ChatColor.RED + "An internal fault occurred when trying to load " + target + "'s enderchest.");
        } else if (reason instanceof OfflineSupportDisabled) {
            CommandSenderHelper.send(scheduler, to, ChatColor.RED + "Spectating offline players' enderchests is disabled.");
        } else {
            CommandSenderHelper.send(scheduler, to, ChatColor.RED + "Could not create " + target + "'s enderchest for an unknown reason.");
        }
    }
}
