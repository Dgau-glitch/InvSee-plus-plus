package com.janboerman.invsee.spigot.addon.clone;

import com.janboerman.invsee.spigot.api.InvseeAPI;
import com.janboerman.invsee.utils.Compat;
import com.janboerman.invsee.utils.StringHelper;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

final class CloneTabCompleter implements TabCompleter {

    private final InvseeAPI api;

    CloneTabCompleter(InvseeAPI api) {
        this.api = api;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String permission = command.getPermission();
        if (permission != null && !sender.hasPermission(permission)) return Compat.emptyList();
        if (args.length == 0 || args.length > 2) return Compat.emptyList();

        String prefix = args[args.length - 1];
        Set<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (String name : api.namesAndUuidsLookup().getCachedUserNamesSnapshot()) {
            if (StringHelper.startsWithIgnoreCase(name, prefix)) {
                names.add(name);
            }
        }
        return new ArrayList<>(names);
    }
}
