package com.janboerman.invsee.spigot;

import com.janboerman.invsee.spigot.command.CommandCompletionService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;

public class InvseeTabCompleter implements TabCompleter {

    private final CommandCompletionService completions;

    public InvseeTabCompleter(CommandCompletionService completions) {
        this.completions = completions;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        return completions.complete(sender, command, args);
    }

}
