// ARQUIVO: src/main/java/com/br/b12clans/commands/subcommands/CargoCommand.java
package com.br.b12clans.commands.subcommands;

import com.br.b12clans.Main;
import com.br.b12clans.managers.ClanManager;
import com.br.b12clans.models.Clan;
import com.br.b12clans.utils.MessagesManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class CargoCommand implements SubCommand {

    private final Main plugin;
    private final ClanManager clanManager;
    private final MessagesManager messages;

    public CargoCommand(Main plugin) {
        this.plugin = plugin;
        this.clanManager = plugin.getClanManager();
        this.messages = plugin.getMessagesManager();
    }

    @Override
    public String getName() {
        return "cargo";
    }

    @Override
    public String getPermission() {
        return null;
    }

    @Override
    public void execute(Player player, String[] args) {
        // A lógica é chamada pelo handleCommand na ClanCommand.
    }

    public void handleCommand(Player player, String[] args, boolean isPromoting) {
        Clan clan = clanManager.getPlayerClan(player.getUniqueId());
        if (clan == null) {
            messages.sendMessage(player, "no-clan");
            return;
        }

        if (args.length < 1) {
            messages.sendMessage(player, isPromoting ? "promote-usage" : "demote-usage");
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            messages.sendMessage(player, "player-not-found", "%player_name%", args[0]);
            return;
        }

        UUID targetUuid = target.getUniqueId();
        if (targetUuid.equals(player.getUniqueId())) {
            messages.sendMessage(player, "cannot-manage-self");
            return;
        }

        // Inicia a cadeia assíncrona
        plugin.getDatabaseManager().getMemberRoleAsync(clan.getId(), player.getUniqueId())
                .thenComposeAsync(promoterRole ->
                                plugin.getDatabaseManager().getMemberRoleAsync(clan.getId(), targetUuid)
                                        .thenComposeAsync(targetRole -> {
                                            if (!hasPermissionToModify(promoterRole, targetRole)) {
                                                return CompletableFuture.failedFuture(new IllegalAccessException("Sem permissão de hierarquia"));
                                            }

                                            String newRole = calculateNewRole(targetRole, isPromoting);
                                            if (newRole == null) {
                                                String reason = isPromoting ? "already-max-rank" : "already-min-rank";
                                                return CompletableFuture.failedFuture(new IllegalAccessException(reason));
                                            }

                                            // Encadeia a atualização do cargo, passando o newRole para a próxima etapa
                                            return plugin.getDatabaseManager().updateMemberRoleAsync(clan.getId(), targetUuid, newRole)
                                                    .thenApply(success -> newRole); // Retorna o newRole se a atualização for bem-sucedida

                                        }, plugin.getThreadPool()),
                        plugin.getThreadPool())
                .thenAccept(newRole -> {
                    // Etapa final, na thread principal
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        String actionMessageKey = isPromoting ? "promote-success" : "demote-success";
                        String targetMessageKey = isPromoting ? "got-promoted" : "got-demoted";

                        messages.sendMessage(player, actionMessageKey, "%player_name%", target.getName(), "%role%", newRole);

                        if (target.isOnline()) {
                            messages.sendMessage(target.getPlayer(), targetMessageKey, "%clan_name%", clan.getName(), "%role%", newRole);
                        }

                        clanManager.broadcastToClan(clan, isPromoting ? "member-promoted" : "member-demoted",
                                "%player_name%", target.getName(),
                                "%promoter_name%", player.getName(),
                                "%role%", newRole);
                    });
                })
                .exceptionally(error -> {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        String errorMessage = error.getCause().getMessage();
                        switch (errorMessage) {
                            case "Sem permissão de hierarquia":
                                messages.sendMessage(player, "no-permission-hierarchy");
                                break;
                            case "already-max-rank":
                                messages.sendMessage(player, "player-already-max-role", "%player_name%", target.getName());
                                break;
                            case "already-min-rank":
                                messages.sendMessage(player, "player-already-min-role", "%player_name%", target.getName());
                                break;
                            default:
                                messages.sendMessage(player, "generic-error");
                                plugin.getLogger().warning("Erro ao alterar cargo: " + error.getMessage());
                                break;
                        }
                    });
                    return null;
                });
    }

    private boolean hasPermissionToModify(String promoterRole, String targetRole) {
        if (promoterRole == null || targetRole == null || !promoterRole.equals("OWNER")) {
            return false;
        }
        return !targetRole.equals("OWNER");
    }

    private String calculateNewRole(String currentRole, boolean isPromoting) {
        if (isPromoting) {
            switch (currentRole) {
                case "MEMBER": return "ADMIN";
                case "ADMIN": return "VICE_LEADER";
                default: return null;
            }
        } else {
            switch (currentRole) {
                case "VICE_LEADER": return "ADMIN";
                case "ADMIN": return "MEMBER";
                default: return null;
            }
        }
    }

    @Override
    public List<String> onTabComplete(Player player, String[] args) {
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> !name.equals(player.getName()))
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}