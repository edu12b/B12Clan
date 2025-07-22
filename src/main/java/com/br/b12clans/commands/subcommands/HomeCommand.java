// ARQUIVO: src/main/java/com/br/b12clans/commands/subcommands/HomeCommand.java
package com.br.b12clans.commands.subcommands;

import com.br.b12clans.Main;
import com.br.b12clans.managers.ClanManager;
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

    public HomeCommand(Main plugin) {
        this.plugin = plugin;
        this.clanManager = plugin.getClanManager();
        this.messages = plugin.getMessagesManager();
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
        } else {
            String action = args[0].toLowerCase();
            switch (action) {
                case "set":
                    handleSetHome(player, clan);
                    break;
                case "clear":
                    handleClearHome(player, clan);
                    break;
                default:
                    messages.sendMessage(player, "home-usage");
                    break;
            }
        }
    }

    private void handleTeleport(Player player, Clan clan) {
        plugin.getDatabaseManager().getClanHomeAsync(clan.getId())
                .thenAccept(homeData -> {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        if (homeData == null) {
                            messages.sendMessage(player, "home-not-set");
                            return;
                        }
                        try {
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
                            player.teleport(homeLocation);
                            messages.sendMessage(player, "home-teleported");
                        } catch (Exception e) {
                            messages.sendMessage(player, "generic-error");
                            plugin.getLogger().severe("Erro ao carregar home do clã " + clan.getId() + ": " + e.getMessage());
                        }
                    });
                })
                .exceptionally(error -> {
                    plugin.getServer().getScheduler().runTask(plugin, () -> messages.sendMessage(player, "generic-error"));
                    return null;
                });
    }

    private void handleSetHome(Player player, Clan clan) {
        plugin.getDatabaseManager().getMemberRoleAsync(clan.getId(), player.getUniqueId())
                .thenComposeAsync(role -> {
                    boolean canSetHome = role != null && (role.equals("OWNER") || (role.equals("VICE_LEADER") && plugin.getConfig().getBoolean("permissions.vice-leader-can-set-home", true)));
                    if (!canSetHome) {
                        return CompletableFuture.failedFuture(new IllegalAccessException("Sem permissão para setar home"));
                    }
                    Location loc = player.getLocation();
                    return plugin.getDatabaseManager().setClanHomeAsync(clan.getId(), loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
                }, plugin.getThreadPool())
                .thenAccept(success -> {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        if (success) {
                            messages.sendMessage(player, "home-set-success");
                        } else {
                            messages.sendMessage(player, "generic-error");
                        }
                    });
                })
                .exceptionally(error -> {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        if (error.getCause() instanceof IllegalAccessException) {
                            messages.sendMessage(player, "home-set-no-permission");
                        } else {
                            messages.sendMessage(player, "generic-error");
                        }
                    });
                    return null;
                });
    }

    private void handleClearHome(Player player, Clan clan) {
        plugin.getDatabaseManager().getMemberRoleAsync(clan.getId(), player.getUniqueId())
                .thenComposeAsync(role -> {
                    if (role == null || !role.equals("OWNER")) {
                        return CompletableFuture.failedFuture(new IllegalAccessException("Sem permissão para limpar home"));
                    }
                    return plugin.getDatabaseManager().clearClanHomeAsync(clan.getId());
                }, plugin.getThreadPool())
                .thenAccept(success -> {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        if (success) {
                            messages.sendMessage(player, "home-cleared");
                        } else {
                            messages.sendMessage(player, "generic-error");
                        }
                    });
                })
                .exceptionally(error -> {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        if (error.getCause() instanceof IllegalAccessException) {
                            messages.sendMessage(player, "home-clear-no-permission");
                        } else {
                            messages.sendMessage(player, "generic-error");
                        }
                    });
                    return null;
                });
    }

    @Override
    public List<String> onTabComplete(Player player, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("set", "clear").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}