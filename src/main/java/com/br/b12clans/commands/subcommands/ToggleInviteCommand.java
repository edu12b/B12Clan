package com.br.b12clans.commands.subcommands;

import com.br.b12clans.Main;
import com.br.b12clans.managers.ClanManager;
import com.br.b12clans.utils.MessagesManager;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class ToggleInviteCommand implements SubCommand {

    private final Main plugin;
    private final ClanManager clanManager;
    private final MessagesManager messages;

    public ToggleInviteCommand(Main plugin) {
        this.plugin = plugin;
        this.clanManager = plugin.getClanManager();
        this.messages = plugin.getMessagesManager();
    }

    @Override
    public String getName() {
        return "toggleinvite";
    }

    @Override
    public String getPermission() {
        return null;
    }

    @Override
    public void execute(Player player, String[] args) {
        // Pega o status ATUAL de forma assíncrona
        clanManager.isInvitesDisabledAsync(player.getUniqueId()).thenAccept(currentlyDisabled -> {
            // Inverte o status
            boolean newStatus = !currentlyDisabled;

            // Manda salvar o novo status no banco de dados e depois atualiza o cache
            plugin.getDatabaseManager().setPlayerInvitesDisabledAsync(player.getUniqueId(), newStatus)
                    .thenRun(() -> {
                        clanManager.loadPlayerInviteStatus(player.getUniqueId());
                        // Envia a mensagem na thread principal
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            messages.sendMessage(player, newStatus ? "invites-disabled" : "invites-enabled");
                        });
                    });
        });
    }

    @Override
    public List<String> onTabComplete(Player player, String[] args) {
        return Collections.emptyList();
    }
}