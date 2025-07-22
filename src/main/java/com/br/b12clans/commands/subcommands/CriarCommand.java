// ARQUIVO: src/main/java/com/br/b12clans/commands/subcommands/CriarCommand.java
package com.br.b12clans.commands.subcommands;

import com.br.b12clans.Main;
import com.br.b12clans.database.ClanExistenceStatus;
import com.br.b12clans.managers.ClanManager;
import com.br.b12clans.models.Clan;
import com.br.b12clans.utils.MessagesManager;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CriarCommand implements SubCommand {

    private final Main plugin;
    private final ClanManager clanManager;
    private final MessagesManager messages;

    public CriarCommand(Main plugin) {
        this.plugin = plugin;
        this.clanManager = plugin.getClanManager();
        this.messages = plugin.getMessagesManager();
    }

    @Override
    public String getName() {
        return "criar";
    }

    @Override
    public String getPermission() {
        return "b12clans.criar";
    }

    @Override
    public void execute(Player player, String[] args) {
        if (args.length < 2) {
            messages.sendMessage(player, "create-usage");
            messages.sendMessage(player, "create-example");
            messages.sendMessage(player, "create-tag-test-tip");
            return;
        }

        String name = args[0];
        String tag = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        if (!clanManager.isValidClanName(name)) {
            messages.sendMessage(player, "invalid-name");
            return;
        }

        if (!clanManager.isValidClanTag(tag)) {
            if (clanManager.isTagTooLong(tag)) {
                messages.sendMessage(player, "tag-too-long");
            } else {
                messages.sendMessage(player, "invalid-tag-rules");
                messages.sendMessage(player, "invalid-tag-symbols");
            }
            return;
        }

        Clan existingClan = clanManager.getPlayerClan(player.getUniqueId());
        if (existingClan != null) {
            messages.sendMessage(player, "already-in-clan", "%clan_name%", existingClan.getName());
            return;
        }

        messages.sendMessage(player, "checking-availability");
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            ClanExistenceStatus status = plugin.getDatabaseManager().clanExists(name, tag);
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (status == ClanExistenceStatus.EXISTS) {
                    messages.sendMessage(player, "clan-exists");
                } else if (status == ClanExistenceStatus.DATABASE_ERROR) {
                    messages.sendMessage(player, "clan-creation-failed");
                } else {
                    createClanTask(player, name, tag);
                }
            });
        });
    }

    private void createClanTask(Player player, String name, String tag) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean success = plugin.getDatabaseManager().createClan(name, tag, player.getUniqueId(), player.getName());
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (success) {
                    clanManager.loadPlayerClan(player.getUniqueId());
                    messages.sendMessage(player, "clan-created-success");
                    messages.sendMessage(player, "clan-created-name", "%name%", clanManager.formatDisplayName(name));
                    messages.sendMessage(player, "clan-created-tag", "%tag%", clanManager.translateColors(tag));
                    messages.sendMessage(player, "clan-created-leader", "%leader%", player.getName());

                    if (plugin.getConfig().getBoolean("discord.enabled", false)) {
                        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                            Clan newClan = clanManager.getPlayerClan(player.getUniqueId());
                            if (newClan != null) {
                                plugin.getDiscordManager().onClanCreated(newClan);
                            }
                        }, 20L);
                    }
                } else {
                    messages.sendMessage(player, "clan-creation-failed");
                }
            });
        });
    }

    @Override
    public List<String> onTabComplete(Player player, String[] args) {
        return Collections.emptyList();
    }
}