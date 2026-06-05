package com.janboerman.invsee.spigot.command;

import com.janboerman.invsee.paper.OfflinePlayerProvider;
import com.janboerman.invsee.spigot.InvseePlusPlus;
import com.janboerman.invsee.spigot.api.InvseeAPI;
import com.janboerman.invsee.spigot.api.Scheduler;
import com.janboerman.invsee.spigot.perworldinventory.PerWorldInventorySeeApi;
import com.janboerman.invsee.spigot.perworldinventory.PwiCommandArgs;
import com.janboerman.invsee.utils.Compat;
import com.janboerman.invsee.utils.StringHelper;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;

/**
 * Folia-safe command completion module.
 *
 * <p>The sets in this service are global plugin state and are safe to read from sync and async
 * tab-completion. Bukkit player collections and permission subscriptions are sampled only from
 * scheduled server work or player events, so async tab completion never calls Bukkit APIs.</p>
 */
public final class CommandCompletionService {

    private final InvseePlusPlus plugin;
    private final Set<String> knownLabels = new ConcurrentSkipListSet<>(String.CASE_INSENSITIVE_ORDER);
    private final Set<String> onlineNames = new ConcurrentSkipListSet<>(String.CASE_INSENSITIVE_ORDER);
    private final Set<String> offlineNames = new ConcurrentSkipListSet<>(String.CASE_INSENSITIVE_ORDER);
    private final Set<UUID> playersWhoCanTabComplete = ConcurrentHashMap.newKeySet();
    private final Set<UUID> playersWhoCanUseInvsee = ConcurrentHashMap.newKeySet();
    private final Set<UUID> playersWhoCanUseEndersee = ConcurrentHashMap.newKeySet();

    public CommandCompletionService(InvseePlusPlus plugin, Scheduler scheduler, OfflinePlayerProvider playerDatabase) {
        this.plugin = plugin;
        knownLabels.addAll(Arrays.asList("invsee", "inventorysee", "isee", "endersee", "enderchestsee", "esee"));
        List<String> prefixed = knownLabels.stream().map(label -> "invseeplusplus:" + label).collect(Collectors.toList());
        knownLabels.addAll(prefixed);

        if (plugin.offlinePlayerSupport() && plugin.tabCompleteOfflinePlayers()) {
            scheduler.runAsync(() -> playerDatabase.getAll(this::rememberOfflineName));
        }

        scheduler.runGlobalRepeatedly(this::refreshOnlineSnapshot, 0L, 20L * 60L);
    }

    public List<String> complete(CommandSender sender, Command command, String[] args) {
        if (!sender.hasPermission(InvseePlusPlus.TABCOMPLETION_PERMISSION)) return Compat.emptyList();
        String commandPermission = command.getPermission();
        if (commandPermission != null && !sender.hasPermission(commandPermission)) return Compat.emptyList();
        if ("invseeplusplusreload".equalsIgnoreCase(command.getName())) return Compat.emptyList();

        if (args.length <= 1) {
            String prefix = args.length == 0 ? "" : args[0];
            return completePlayerNames(prefix);
        }

        InvseeAPI api = plugin.getApi();
        if (args.length == 2 && api instanceof PerWorldInventorySeeApi) {
            return PwiCommandArgs.complete(args[1], ((PerWorldInventorySeeApi) api).getHook());
        }

        return Compat.emptyList();
    }

    public boolean isKnownCommandBuffer(String buffer) {
        String lower = buffer.toLowerCase(Locale.ROOT);
        for (String label : knownLabels) {
            if (lower.startsWith("/" + label.toLowerCase(Locale.ROOT) + " ")) {
                return true;
            }
        }
        return false;
    }

    public boolean canTabComplete(Player player, String buffer) {
        ParsedCommand parsed = parse(buffer);
        if (parsed == null) return false;
        UUID playerId = player.getUniqueId();
        if (!playersWhoCanTabComplete.contains(playerId)) return false;
        if (isEnderLabel(parsed.label)) return playersWhoCanUseEndersee.contains(playerId);
        return playersWhoCanUseInvsee.contains(playerId);
    }

    public List<String> completeAsyncCommandBuffer(String buffer) {
        ParsedCommand parsed = parse(buffer);
        if (parsed == null) return Compat.emptyList();

        if (parsed.arguments.length <= 1) {
            String prefix = parsed.arguments.length == 0 ? "" : parsed.arguments[0];
            return completePlayerNames(prefix);
        }

        return Compat.emptyList();
    }

    public void rememberJoin(Player player) {
        onlineNames.add(player.getName());
        rememberOfflineName(player.getName());
        rememberPermissions(player);
    }

    public void rememberQuit(Player player) {
        onlineNames.remove(player.getName());
        UUID playerId = player.getUniqueId();
        playersWhoCanTabComplete.remove(playerId);
        playersWhoCanUseInvsee.remove(playerId);
        playersWhoCanUseEndersee.remove(playerId);
    }

    public void rememberOfflineName(String name) {
        if (name != null && !name.isEmpty()) offlineNames.add(name);
    }

    private List<String> completePlayerNames(String prefix) {
        Set<String> names = new ConcurrentSkipListSet<>(String.CASE_INSENSITIVE_ORDER);
        addMatching(names, onlineNames, prefix);
        if (plugin.offlinePlayerSupport() && plugin.tabCompleteOfflinePlayers()) {
            addMatching(names, offlineNames, prefix);
            InvseeAPI api = plugin.getApi();
            if (api != null) addMatching(names, api.namesAndUuidsLookup().getCachedUserNamesSnapshot(), prefix);
        }
        return new ArrayList<>(names);
    }

    private static void addMatching(Collection<String> destination, Iterable<String> source, String prefix) {
        for (String name : source) {
            if (StringHelper.startsWithIgnoreCase(name, prefix)) {
                destination.add(name);
            }
        }
    }

    private void refreshOnlineSnapshot() {
        Set<String> names = new ConcurrentSkipListSet<>(String.CASE_INSENSITIVE_ORDER);
        Set<UUID> permitted = ConcurrentHashMap.newKeySet();
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            names.add(player.getName());
            rememberOfflineName(player.getName());
            if (player.hasPermission(InvseePlusPlus.TABCOMPLETION_PERMISSION)) {
                permitted.add(player.getUniqueId());
            }
            rememberPermissions(player);
        }
        onlineNames.clear();
        onlineNames.addAll(names);
        playersWhoCanTabComplete.clear();
        playersWhoCanTabComplete.addAll(permitted);
    }

    private void rememberPermissions(Player player) {
        rememberPermission(player, playersWhoCanTabComplete, InvseePlusPlus.TABCOMPLETION_PERMISSION);
        rememberPermission(player, playersWhoCanUseInvsee, "invseeplusplus.invsee.view");
        rememberPermission(player, playersWhoCanUseEndersee, "invseeplusplus.endersee.view");
    }

    private static void rememberPermission(Player player, Set<UUID> cache, String permission) {
        UUID playerId = player.getUniqueId();
        if (player.hasPermission(permission)) {
            cache.add(playerId);
        } else {
            cache.remove(playerId);
        }
    }

    private static boolean isEnderLabel(String label) {
        String lower = label.toLowerCase(Locale.ROOT);
        return lower.equals("endersee") || lower.equals("enderchestsee") || lower.equals("esee")
                || lower.endsWith(":endersee") || lower.endsWith(":enderchestsee") || lower.endsWith(":esee");
    }

    private ParsedCommand parse(String buffer) {
        String lower = buffer.toLowerCase(Locale.ROOT);
        for (String label : knownLabels) {
            String commandPrefix = "/" + label.toLowerCase(Locale.ROOT);
            if (lower.equals(commandPrefix) || lower.startsWith(commandPrefix + " ")) {
                String tail = buffer.length() == commandPrefix.length() ? "" : buffer.substring(commandPrefix.length()).trim();
                return new ParsedCommand(label, tail.isEmpty() ? new String[0] : tail.split("\\s+"));
            }
        }
        return null;
    }

    private static final class ParsedCommand {
        private final String label;
        private final String[] arguments;

        private ParsedCommand(String label, String[] arguments) {
            this.label = label;
            this.arguments = arguments;
        }
    }
}
