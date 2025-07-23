package com.br.b12clans.commands.subcommands;

import com.br.b12clans.Main;
import com.br.b12clans.managers.ClanManager;
import com.br.b12clans.managers.CommandManager;
import com.br.b12clans.models.Clan;
import com.br.b12clans.utils.MessagesManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class HomeCommand implements SubCommand {

    private final Main plugin;
    private final ClanManager clanManager;
    private final MessagesManager messages;
    private final CommandManager commandManager;

    public HomeCommand(Main plugin) {
        this.plugin = plugin;
        this.clanManager = plugin.getClanManager();
        this.messages = plugin.getMessagesManager();
        this.commandManager = plugin.getCommandManager();
    }

    @Override
    public String getName() {
        return "home";
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

        if (args.length == 0) {
            handleTeleport(player, clan);
            return;
        }

        String action = args[0].toLowerCase();

        if (commandManager.getActionAliasesFor("home", "set").contains(action)) {
            handleSet(player, clan);
        } else if (commandManager.getActionAliasesFor("home", "clear").contains(action)) {
            handleClear(player, clan);
        } else {
            messages.sendMessage(player, "home-usage");
        }
    }

    // ##### MÉTODO CORRIGIDO #####
    private void handleTeleport(Player player, Clan clan) {
        plugin.getDatabaseManager().getClanHomeAsync(clan.getId()).thenAccept(homeData -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (homeData == null) {
                    messages.sendMessage(player, "home-not-set");
                    return;
                }
                try {
                    // Monta o objeto Location a partir dos dados brutos
                    String worldName = (String) homeData[0];
                    double x = (Double) homeData[1];
                    double y = (Double) homeData[2];
                    double z = (Double) homeData[3];
                    float yaw = ((Number) homeData[4]).floatValue();
                    float pitch = ((Number) homeData[5]).floatValue();

                    Location homeLocation = new Location(Bukkit.getWorld(worldName), x, y, z, yaw, pitch);

                    if (homeLocation.getWorld() == null) {
                        messages.sendMessage(player, "home-world-not-found");
                        return;
                    }

                    // Agora sim, teleporta para a Location correta
                    player.teleport(homeLocation);
                    messages.sendMessage(player, "home-teleported");

                } catch (Exception e) {
                    messages.sendMessage(player, "generic-error");
                    plugin.getLogger().severe("Erro ao carregar e teleportar para a home do clã " + clan.getId() + ": " + e.getMessage());
                }
            });
        });
    }

    private void handleSet(Player player, Clan clan) {
        plugin.getDatabaseManager().getMemberRoleAsync(clan.getId(), player.getUniqueId())
                .thenAccept(role -> {
                    if (role == null || !(role.equals("OWNER") || role.equals("VICE_LEADER"))) {
                        plugin.getServer().getScheduler().runTask(plugin, () -> messages.sendMessage(player, "home-set-no-permission"));
                        return;
                    }

                    Location loc = player.getLocation();
                    plugin.getDatabaseManager().setClanHomeAsync(clan.getId(), loc).thenRun(() -> {
                        plugin.getServer().getScheduler().runTask(plugin, () -> messages.sendMessage(player, "home-set-success"));
                    });
                });
    }

    private void handleClear(Player player, Clan clan) {
        plugin.getDatabaseManager().getMemberRoleAsync(clan.getId(), player.getUniqueId())
                .thenAccept(role -> {
                    if (role == null || !role.equals("OWNER")) {
                        plugin.getServer().getScheduler().runTask(plugin, () -> messages.sendMessage(player, "home-clear-no-permission"));
                        return;
                    }

                    plugin.getDatabaseManager().clearClanHomeAsync(clan.getId()).thenRun(() -> {
                        plugin.getServer().getScheduler().runTask(plugin, () -> messages.sendMessage(player, "home-cleared"));
                    });
                });
    }

    @Override
    public List<String> onTabComplete(Player player, String[] args) {
        if (args.length == 1) {
            List<String> allActions = commandManager.getActionAliasesFor("home", "set");
            allActions.addAll(commandManager.getActionAliasesFor("home", "clear"));

            return allActions.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}