// ARQUIVO: src/main/java/com/br/b12clans/commands/subcommands/RelacionamentoCommand.java
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

public class RelacionamentoCommand implements SubCommand {

    private final Main plugin;
    private final ClanManager clanManager;
    private final MessagesManager messages;

    public RelacionamentoCommand(Main plugin) {
        this.plugin = plugin;
        this.clanManager = plugin.getClanManager();
        this.messages = plugin.getMessagesManager();
    }

    @Override
    public String getName() {
        return "relacionamento";
    }

    @Override
    public String getPermission() {
        return null;
    }

    @Override
    public void execute(Player player, String[] args) {
        // A lógica é chamada pelo handleCommand na ClanCommand.
    }

    public void handleCommand(Player player, String[] args, boolean isAlly) {
        if (isAlly) {
            handleAllyCommand(player, args);
        } else {
            handleRivalCommand(player, args);
        }
    }

    // LÓGICA PARA ALIANÇAS
    private void handleAllyCommand(Player player, String[] args) {
        Clan sourceClan = clanManager.getPlayerClan(player.getUniqueId());
        if (sourceClan == null) {
            messages.sendMessage(player, "no-clan");
            return;
        }

        if (args.length < 2) {
            messages.sendMessage(player, "ally-usage-new");
            return;
        }

        String action = args[0].toLowerCase();
        String targetTag = args[1];

        plugin.getDatabaseManager().getMemberRoleAsync(sourceClan.getId(), player.getUniqueId())
                .thenAccept(role -> {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        if (role == null || !(role.equals("OWNER") || role.equals("VICE_LEADER"))) {
                            messages.sendMessage(player, "ally-no-permission");
                            return;
                        }

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

    private void handleAllyRequest(Player player, Clan sourceClan, String targetTag) {
        clanManager.getClanByTagAsync(targetTag).thenAccept(targetClan -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (targetClan == null) {
                    messages.sendMessage(player, "clan-not-found", "%tag%", targetTag);
                    return;
                }
                if (targetClan.getId() == sourceClan.getId()) {
                    messages.sendMessage(player, "cannot-ally-self");
                    return;
                }

                clanManager.addAllianceRequest(targetClan.getId(), sourceClan.getId());
                messages.sendMessage(player, "ally-request-sent", "%clan_name%", targetClan.getName());
                clanManager.broadcastToClan(targetClan, "ally-request-received", "%clan_name%", sourceClan.getName(), "%clan_tag%", clanManager.getCleanTag(sourceClan.getTag()));
            });
        });
    }

    private void handleAllyAccept(Player player, Clan targetClan, String sourceTag) {
        Integer sourceClanId = clanManager.getPendingAllianceRequest(targetClan.getId());
        if (sourceClanId == null) {
            messages.sendMessage(player, "no-pending-ally-request", "%clan_tag%", sourceTag);
            return;
        }

        clanManager.getClanById(sourceClanId).thenComposeAsync(sourceClan -> {
            if (sourceClan == null || !clanManager.getCleanTag(sourceClan.getTag()).equalsIgnoreCase(sourceTag)) {
                return CompletableFuture.failedFuture(new IllegalAccessException("Pedido inválido"));
            }
            return plugin.getDatabaseManager().addAllyAsync(sourceClan.getId(), targetClan.getId())
                    .thenCompose(v -> plugin.getDatabaseManager().addAllyAsync(targetClan.getId(), sourceClan.getId()))
                    .thenApply(success -> sourceClan);
        }, plugin.getThreadPool()).thenAccept(sourceClan -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                clanManager.removeAllianceRequest(targetClan.getId());
                messages.sendMessage(player, "ally-request-accepted", "%clan_name%", sourceClan.getName());
                clanManager.broadcastToClan(targetClan, "ally-added", "%clan_name%", sourceClan.getName());
                clanManager.broadcastToClan(sourceClan, "ally-added", "%clan_name%", targetClan.getName());
            });
        }).exceptionally(error -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> messages.sendMessage(player, "no-pending-ally-request", "%clan_tag%", sourceTag));
            return null;
        });
    }

    private void handleAllyDeny(Player player, Clan targetClan, String sourceTag) {
        Integer sourceClanId = clanManager.getPendingAllianceRequest(targetClan.getId());
        if (sourceClanId == null) {
            messages.sendMessage(player, "no-pending-ally-request", "%clan_tag%", sourceTag);
            return;
        }
        clanManager.removeAllianceRequest(targetClan.getId());
        messages.sendMessage(player, "ally-request-denied", "%clan_name%", sourceTag);
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
                messages.sendMessage(player, "ally-removed", "%clan_name%", targetClan.getName());
                // Idealmente, teríamos uma mensagem específica para notificar o outro clã
                clanManager.broadcastToClan(targetClan, "ally-removed-broadcast", "%clan_name%", sourceClan.getName());
            });
        });
    }

    // LÓGICA PARA RIVALIDADES
    private void handleRivalCommand(Player player, String[] args) {
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

    // MÉTODO onTabComplete ATUALIZADO
    @Override
    public List<String> onTabComplete(Player player, String[] args) {
        String commandAlias = clanManager.getCommandFromAlias(args.length > 0 ? args[0] : "");
        boolean isAlly = commandAlias.equals("ally");

        // Auto-complete para o primeiro argumento (request, accept, add, remove, etc.)
        if (args.length == 1) {
            List<String> actions = isAlly ?
                    Arrays.asList("request", "accept", "deny", "remove") :
                    Arrays.asList("add", "remove");

            return actions.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        // Auto-complete para o segundo argumento (a tag do clã)
        if (args.length == 2) {
            String action = args[0].toLowerCase();
            Clan playerClan = clanManager.getPlayerClan(player.getUniqueId());
            if (playerClan == null) return Collections.emptyList();

            CompletableFuture<List<String>> futureTags;

            if (isAlly) {
                switch (action) {
                    case "request":
                        futureTags = plugin.getDatabaseManager().getAllClanTagsAsync();
                        break;
                    case "remove":
                        futureTags = plugin.getDatabaseManager().getAllAllyTagsAsync(playerClan.getId());
                        break;
                    default:
                        return Collections.emptyList(); // Não sugere nada para accept/deny
                }
            } else { // É rival
                switch (action) {
                    case "add":
                        futureTags = plugin.getDatabaseManager().getAllClanTagsAsync();
                        break;
                    case "remove":
                        futureTags = plugin.getDatabaseManager().getAllRivalTagsAsync(playerClan.getId());
                        break;
                    default:
                        return Collections.emptyList();
                }
            }

            // Como a busca é assíncrona, não podemos retornar a lista diretamente.
            // O Bukkit não suporta tab-complete assíncrono de forma nativa.
            // A solução é retornar o que já temos em cache ou uma lista de jogadores online como fallback.
            // Para uma implementação completa, precisaríamos de um framework de comandos como o Brigadier.
            // Por enquanto, vamos retornar uma lista de jogadores online, que é melhor que nada.
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