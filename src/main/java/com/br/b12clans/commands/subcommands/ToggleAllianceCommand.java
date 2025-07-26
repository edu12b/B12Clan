package com.br.b12clans.commands.subcommands;

import com.br.b12clans.Main;
import com.br.b12clans.managers.ClanManager;
import com.br.b12clans.models.Clan;
import com.br.b12clans.utils.MessagesManager;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class ToggleAllianceCommand implements SubCommand {

    private final Main plugin;
    private final ClanManager clanManager;
    private final MessagesManager messages;

    public ToggleAllianceCommand(Main plugin) {
        this.plugin = plugin;
        this.clanManager = plugin.getClanManager();
        this.messages = plugin.getMessagesManager();
    }

    @Override
    public String getName() {
        return "togglealliance";
    }

    @Override
    public String getPermission() {
        return null; // A permissão é ser líder
    }

    @Override
    public void execute(Player player, String[] args) {
        Clan clan = clanManager.getPlayerClan(player.getUniqueId());
        if (clan == null) {
            messages.sendMessage(player, "no-clan");
            return;
        }

        // Apenas líderes podem usar este comando
        plugin.getDatabaseManager().getMemberRoleAsync(clan.getId(), player.getUniqueId()).thenAccept(role -> {
            if (role == null || !(role.equals("OWNER") || role.equals("VICE_LEADER"))) {
                messages.sendMessage(player, "ally-no-permission"); // Reutilizando mensagem
                return;
            }

            clanManager.areAllianceRequestsDisabledAsync(clan.getId()).thenAccept(currentlyDisabled -> {
                boolean newStatus = !currentlyDisabled;
                plugin.getDatabaseManager().setAllianceRequestsDisabledAsync(clan.getId(), newStatus)
                        .thenRun(() -> {
                            clanManager.loadAllianceRequestStatus(clan.getId());
                            plugin.getServer().getScheduler().runTask(plugin, () -> {
                                messages.sendMessage(player, newStatus ? "alliance-requests-disabled" : "alliance-requests-enabled");
                            });
                        });
            });
        });
    }

    @Override
    public List<String> onTabComplete(Player player, String[] args) {
        return Collections.emptyList();
    }
}