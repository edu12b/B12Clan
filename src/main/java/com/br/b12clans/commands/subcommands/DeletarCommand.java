// ARQUIVO: src/main/java/com/br/b12clans/commands/subcommands/DeletarCommand.java

package com.br.b12clans.commands.subcommands;

import com.br.b12clans.Main;
import com.br.b12clans.managers.ClanManager;
import com.br.b12clans.managers.CommandManager; // <-- IMPORTAÇÃO ADICIONADA
import com.br.b12clans.models.Clan;
import com.br.b12clans.utils.MessagesManager;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors; // <-- IMPORTAÇÃO ADICIONADA

public class DeletarCommand implements SubCommand {

    private final Main plugin;
    private final ClanManager clanManager;
    private final MessagesManager messages;
    private final CommandManager commandManager; // <-- DEPENDÊNCIA ADICIONADA

    public DeletarCommand(Main plugin) {
        this.plugin = plugin;
        this.clanManager = plugin.getClanManager();
        this.messages = plugin.getMessagesManager();
        this.commandManager = plugin.getCommandManager(); // <-- INICIALIZAÇÃO
    }

    @Override
    public String getName() {
        return "deletar";
    }

    @Override
    public String getPermission() {
        return null;
    }

    @Override
    public void execute(Player player, String[] args) {
        Clan clan = clanManager.getPlayerClan(player.getUniqueId());
        if (clan == null) {
            messages.sendMessage(player, "no-clan");
            return;
        }

        // Se o jogador não digitou "confirm" (ou um de seus aliases)
        if (args.length == 0 || !commandManager.getActionAliasesFor("delete", "confirm").contains(args[0].toLowerCase())) {
            messages.sendMessage(player, "delete-confirm"); // Envia a mensagem de aviso
            return;
        }

        // Se chegou aqui, o jogador digitou "/clan deletar confirm", então prosseguimos.
        plugin.getDatabaseManager().getMemberRoleAsync(clan.getId(), player.getUniqueId())
                .thenComposeAsync(role -> {
                    if (role == null || !role.equals("OWNER")) {
                        return CompletableFuture.failedFuture(new IllegalAccessException("delete-no-permission"));
                    }
                    return CompletableFuture.supplyAsync(() ->
                            plugin.getDatabaseManager().deleteClan(clan.getId()), plugin.getThreadPool()
                    );
                }, plugin.getThreadPool())
                .thenAccept(success -> {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        if (success) {
                            clanManager.unloadClanFromAllMembers(clan);
                            messages.sendMessage(player, "clan-deleted-success", "%clan_name%", clan.getName());
                            if (plugin.getConfig().getBoolean("discord.enabled", false)) {
                                plugin.getDiscordManager().onClanDisbanded(clan);
                            }
                        } else {
                            messages.sendMessage(player, "error-deleting-clan");
                        }
                    });
                })
                .exceptionally(error -> {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        messages.sendMessage(player, "delete-no-permission");
                    });
                    return null;
                });
    }

    @Override
    public List<String> onTabComplete(Player player, String[] args) {
        // Agora o auto-complete vai sugerir "confirm" e "confirmar"
        if (args.length == 1) {
            return commandManager.getActionAliasesFor("delete", "confirm").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}