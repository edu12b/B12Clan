package com.br.b12clans.commands.subcommands;

import com.br.b12clans.Main;
import com.br.b12clans.managers.ClanManager;
import com.br.b12clans.models.Clan;
import com.br.b12clans.utils.MessagesManager;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class FriendlyFireCommand implements SubCommand {

    private final Main plugin;
    private final ClanManager clanManager;
    private final MessagesManager messages;

    public FriendlyFireCommand(Main plugin) {
        this.plugin = plugin;
        this.clanManager = plugin.getClanManager();
        this.messages = plugin.getMessagesManager();
    }

    @Override
    public String getName() {
        return "friendlyfire";
    }

    @Override
    public String getPermission() {
        return null; // A permissão é ser Dono do clã
    }

    @Override
    public void execute(Player player, String[] args) {
        Clan clan = clanManager.getPlayerClan(player.getUniqueId());
        if (clan == null) {
            messages.sendMessage(player, "no-clan");
            return;
        }

        plugin.getDatabaseManager().getMemberRoleAsync(clan.getId(), player.getUniqueId())
                .thenComposeAsync(role -> {
                    if (role == null || !role.equals("OWNER")) {
                        return CompletableFuture.failedFuture(new IllegalAccessException("ff-no-permission"));
                    }

                    if (args.length == 0) {
                        // A lógica para /clan ff (ver status) permanece a mesma
                        return clanManager.isFriendlyFireDisabled(clan.getId()).thenAccept(isDisabled -> {
                            String status = isDisabled ? "&cDESATIVADO" : "&aATIVADO";
                            messages.sendMessage(player, "ff-status", "%status%", status);
                        }).thenApply(v -> true); // Apenas para satisfazer o tipo de retorno
                    }

                    String action = args[0].toLowerCase();
                    boolean shouldBeDisabled;

                    if (action.equals("off") || action.equals("desativar")) {
                        shouldBeDisabled = true;
                    } else if (action.equals("on") || action.equals("ativar")) {
                        shouldBeDisabled = false;
                    } else {
                        return CompletableFuture.failedFuture(new IllegalAccessException("ff-usage"));
                    }

                    // ##### LÓGICA CORRIGIDA AQUI #####
                    return plugin.getDatabaseManager().updateFriendlyFireAsync(clan.getId(), shouldBeDisabled)
                            .thenApply(success -> {
                                if (success) {
                                    // ATUALIZA O CACHE COM O NOVO VALOR em vez de apenas invalidar
                                    clanManager.updateFriendlyFireCache(clan.getId(), shouldBeDisabled);
                                    messages.sendMessage(player, shouldBeDisabled ? "ff-disabled" : "ff-enabled");
                                } else {
                                    messages.sendMessage(player, "generic-error");
                                }
                                return success; // Retorna o status do sucesso
                            });
                    // #################################

                }, plugin.getThreadPool())
                .exceptionally(error -> {
                    plugin.getAsyncHandler().handleException(player, error, "generic-error");
                    return null;
                });
    }

    @Override
    public List<String> onTabComplete(Player player, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("on", "off");
        }
        return Collections.emptyList();
    }
}