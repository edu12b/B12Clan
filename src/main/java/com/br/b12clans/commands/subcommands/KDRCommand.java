// ARQUIVO: src/main/java/com/br/b12clans/commands/subcommands/KDRCommand.java
package com.br.b12clans.commands.subcommands;

import com.br.b12clans.Main;
import com.br.b12clans.managers.ClanManager;
import com.br.b12clans.utils.MessagesManager;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class KDRCommand implements SubCommand {

    private final Main plugin;
    private final ClanManager clanManager;
    private final MessagesManager messages;

    public KDRCommand(Main plugin) {
        this.plugin = plugin;
        this.clanManager = plugin.getClanManager();
        this.messages = plugin.getMessagesManager();
    }

    @Override
    public String getName() {
        return "kdr";
    }

    @Override
    public String getPermission() {
        return null;
    }

    @Override
    public void execute(Player player, String[] args) {
        if (clanManager.getPlayerClan(player.getUniqueId()) == null) {
            messages.sendMessage(player, "no-clan");
            return;
        }

        // Lógica com CompletableFuture CORRIGIDA
        plugin.getDatabaseManager().getPlayerKDRAsync(player.getUniqueId())
                .thenAccept(kdrData -> {
                    // Este código ainda está rodando de forma assíncrona.
                    // Agora, agendamos a parte final para rodar na thread principal.
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        int kills = kdrData[0];
                        int deaths = kdrData[1];
                        double kdr = (deaths == 0) ? kills : (double) kills / deaths;

                        messages.sendMessage(player, "kdr-header");
                        messages.sendMessage(player, "kdr-ratio", "%kdr%", String.format("%.2f", kdr));
                        messages.sendMessage(player, "kdr-details", "%kills%", String.valueOf(kills), "%deaths%", String.valueOf(deaths));
                        messages.sendMessage(player, "kdr-footer");
                    });
                })
                .exceptionally(error -> {
                    // Uma única linha que faz todo o trabalho!
                    plugin.getAsyncHandler().handleException(player, error, "generic-error");
                    return null;
                });
    }

    @Override
    public List<String> onTabComplete(Player player, String[] args) {
        return Collections.emptyList();
    }
}