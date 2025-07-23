// ARQUIVO: src/main/java/com/br/b12clans/commands/subcommands/AllyCommand.java
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

public class AllyCommand implements SubCommand {

    private final Main plugin;
    private final ClanManager clanManager;
    private final MessagesManager messages;

    public AllyCommand(Main plugin) {
        this.plugin = plugin;
        this.clanManager = plugin.getClanManager();
        this.messages = plugin.getMessagesManager();
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
        Clan sourceClan = clanManager.getPlayerClan(player.getUniqueId());
        if (sourceClan == null) {
            messages.sendMessage(player, "no-clan");
            return;
        }

        if (args.length < 1) {
            messages.sendMessage(player, "ally-usage-new");
            return;
        }

        String action = args[0].toLowerCase();

        // Permissão é verificada para todas as ações
        plugin.getDatabaseManager().getMemberRoleAsync(sourceClan.getId(), player.getUniqueId())
                .thenAccept(role -> {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        if (role == null || !(role.equals("OWNER") || role.equals("VICE_LEADER"))) {
                            messages.sendMessage(player, "ally-no-permission");
                            return;
                        }

                        if (args.length < 2 && !action.equals("list")) { // "list" pode não ter argumentos
                            messages.sendMessage(player, "ally-usage-new");
                            return;
                        }

                        String targetTag = args.length > 1 ? args[1] : null;

                        switch (action) {
                            case "request":
                                handleAllyRequest(player, sourceClan, targetTag);
                                break;
                            case "accept":
                                handleAllyAccept(player, sourceClan, targetTag);
                                break;
                            case "deny":
                                handleAllyDeny(player, sourceClan, targetTag);
                                break;
                            case "remove":
                                handleAllyRemove(player, sourceClan, targetTag);
                                break;
                            default:
                                messages.sendMessage(player, "ally-usage-new");
                                break;
                        }
                    });
                });
    }

    // ##### MÉTODO REESCRITO E CORRIGIDO #####
    private void handleAllyRequest(Player player, Clan sourceClan, String targetTag) {
        clanManager.getClanByTagAsync(targetTag).thenAccept(targetClan -> {
            if (targetClan == null) {
                plugin.getServer().getScheduler().runTask(plugin, () -> messages.sendMessage(player, "clan-not-found", "%tag%", targetTag));
                return;
            }
            if (targetClan.getId() == sourceClan.getId()) {
                plugin.getServer().getScheduler().runTask(plugin, () -> messages.sendMessage(player, "cannot-ally-self"));
                return;
            }

            // Verificação de aliança existente
            plugin.getDatabaseManager().areAlliesAsync(sourceClan.getId(), targetClan.getId())
                    .thenAccept(areAllies -> {
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            if (areAllies) {
                                // CORREÇÃO AQUI: Usando o placeholder e o valor corretos
                                messages.sendMessage(player, "already-allies", "%target_clan%", targetClan.getName());
                            } else {
                                // Se não são aliados, envia o pedido
                                clanManager.addAllianceRequest(targetClan.getId(), sourceClan.getId());
                                messages.sendMessage(player, "ally-request-sent", "%target_clan%", targetClan.getName());
                                clanManager.broadcastToClan(targetClan, "ally-request-received", "%source_clan%", sourceClan.getName(), "%source_clan_tag%", clanManager.getCleanTag(sourceClan.getTag()));
                            }
                        });
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
                return CompletableFuture.failedFuture(new IllegalAccessException("Pedido inválido"));
            }
            return plugin.getDatabaseManager().addAllyAsync(requesterClan.getId(), acceptorClan.getId())
                    .thenCompose(v -> plugin.getDatabaseManager().addAllyAsync(acceptorClan.getId(), requesterClan.getId()))
                    .thenApply(success -> requesterClan);
        }, plugin.getThreadPool()).thenAccept(requesterClan -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                clanManager.removeAllianceRequest(acceptorClan.getId());
                messages.sendMessage(player, "ally-request-accepted", "%source_clan%", requesterClan.getName());
                clanManager.broadcastToClan(acceptorClan, "ally-added", "%target_clan%", requesterClan.getName());
                clanManager.broadcastToClan(requesterClan, "ally-added", "%target_clan%", acceptorClan.getName());
            });
        }).exceptionally(error -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> messages.sendMessage(player, "no-pending-ally-request", "%clan_tag%", requesterTag));
            return null;
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
                    // Mensagem para quem negou
                    messages.sendMessage(player, "ally-request-denied", "%source_clan%", requesterClan.getName());
                    // Mensagem para quem teve o pedido negado
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
                return CompletableFuture.failedFuture(new IllegalAccessException("Clan não encontrado"));
            }
            return plugin.getDatabaseManager().removeAllyAsync(sourceClan.getId(), targetClan.getId())
                    .thenCompose(v -> plugin.getDatabaseManager().removeAllyAsync(targetClan.getId(), sourceClan.getId()))
                    .thenApply(success -> targetClan);
        }, plugin.getThreadPool()).thenAccept(targetClan -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                messages.sendMessage(player, "ally-removed", "%target_clan%", targetClan.getName());
                clanManager.broadcastToClan(targetClan, "ally-removed-broadcast", "%source_clan%", sourceClan.getName());
            });
        });
    }

    @Override
    public List<String> onTabComplete(Player player, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("request", "accept", "deny", "remove").stream()
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