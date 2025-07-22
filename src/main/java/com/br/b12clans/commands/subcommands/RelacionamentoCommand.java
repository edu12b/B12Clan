// ARQUIVO: src/main/java/com/br/b12clans/commands/subcommands/RelacionamentoCommand.java
package com.br.b12clans.commands.subcommands;

import com.br.b12clans.Main;
import com.br.b12clans.managers.ClanManager;
import com.br.b12clans.models.Clan;
import com.br.b12clans.utils.MessagesManager;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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
        return "relacionamento"; // Nome interno da classe
    }

    @Override
    public String getPermission() {
        return null; // A permissão é baseada no cargo, verificada internamente
    }

    @Override
    public void execute(Player player, String[] args) {
        // A lógica será chamada pelo método handleCommand, não diretamente aqui.
    }

    public void handleCommand(Player player, String[] args, boolean isAlly) {
        Clan clan = clanManager.getPlayerClan(player.getUniqueId());
        if (clan == null) {
            messages.sendMessage(player, "no-clan");
            return;
        }

        String usageMessage = isAlly ? "ally-usage" : "rival-usage";
        if (args.length < 2) {
            messages.sendMessage(player, usageMessage);
            return;
        }

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            String role = plugin.getDatabaseManager().getMemberRole(clan.getId(), player.getUniqueId());
            boolean canManage = (role != null && (role.equals("OWNER") || (role.equals("VICE_LEADER") && canViceLeaderManage(isAlly))));

            if (!canManage) {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    messages.sendMessage(player, isAlly ? "ally-no-permission" : "rival-no-permission");
                });
                return;
            }

            String action = args[0].toLowerCase();
            String targetTag = args[1];

            Clan targetClan = plugin.getDatabaseManager().getClanByTag(targetTag);
            if (targetClan == null) {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    messages.sendMessage(player, "clan-not-found", "%tag%", targetTag);
                });
                return;
            }

            if (targetClan.getId() == clan.getId()) {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    messages.sendMessage(player, isAlly ? "cannot-ally-self" : "cannot-rival-self");
                });
                return;
            }

            // Lógica para adicionar ou remover
            if ("add".equals(action)) {
                handleAddRelationship(player, clan, targetClan, isAlly);
            } else if ("remove".equals(action)) {
                handleRemoveRelationship(player, clan, targetClan, isAlly);
            } else {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    messages.sendMessage(player, usageMessage);
                });
            }
        });
    }

    private void handleAddRelationship(Player player, Clan sourceClan, Clan targetClan, boolean isAlly) {
        boolean alreadyExists = isAlly ? plugin.getDatabaseManager().areAllies(sourceClan.getId(), targetClan.getId())
                : plugin.getDatabaseManager().areRivals(sourceClan.getId(), targetClan.getId());
        if (alreadyExists) {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                messages.sendMessage(player, isAlly ? "already-allies" : "already-rivals", "%clan_name%", targetClan.getName());
            });
            return;
        }

        boolean success = isAlly ? plugin.getDatabaseManager().addAlly(sourceClan.getId(), targetClan.getId())
                : plugin.getDatabaseManager().addRival(sourceClan.getId(), targetClan.getId());

        if (success) {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                messages.sendMessage(player, isAlly ? "ally-added" : "rival-added", "%clan_name%", targetClan.getName());
            });
        }
    }

    private void handleRemoveRelationship(Player player, Clan sourceClan, Clan targetClan, boolean isAlly) {
        boolean relationshipExists = isAlly ? plugin.getDatabaseManager().areAllies(sourceClan.getId(), targetClan.getId())
                : plugin.getDatabaseManager().areRivals(sourceClan.getId(), targetClan.getId());
        if (!relationshipExists) {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                messages.sendMessage(player, isAlly ? "not-allies" : "not-rivals", "%clan_name%", targetClan.getName());
            });
            return;
        }

        boolean success = isAlly ? plugin.getDatabaseManager().removeAlly(sourceClan.getId(), targetClan.getId())
                : plugin.getDatabaseManager().removeRival(sourceClan.getId(), targetClan.getId());

        if (success) {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                messages.sendMessage(player, isAlly ? "ally-removed" : "rival-removed", "%clan_name%", targetClan.getName());
            });
        }
    }

    private boolean canViceLeaderManage(boolean isAlly) {
        String configPath = isAlly ? "permissions.vice-leader-can-manage-allies" : "permissions.vice-leader-can-manage-rivals";
        return plugin.getConfig().getBoolean(configPath, true);
    }

    @Override
    public List<String> onTabComplete(Player player, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("add", "remove").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        // Tab-complete para a tag do clã seria complexo, então deixamos sem sugestão por enquanto.
        return Collections.emptyList();
    }
}