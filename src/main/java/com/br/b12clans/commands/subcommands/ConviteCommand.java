// ARQUIVO: src/main/java/com/br/b12clans/commands/subcommands/ConviteCommand.java
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
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class ConviteCommand implements SubCommand {

    private final Main plugin;
    private final ClanManager clanManager;
    private final MessagesManager messages;

    public ConviteCommand(Main plugin) {
        this.plugin = plugin;
        this.clanManager = plugin.getClanManager();
        this.messages = plugin.getMessagesManager();
    }

    @Override
    public String getName() {
        return "convite";
    }

    @Override
    public String getPermission() {
        return null;
    }

    @Override
    public void execute(Player player, String[] args) {
        if (args.length < 1) {
            messages.sendMessage(player, "convite-usage");
            return;
        }

        String action = args[0].toLowerCase();
        String[] actionArgs = Arrays.copyOfRange(args, 1, args.length);

        switch (action) {
            case "add":
                handleConvidar(player, actionArgs);
                break;
            case "accept": case "aceitar":
                handleAceitar(player, actionArgs);
                break;
            case "deny": case "negar":
                handleNegar(player, actionArgs);
                break;
            default:
                messages.sendMessage(player, "convite-usage");
                break;
        }
    }

    private void handleConvidar(Player player, String[] args) {
        Clan inviterClan = clanManager.getPlayerClan(player.getUniqueId());
        if (inviterClan == null) {
            messages.sendMessage(player, "no-clan");
            return;
        }

        if (args.length < 1) {
            messages.sendMessage(player, "invite-usage");
            return;
        }

        Player target = Bukkit.getPlayer(args[0]);
        // ... (verificações síncronas permanecem as mesmas)

        plugin.getDatabaseManager().getMemberRoleAsync(inviterClan.getId(), player.getUniqueId())
                .thenAccept(role -> {
                    // CÓDIGO CORRIGIDO: Voltando para a thread principal com runTask
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        if (role == null || !(role.equals("OWNER") || role.equals("VICE_LEADER") || role.equals("ADMIN"))) {
                            messages.sendMessage(player, "invite-no-permission");
                            return;
                        }
                        clanManager.addInvite(target.getUniqueId(), inviterClan.getId());
                        messages.sendMessage(player, "invite-sent", "%player_name%", target.getName());
                        messages.sendMessage(target, "invite-received",
                                "%clan_name%", inviterClan.getName(),
                                "%clan_tag%", clanManager.getCleanTag(inviterClan.getTag()));
                    });
                })
                .exceptionally(error -> {
                    plugin.getServer().getScheduler().runTask(plugin, () -> messages.sendMessage(player, "generic-error"));
                    plugin.getLogger().warning("Erro ao verificar cargo para convite: " + error.getMessage());
                    return null;
                });
    }

    private void handleAceitar(Player player, String[] args) {
        if (args.length < 1) {
            messages.sendMessage(player, "accept-usage");
            return;
        }

        Integer invitedClanId = clanManager.getPendingInvite(player.getUniqueId());
        if (invitedClanId == null) {
            messages.sendMessage(player, "no-pending-invite");
            return;
        }

        clanManager.getClanById(invitedClanId)
                .thenComposeAsync(clan -> {
                    if (clan == null || !clanManager.getCleanTag(clan.getTag()).equalsIgnoreCase(args[0])) {
                        return CompletableFuture.failedFuture(new IllegalAccessException("Convite inválido"));
                    }
                    return plugin.getDatabaseManager().addClanMemberAsync(clan.getId(), player.getUniqueId(), player.getName())
                            .thenApply(success -> {
                                if (!success) throw new RuntimeException("Falha ao adicionar membro no DB");
                                return clan;
                            });
                }, plugin.getThreadPool())
                .thenAccept(clan -> {
                    // CÓDIGO CORRIGIDO: Voltando para a thread principal com runTask
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        clanManager.removeInvite(player.getUniqueId());
                        clanManager.loadPlayerClan(player.getUniqueId());
                        messages.sendMessage(player, "invite-accepted", "%clan_name%", clan.getName());
                        clanManager.broadcastToClan(clan, "player-joined-clan-broadcast", "%player_name%", player.getName());
                        if (plugin.getConfig().getBoolean("discord.enabled", false)) {
                            plugin.getDiscordManager().onMemberJoined(clan, player);
                        }
                    });
                })
                .exceptionally(error -> {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        if (error.getCause() instanceof IllegalAccessException) {
                            messages.sendMessage(player, "no-pending-invite");
                        } else {
                            messages.sendMessage(player, "generic-error");
                            plugin.getLogger().warning("Erro ao aceitar convite: " + error.getMessage());
                        }
                    });
                    return null;
                });
    }

    private void handleNegar(Player player, String[] args) {
        if (args.length < 1) {
            messages.sendMessage(player, "deny-usage");
            return;
        }

        Integer invitedClanId = clanManager.getPendingInvite(player.getUniqueId());
        if (invitedClanId == null) {
            messages.sendMessage(player, "no-pending-invite");
            return;
        }

        clanManager.getClanById(invitedClanId)
                .thenAccept(clan -> {
                    // CÓDIGO CORRIGIDO: Voltando para a thread principal com runTask
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        if (clan != null && clanManager.getCleanTag(clan.getTag()).equalsIgnoreCase(args[0])) {
                            clanManager.removeInvite(player.getUniqueId());
                            messages.sendMessage(player, "invite-denied", "%clan_name%", clan.getName());
                        } else {
                            messages.sendMessage(player, "no-pending-invite");
                        }
                    });
                });
    }

    @Override
    public List<String> onTabComplete(Player player, String[] args) {
        // ... (lógica de tab-complete permanece a mesma)
        if (args.length == 1) {
            return Arrays.asList("add", "accept", "deny").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("add")) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }
        return Collections.emptyList();
    }
}