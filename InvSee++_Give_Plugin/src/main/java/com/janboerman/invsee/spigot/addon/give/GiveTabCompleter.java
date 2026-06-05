package com.janboerman.invsee.spigot.addon.give;

import static com.janboerman.invsee.utils.Compat.emptyList;
import static com.janboerman.invsee.utils.Compat.singletonList;

import com.janboerman.invsee.spigot.addon.give.common.GiveApi;
import com.janboerman.invsee.spigot.api.InvseeAPI;
import com.janboerman.invsee.utils.StringHelper;
import org.bukkit.Material;
import org.bukkit.command.*;

import java.util.List;
import java.util.StringJoiner;
import java.util.function.Supplier;
import java.util.stream.*;

// TODO can we use Brigadier for ItemStack completions?
// TODO possibly, but we need to keep this around for 1.12.2 and 1.8.8 servers.
class GiveTabCompleter implements TabCompleter {

    private final GiveApi giveApi;
    private final InvseeAPI invseeApi;
    private final Supplier<Stream<Material>> materials;

    GiveTabCompleter(GiveApi giveApi, InvseeAPI invseeApi, Supplier<Stream<Material>> materials) {
        this.giveApi = giveApi;
        this.invseeApi = invseeApi;
        this.materials = materials;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getPermission() != null && !sender.hasPermission(command.getPermission())) return emptyList();

        if (args.length == 0 || args.length == 1) {
            final String inputName = args.length == 0 ? "" : args[0];
            return invseeApi.namesAndUuidsLookup().getCachedUserNamesSnapshot().stream()
                    .filter(name -> StringHelper.startsWithIgnoreCase(name, inputName))
                    .collect(Collectors.toList());
        }

        else if (args.length == 2) {
            final String inputMaterial = args[1].toLowerCase();
            Stream<String> materialNames = materials.get().map(material -> material.name().toLowerCase());
            if (!inputMaterial.isEmpty()) {
                materialNames = materialNames.filter(name -> name.startsWith(inputMaterial));
            }
            return materialNames.collect(Collectors.toList());
        }

        else if (args.length == 3) {
            final String inputAmount = args[2];
            Material material = Material.matchMaterial(args[1]);
            Stream<String> amounts = IntStream.rangeClosed(1, material != null ? material.getMaxStackSize() : giveApi.maxStackSize())
                    .mapToObj(Integer::toString);
            if (!inputAmount.isEmpty()) {
                amounts = amounts.filter(amount -> amount.startsWith(inputAmount));
            }
            return amounts.collect(Collectors.toList());
        }

        else if (args.length >= 4) {
            final StringJoiner sj = new StringJoiner(" ");
            for (int i = 0; i < args.length; i++) sj.add(args[i]);
            final String inputTag = sj.toString();
            if (inputTag.length() == 0) return singletonList("{");
            //else: how to tabcomplete an nbt tag?
        }

        return emptyList();
    }

}
