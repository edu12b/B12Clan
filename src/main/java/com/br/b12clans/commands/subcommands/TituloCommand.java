// ARQUIVO: src/main/java/com/br/b12clans/commands/subcommands/TituloCommand.java
package com.br.b12clans.commands.subcommands;

import com.br.b12clans.Main;
import com.br.b12clans.managers.ClanManager;
import com.br.b12clans.models.Clan;
import com.br.b12clans.utils.MessagesManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture; // <-- IMPORTAÇÃO ADICIONADA
import java.util.stream.Collectors;

public class TituloCommand implements SubCommand {

    private final Main plugin;
    private final ClanManager clanManager;
    private final MessagesManager messages;

    public TituloCommand(Main plugin) {
        this.plugin = plugin;
        this.clanManager = plugin.getClanManager();
        this.messages = plugin.getMessagesManager();
    }

    @Override
    public String getName() {
        return "titulo";
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

        if (args.length < 1) {
            messages.sendMessage(player, "title-usage");
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            messages.sendMessage(player, "player-not-found", "%player_name%", args[0]);
            return;
        }

        String title = args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : null;
        if (title != null && title.length() > 50) {
            messages.sendMessage(player, "title-too-long");
            return;
        }
        String coloredTitle = (title != null) ? clanManager.translateColors(title) : "";

        plugin.getDatabaseManager().getMemberRoleAsync(clan.getId(), player.getUniqueId())
                .thenComposeAsync(executorRole -> {
                    if (executorRole == null || !(executorRole.equals("OWNER") || executorRole.equals("VICE_LEADER") || executorRole.equals("ADMIN"))) {
                        return CompletableFuture.failedFuture(new IllegalAccessException("no-permission-to-set-title"));
                    }
                    return plugin.getDatabaseManager().getMemberRoleAsync(clan.getId(), target.getUniqueId());
                }, plugin.getThreadPool())
                .thenComposeAsync(targetRole -> {
                    if (targetRole == null) {
                        return CompletableFuture.failedFuture(new IllegalAccessException("player-not-member"));
                    }
                    return CompletableFuture.supplyAsync(() ->
                            plugin.getDatabaseManager().updateMemberTitle(clan.getId(), target.getUniqueId(), title), plugin.getThreadPool()
                    );
                }, plugin.getThreadPool())
                .thenAccept(success -> {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        if (success) {
                            if (title == null || title.isEmpty()) {
                                messages.sendMessage(player, "title-cleared", "%player_name%", target.getName());
                            } else {
                                messages.sendMessage(player, "title-set", "%player_name%", target.getName(), "%title%", coloredTitle);
                            }
                        } else {
                            messages.sendMessage(player, "generic-error");
                        }
                    });
                })
                .exceptionally(error -> {
                    // Uma única linha que faz todo o trabalho!
                    plugin.getAsyncHandler().handleException(player, error, "generic-error");
                    return null;
                });
    }

    @Override
    public List<String> onTabComplete(Player player, String[] args) {
        if (args.length == 1) {
            Clan clan = clanManager.getPlayerClan(player.getUniqueId());
            if (clan == null) return Collections.emptyList();

            return Bukkit.getOnlinePlayers().stream()
                    .filter(p -> clan.getId() == (clanManager.getPlayerClan(p.getUniqueId()) != null ? clanManager.getPlayerClan(p.getUniqueId()).getId() : -1))
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}