package com.br.b12clans.commands.subcommands;

import com.br.b12clans.Main;
import com.br.b12clans.managers.ClanManager;
import com.br.b12clans.managers.CommandManager;
import com.br.b12clans.models.Clan;
import com.br.b12clans.utils.MessagesManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class ConviteCommand implements SubCommand {

    private final Main plugin;
    private final ClanManager clanManager;
    private final MessagesManager messages;
    private final CommandManager commandManager;

    public ConviteCommand(Main plugin) {
        this.plugin = plugin;
        this.clanManager = plugin.getClanManager();
        this.messages = plugin.getMessagesManager();
        this.commandManager = plugin.getCommandManager();
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

        if (commandManager.getActionAliasesFor("invite", "add").contains(action)) {
            handleAdd(player, args);
        } else if (commandManager.getActionAliasesFor("invite", "accept").contains(action)) {
            handleAccept(player, args);
        } else if (commandManager.getActionAliasesFor("invite", "deny").contains(action)) {
            handleDeny(player, args);
        } else {
            messages.sendMessage(player, "convite-usage");
        }
    }

    private void handleAdd(Player player, String[] args) {
        if (args.length < 2) {
            messages.sendMessage(player, "convite-usage");
            return;
        }
        Clan clan = clanManager.getPlayerClan(player.getUniqueId());
        if (clan == null) {
            messages.sendMessage(player, "no-clan");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            messages.sendMessage(player, "player-not-found", "%player_name%", args[1]);
            return;
        }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            messages.sendMessage(player, "cannot-invite-self");
            return;
        }
        if (clanManager.getPlayerClan(target.getUniqueId()) != null) {
            messages.sendMessage(player, "target-already-in-clan", "%player_name%", target.getName());
            return;
        }

        // Inicia a cadeia de verificações assíncronas
        plugin.getDatabaseManager().getMemberRoleAsync(clan.getId(), player.getUniqueId())
                .thenComposeAsync(role -> {
                    if (role == null || !(role.equals("OWNER") || role.equals("VICE_LEADER") || role.equals("ADMIN"))) {
                        return CompletableFuture.failedFuture(new IllegalAccessException("invite-no-permission"));
                    }
                    // A seguir, verifica a configuração de convites do jogador alvo
                    return clanManager.isInvitesDisabledAsync(target.getUniqueId());
                }, plugin.getThreadPool())
                .thenAccept(targetIsDisabled -> {
                    // Se chegou aqui, as verificações passaram. Agora age na thread principal.
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        if (targetIsDisabled) {
                            messages.sendMessage(player, "target-invites-disabled", "%player_name%", target.getName());
                            return;
                        }

                        // Se o alvo não desativou os convites, envia o convite.
                        clanManager.addInvite(target.getUniqueId(), clan.getId());
                        messages.sendMessage(player, "invite-sent", "%player_name%", target.getName());
                        messages.sendMessage(target, "invite-received",
                                "%source_clan%", clan.getName(),
                                "%source_clan_tag%", clanManager.getCleanTag(clan.getTag()));
                    });
                })
                .exceptionally(error -> {
                    plugin.getAsyncHandler().handleException(player, error, "generic-error");
                    return null;
                });
    }

    private void handleAccept(Player player, String[] args) {
        if (args.length < 2) {
            messages.sendMessage(player, "convite-usage"); // Ajustar para uma mensagem mais específica
            return;
        }
        String clanTag = args[1];
        Integer clanId = clanManager.getPendingInvite(player.getUniqueId());

        if (clanId == null) {
            messages.sendMessage(player, "no-pending-invite");
            return;
        }

        clanManager.getClanById(clanId).thenComposeAsync(clan -> {
            if (clan == null || !clanManager.getCleanTag(clan.getTag()).equalsIgnoreCase(clanTag)) {
                return CompletableFuture.failedFuture(new IllegalAccessException("Convite inválido"));
            }
            // CORREÇÃO 1: Nome do método correto (addClanMemberAsync) e argumento correto (player.getName())
            return plugin.getDatabaseManager().addClanMemberAsync(clan.getId(), player.getUniqueId(), player.getName())
                    .thenApply(success -> {
                        if (!success) {
                            throw new RuntimeException("Falha ao adicionar membro no DB");
                        }
                        return clan;
                    });
        }, plugin.getThreadPool()).thenAccept(clan -> { // CORREÇÃO 2: O tipo 'clan' agora é reconhecido corretamente
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                clanManager.removeInvite(player.getUniqueId());
                clanManager.loadPlayerClan(player.getUniqueId());
                messages.sendMessage(player, "invite-accepted", "%clan_name%", clan.getName());
                clanManager.broadcastToClan(clan, "player-joined-clan-broadcast", "%player_name%", player.getName());
            });
        }).exceptionally(error -> {
            // Uma única linha que faz todo o trabalho!
            plugin.getAsyncHandler().handleException(player, error, "generic-error");
            return null;
        });
    }

    private void handleDeny(Player player, String[] args) {
        if (args.length < 2) {
            messages.sendMessage(player, "convite-usage"); // Ajustar para uma mensagem mais específica
            return;
        }
        String clanTag = args[1];
        Integer clanId = clanManager.getPendingInvite(player.getUniqueId());

        if (clanId == null) {
            messages.sendMessage(player, "no-pending-invite");
            return;
        }

        clanManager.removeInvite(player.getUniqueId());
        messages.sendMessage(player, "invite-denied", "%clan_name%", clanTag);
    }

    @Override
    public List<String> onTabComplete(Player player, String[] args) {
        if (args.length == 1) {
            List<String> allActions = commandManager.getActionAliasesFor("invite", "add");
            allActions.addAll(commandManager.getActionAliasesFor("invite", "accept"));
            allActions.addAll(commandManager.getActionAliasesFor("invite", "deny"));

            return allActions.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2) {
            String action = args[0].toLowerCase();
            if (commandManager.getActionAliasesFor("invite", "add").contains(action)) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }
        return Collections.emptyList();
    }
}