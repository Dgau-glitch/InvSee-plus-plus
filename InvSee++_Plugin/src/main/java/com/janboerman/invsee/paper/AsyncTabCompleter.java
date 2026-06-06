package com.janboerman.invsee.paper;

import com.destroystokyo.paper.event.server.AsyncTabCompleteEvent;
import com.janboerman.invsee.spigot.command.CommandCompletionService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Collections;
import java.util.List;

public class AsyncTabCompleter implements Listener {

    private final CommandCompletionService completions;

    public AsyncTabCompleter(CommandCompletionService completions) {
        this.completions = completions;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        completions.rememberJoin(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        completions.rememberQuit(event.getPlayer());
    }

    @EventHandler
    public void onTabComplete(AsyncTabCompleteEvent event) {
        if (!event.isCommand() || !completions.isKnownCommandBuffer(event.getBuffer())) {
            return;
        }

        if (event.getSender() instanceof Player && !completions.canTabComplete((Player) event.getSender(), event.getBuffer())) {
            event.setHandled(true);
            event.setCompletions(Collections.emptyList());
            return;
        }

        List<String> suggestions = completions.completeAsyncCommandBuffer(event.getBuffer());
        event.setCompletions(suggestions);
        event.setHandled(true);
    }
}
