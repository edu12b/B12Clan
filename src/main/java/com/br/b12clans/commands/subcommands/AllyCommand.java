package com.br.b12clans.commands.subcommands;

import com.br.b12clans.Main;
import com.br.b12clans.managers.ClanManager;
import com.br.b12clans.managers.CommandManager;
import com.br.b12clans.models.Clan;
import com.br.b12clans.utils.MessagesManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class AllyCommand implements SubCommand {

    private final Main plugin;
    private final ClanManager clanManager;
    private final MessagesManager messages;
    private final CommandManager commandManager;

    public AllyCommand(Main plugin) {
        this.plugin = plugin;
        this.clanManager = plugin.getClanManager();
        this.messages = plugin.getMessagesManager();
        this.commandManager = plugin.getCommandManager();
    }

    @Override
    public String getName() {
        return "ally";
    }

    @Override
    public String getPermission() {
        return null;
    }

    @Override
    public void execute(Player player, String[] args) {
        Clan playerClan = clanManager.getPlayerClan(player.getUniqueId());
        if (playerClan == null) {
            messages.sendMessage(player, "no-clan");
            return;
        }

        if (args.length < 1) {
            messages.sendMessage(player, "ally-usage-new");
            return;
        }

        String action = args[0].toLowerCase();

        plugin.getDatabaseManager().getMemberRoleAsync(playerClan.getId(), player.getUniqueId())
                .thenAccept(role -> {
                    // A verificação de permissão roda na thread principal para segurança
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        if (role == null || !(role.equals("OWNER") || role.equals("VICE_LEADER"))) {
                            messages.sendMessage(player, "ally-no-permission");
                            return;
                        }
                        if (args.length < 2) {
                            messages.sendMessage(player, "ally-usage-new");
                            return;
                        }
                        String targetTag = args[1];

                        if (commandManager.getActionAliasesFor("ally", "request").contains(action)) {
                            handleAllyRequest(player, playerClan, targetTag);
                        } else if (commandManager.getActionAliasesFor("ally", "accept").contains(action)) {
                            handleAllyAccept(player, playerClan, targetTag);
                        } else if (commandManager.getActionAliasesFor("ally", "deny").contains(action)) {
                            handleAllyDeny(player, playerClan, targetTag);
                        } else if (commandManager.getActionAliasesFor("ally", "remove").contains(action)) {
                            handleAllyRemove(player, playerClan, targetTag);
                        } else {
                            messages.sendMessage(player, "ally-usage-new");
                        }
                    });
                });
    }

    private void handleAllyAccept(Player player, Clan acceptorClan, String requesterTag) {
        Integer requesterClanId = clanManager.getPendingAllianceRequest(acceptorClan.getId());
        if (requesterClanId == null) {
            messages.sendMessage(player, "no-pending-ally-request", "%clan_tag%", requesterTag);
            return;
        }

        clanManager.getClanById(requesterClanId).thenComposeAsync(requesterClan -> {
            if (requesterClan == null || !clanManager.getCleanTag(requesterClan.getTag()).equalsIgnoreCase(requesterTag)) {
                return CompletableFuture.failedFuture(new IllegalAccessException("no-pending-ally-request"));
            }
            return plugin.getDatabaseManager().addAllyAsync(requesterClan.getId(), acceptorClan.getId())
                    .thenApply(success -> {
                        if (!success) {
                            throw new RuntimeException("Falha ao adicionar aliança no DB");
                        }
                        clanManager.invalidateRelationshipCache(acceptorClan.getId());
                        clanManager.invalidateRelationshipCache(requesterClan.getId());
                        clanManager.getClanAlliesAsync(acceptorClan.getId());
                        clanManager.getClanAlliesAsync(requesterClan.getId());
                        return requesterClan;
                    });
        }, plugin.getThreadPool()).thenAccept(requesterClan -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                clanManager.removeAllianceRequest(acceptorClan.getId());
                messages.sendMessage(player, "ally-request-accepted", "%source_clan%", requesterClan.getName());
                clanManager.broadcastToClan(acceptorClan, "ally-added", "%target_clan%", requesterClan.getName());
                clanManager.broadcastToClan(requesterClan, "ally-added", "%target_clan%", acceptorClan.getName());
            });
        }).exceptionally(error -> {
            plugin.getAsyncHandler().handleException(player, error, "generic-error");
            return null;
        });
    }

    // ##### MÉTODO CORRIGIDO E REESCRITO #####
    private void handleAllyRequest(Player player, Clan sourceClan, String targetTag) {
        clanManager.getClanByTagAsync(targetTag)
                .thenComposeAsync(targetClan -> {
                    if (targetClan == null) {
                        return CompletableFuture.failedFuture(new IllegalAccessException("clan-not-found:" + targetTag));
                    }
                    if (targetClan.getId() == sourceClan.getId()) {
                        return CompletableFuture.failedFuture(new IllegalAccessException("cannot-ally-self"));
                    }
                    return CompletableFuture.completedFuture(targetClan);
                }, plugin.getThreadPool())
                .thenComposeAsync(targetClan ->
                        clanManager.areAllianceRequestsDisabledAsync(targetClan.getId()).thenApply(isDisabled -> {
                            if (isDisabled) {
                                throw new RuntimeException("target-alliance-requests-disabled:" + targetClan.getName());
                            }
                            return targetClan;
                        }), plugin.getThreadPool())
                .thenComposeAsync(targetClan ->
                        plugin.getDatabaseManager().areAlliesAsync(sourceClan.getId(), targetClan.getId()).thenApply(areAllies -> {
                            if (areAllies) {
                                throw new RuntimeException("already-allies:" + targetClan.getName());
                            }
                            return targetClan;
                        }), plugin.getThreadPool())
                .thenAccept(targetClan -> {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        clanManager.addAllianceRequest(targetClan.getId(), sourceClan.getId());
                        messages.sendMessage(player, "ally-request-sent", "%target_clan%", targetClan.getName());
                        clanManager.broadcastToClan(targetClan, "ally-request-received",
                                "%source_clan%", sourceClan.getName(),
                                "%source_clan_tag%", clanManager.getCleanTag(sourceClan.getTag()));
                    });
                })
                .exceptionally(error -> {
                    // ##### LÓGICA CORRIGIDA AQUI #####
                    String errorMessage = error.getCause().getMessage();
                    if (errorMessage != null && errorMessage.contains(":")) {
                        String[] parts = errorMessage.split(":", 2);
                        String messageKey = parts[0];
                        String placeholderValue = parts[1];
                        String placeholder;

                        // Agora seleciona o placeholder correto para cada mensagem
                        switch (messageKey) {
                            case "clan-not-found":
                                placeholder = "%tag%";
                                break;
                            case "target-alliance-requests-disabled":
                                placeholder = "%clan_name%"; // <-- A correção principal
                                break;
                            default:
                                placeholder = "%target_clan%";
                                break;
                        }
                        messages.sendMessage(player, messageKey, placeholder, placeholderValue);
                    } else {
                        plugin.getAsyncHandler().handleException(player, error, "generic-error");
                    }
                    return null;
                    // #################################
                });
    }

    private void handleAllyDeny(Player player, Clan acceptorClan, String requesterTag) {
        Integer requesterClanId = clanManager.getPendingAllianceRequest(acceptorClan.getId());
        if (requesterClanId == null) {
            messages.sendMessage(player, "no-pending-ally-request", "%clan_tag%", requesterTag);
            return;
        }

        clanManager.getClanById(requesterClanId).thenAccept(requesterClan -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (requesterClan != null && clanManager.getCleanTag(requesterClan.getTag()).equalsIgnoreCase(requesterTag)) {
                    clanManager.removeAllianceRequest(acceptorClan.getId());
                    messages.sendMessage(player, "ally-request-denied", "%source_clan%", requesterClan.getName());
                    clanManager.broadcastToClan(requesterClan, "ally-request-denied-broadcast", "%target_clan%", acceptorClan.getName());
                } else {
                    messages.sendMessage(player, "no-pending-ally-request", "%clan_tag%", requesterTag);
                }
            });
        });
    }

    private void handleAllyRemove(Player player, Clan sourceClan, String targetTag) {
        clanManager.getClanByTagAsync(targetTag).thenComposeAsync(targetClan -> {
            if (targetClan == null) {
                return CompletableFuture.failedFuture(new IllegalAccessException("clan-not-found"));
            }
            return plugin.getDatabaseManager().removeAllyAsync(sourceClan.getId(), targetClan.getId())
                    .thenCompose(v -> plugin.getDatabaseManager().removeAllyAsync(targetClan.getId(), sourceClan.getId()))
                    .thenApply(success -> {
                        clanManager.invalidateRelationshipCache(sourceClan.getId());
                        clanManager.invalidateRelationshipCache(targetClan.getId());
                        clanManager.getClanAlliesAsync(sourceClan.getId());
                        clanManager.getClanAlliesAsync(targetClan.getId());
                        return targetClan;
                    });
        }, plugin.getThreadPool()).thenAccept(targetClan -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                messages.sendMessage(player, "ally-removed", "%target_clan%", targetClan.getName());
                clanManager.broadcastToClan(targetClan, "ally-removed-broadcast", "%source_clan%", sourceClan.getName());
            });
        }).exceptionally(error -> {
            plugin.getAsyncHandler().handleException(player, error, "generic-error");
            return null;
        });
    }

    @Override
    public List<String> onTabComplete(Player player, String[] args) {
        if (args.length == 1) {
            List<String> allActions = commandManager.getActionAliasesFor("ally", "request");
            allActions.addAll(commandManager.getActionAliasesFor("ally", "accept"));
            allActions.addAll(commandManager.getActionAliasesFor("ally", "deny"));
            allActions.addAll(commandManager.getActionAliasesFor("ally", "remove"));

            return allActions.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            return clanManager.getClanTagSuggestions(player, args[1]);
        }
        return Collections.emptyList();
    }
}