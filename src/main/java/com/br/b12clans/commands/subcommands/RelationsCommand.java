package com.br.b12clans.commands.subcommands;

import com.br.b12clans.Main;
import com.br.b12clans.managers.ClanManager;
import com.br.b12clans.managers.CommandManager;
import com.br.b12clans.models.Clan;
import com.br.b12clans.utils.MessagesManager;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class RelationsCommand implements SubCommand {

    private final Main plugin;
    private final ClanManager clanManager;
    private final MessagesManager messages;
    private final CommandManager commandManager;

    public RelationsCommand(Main plugin) {
        this.plugin = plugin;
        this.clanManager = plugin.getClanManager();
        this.messages = plugin.getMessagesManager();
        this.commandManager = plugin.getCommandManager();
    }

    @Override
    public String getName() {
        return "relations";
    }

    @Override
    public String getPermission() {
        return null; // Comando informativo, sem permissão específica
    }

    @Override
    public void execute(Player player, String[] args) {
        Clan clan = clanManager.getPlayerClan(player.getUniqueId());
        if (clan == null) {
            messages.sendMessage(player, "no-clan");
            return;
        }

        // Se nenhum argumento for fornecido, mostra tudo
        if (args.length == 0) {
            showAllRelations(player, clan);
            return;
        }

        String action = args[0].toLowerCase();

        // Mostra apenas os aliados
        if (commandManager.getActionAliasesFor("relations", "allies").contains(action)) {
            showOnlyAllies(player, clan);
        }
        // Mostra apenas os rivais
        else if (commandManager.getActionAliasesFor("relations", "rivals").contains(action)) {
            showOnlyRivals(player, clan);
        }
        // Argumento inválido
        else {
            messages.sendMessage(player, "relations-usage");
        }
    }

    private void showAllRelations(Player player, Clan clan) {
        CompletableFuture<List<Clan>> alliesFuture = getClansFromIds(clanManager.getClanAlliesAsync(clan.getId()));
        CompletableFuture<List<Clan>> rivalsFuture = getClansFromIds(clanManager.getClanRivalsAsync(clan.getId()));

        CompletableFuture.allOf(alliesFuture, rivalsFuture).thenRun(() -> {
            List<Clan> allies = alliesFuture.join();
            List<Clan> rivals = rivalsFuture.join();

            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (allies.isEmpty() && rivals.isEmpty()) {
                    messages.sendMessage(player, "no-relations");
                    return;
                }
                messages.sendMessage(player, "relations-header");
                displayClanList(player, "relations-allies-header", allies);
                displayClanList(player, "relations-rivals-header", rivals);
                messages.sendMessage(player, "relations-footer");
            });
        });
    }

    private void showOnlyAllies(Player player, Clan clan) {
        getClansFromIds(clanManager.getClanAlliesAsync(clan.getId())).thenAccept(allies -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (allies.isEmpty()) {
                    messages.sendMessage(player, "no-allies");
                    return;
                }
                messages.sendMessage(player, "relations-header");
                displayClanList(player, "relations-allies-header", allies);
                messages.sendMessage(player, "relations-footer");
            });
        });
    }

    private void showOnlyRivals(Player player, Clan clan) {
        getClansFromIds(clanManager.getClanRivalsAsync(clan.getId())).thenAccept(rivals -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (rivals.isEmpty()) {
                    messages.sendMessage(player, "no-rivals");
                    return;
                }
                messages.sendMessage(player, "relations-header");
                displayClanList(player, "relations-rivals-header", rivals);
                messages.sendMessage(player, "relations-footer");
            });
        });
    }

    /**
     * Método auxiliar para exibir uma lista de clãs de forma formatada.
     */
    private void displayClanList(Player player, String headerKey, List<Clan> clans) {
        if (clans.isEmpty()) return;
        messages.sendMessage(player, headerKey, "%count%", String.valueOf(clans.size()));
        clans.forEach(c -> messages.sendMessage(player, "relations-line",
                "%clan_name%", clanManager.formatDisplayName(c.getName()),
                "%clan_tag%", clanManager.translateColors(c.getTag())));
    }

    /**
     * Método auxiliar para transformar uma lista de IDs de clãs em uma lista de objetos Clan.
     */
    private CompletableFuture<List<Clan>> getClansFromIds(CompletableFuture<List<Integer>> idFuture) {
        return idFuture.thenCompose(ids -> {
            if (ids == null || ids.isEmpty()) {
                return CompletableFuture.completedFuture(Collections.emptyList());
            }
            List<CompletableFuture<Clan>> clanFutures = ids.stream()
                    .map(clanManager::getClanById)
                    .collect(Collectors.toList());
            return CompletableFuture.allOf(clanFutures.toArray(new CompletableFuture[0]))
                    .thenApply(v -> clanFutures.stream()
                            .map(CompletableFuture::join)
                            .filter(c -> c != null)
                            .collect(Collectors.toList()));
        });
    }

    @Override
    public List<String> onTabComplete(Player player, String[] args) {
        if (args.length == 1) {
            List<String> suggestions = commandManager.getActionAliasesFor("relations", "allies");
            suggestions.addAll(commandManager.getActionAliasesFor("relations", "rivals"));
            return suggestions.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}