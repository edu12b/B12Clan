// ARQUIVO: src/main/java/com/br/b12clans/commands/subcommands/DescriptionCommand.java
package com.br.b12clans.commands.subcommands;

import com.br.b12clans.Main;
import com.br.b12clans.managers.ClanManager;
import com.br.b12clans.models.Clan;
import com.br.b12clans.utils.MessagesManager;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DescriptionCommand implements SubCommand {

    private final Main plugin;
    private final ClanManager clanManager;
    private final MessagesManager messages;

    public DescriptionCommand(Main plugin) {
        this.plugin = plugin;
        this.clanManager = plugin.getClanManager();
        this.messages = plugin.getMessagesManager();
    }

    @Override
    public String getName() {
        return "description";
    }

    @Override
    public String getPermission() {
        return null; // Permissão baseada no cargo, verificada internamente.
    }

    @Override
    public void execute(Player player, String[] args) {
        Clan clan = clanManager.getPlayerClan(player.getUniqueId());
        if (clan == null) {
            messages.sendMessage(player, "no-clan");
            return;
        }

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            String role = plugin.getDatabaseManager().getMemberRole(clan.getId(), player.getUniqueId());

            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (role == null || !(role.equals("OWNER") || role.equals("VICE_LEADER"))) {
                    messages.sendMessage(player, "no-permission-to-set-description");
                    return;
                }

                if (args.length < 1) {
                    messages.sendMessage(player, "description-usage");
                    return;
                }

                String description = String.join(" ", args);
                if (description.length() > 100) {
                    messages.sendMessage(player, "description-too-long");
                    return;
                }

                String coloredDescription = clanManager.translateColors(description);

                plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                    boolean success = plugin.getDatabaseManager().updateClanDescription(clan.getId(), description);

                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        if (success) {
                            // LINHA CORRIGIDA: Em vez de tentar modificar o objeto, recarregamos os dados.
                            clanManager.loadPlayerClan(player.getUniqueId());
                            messages.sendMessage(player, "description-set-success", "%description%", coloredDescription);
                        } else {
                            messages.sendMessage(player, "generic-error");
                        }
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