// ARQUIVO NOVO
package com.br.b12clans.commands.subcommands;

import com.br.b12clans.Main;
import com.br.b12clans.managers.ClanManager;
import com.br.b12clans.models.Clan;
import com.br.b12clans.utils.MessagesManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class RivalCommand implements SubCommand {

    private final Main plugin;
    private final ClanManager clanManager;
    private final MessagesManager messages;

    public RivalCommand(Main plugin) {
        this.plugin = plugin;
        this.clanManager = plugin.getClanManager();
        this.messages = plugin.getMessagesManager();
    }

    @Override
    public String getName() {
        return "rival";
    }

    @Override
    public String getPermission() {
        return null;
    }

    @Override
    public void execute(Player player, String[] args) {
        Clan sourceClan = clanManager.getPlayerClan(player.getUniqueId());
        if (sourceClan == null) {
            messages.sendMessage(player, "no-clan");
            return;
        }
        if (args.length < 2) {
            messages.sendMessage(player, "rival-usage");
            return;
        }
        String action = args[0].toLowerCase();
        String targetTag = args[1];

        clanManager.getClanByTagAsync(targetTag).thenAccept(targetClan -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (targetClan == null) {
                    messages.sendMessage(player, "clan-not-found", "%tag%", targetTag);
                    return;
                }
                if (targetClan.getId() == sourceClan.getId()) {
                    messages.sendMessage(player, "cannot-rival-self");
                    return;
                }
                if ("add".equals(action)) {
                    plugin.getDatabaseManager().addRivalAsync(sourceClan.getId(), targetClan.getId()).thenRun(() -> {
                        plugin.getServer().getScheduler().runTask(plugin, () -> messages.sendMessage(player, "rival-added", "%clan_name%", targetClan.getName()));
                    });
                } else if ("remove".equals(action)) {
                    plugin.getDatabaseManager().removeRivalAsync(sourceClan.getId(), targetClan.getId()).thenRun(() -> {
                        plugin.getServer().getScheduler().runTask(plugin, () -> messages.sendMessage(player, "rival-removed", "%clan_name%", targetClan.getName()));
                    });
                } else {
                    messages.sendMessage(player, "rival-usage");
                }
            });
        });
    }

    @Override
    public List<String> onTabComplete(Player player, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("add", "remove").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2) {
            Clan playerClan = clanManager.getPlayerClan(player.getUniqueId());
            if (playerClan == null) return Collections.emptyList();

            return Bukkit.getOnlinePlayers().stream()
                    .map(p -> clanManager.getPlayerClan(p.getUniqueId()))
                    .filter(c -> c != null && c.getId() != playerClan.getId())
                    .map(c -> clanManager.getCleanTag(c.getTag()))
                    .distinct()
                    .filter(tag -> tag.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}