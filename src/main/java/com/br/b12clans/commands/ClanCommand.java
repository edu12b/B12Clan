package com.br.b12clans.commands;

import com.br.b12clans.Main;
import com.br.b12clans.database.ClanExistenceStatus;
import com.br.b12clans.managers.ClanManager;
import com.br.b12clans.models.Clan;
import com.br.b12clans.utils.MessagesManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ClanCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;
    private final ClanManager clanManager;
    private final MessagesManager messages;

    public ClanCommand(Main plugin, ClanManager clanManager, MessagesManager messagesManager) {
        this.plugin = plugin;
        this.clanManager = clanManager;
        this.messages = messagesManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            messages.sendMessage(sender, "player-only");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "criar":
                handleCreateCommand(player, args);
                break;
            case "info":
                handleInfoCommand(player, args);
                break;
            case "ver":
                handleVerCommand(player, args);
                break;
            default:
                sendHelp(player);
                break;
        }

        return true;
    }

    private void handleCreateCommand(Player player, String[] args) {
        if (!player.hasPermission("b12clans.criar")) {
            messages.sendMessage(player, "no-permission");
            return;
        }

        if (args.length < 3) {
            messages.sendMessage(player, "create-usage");
            messages.sendMessage(player, "create-example");
            messages.sendMessage(player, "create-tag-test-tip");
            return;
        }

        String name = args[1];
        String tag = String.join(" ", Arrays.copyOfRange(args, 2, args.length));

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
            // Bloco Assíncrono: verificação no banco de dados
            ClanExistenceStatus status = plugin.getDatabaseManager().clanExists(name, tag);

            // Volta para a Thread Principal para enviar mensagens e criar o clã
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                switch (status) {
                    case EXISTS:
                        messages.sendMessage(player, "clan-exists");
                        break;

                    case DATABASE_ERROR:
                        messages.sendMessage(player, "clan-creation-failed");
                        break;

                    case DOES_NOT_EXIST:
                        // O clã não existe, agora tentamos criar de forma assíncrona
                        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                            boolean success = plugin.getDatabaseManager().createClan(name, tag, player.getUniqueId(), player.getName());

                            // Volta para a thread principal para dar a resposta final
                            plugin.getServer().getScheduler().runTask(plugin, () -> {
                                if (success) {
                                    clanManager.loadPlayerClan(player.getUniqueId());
                                    messages.sendMessage(player, "clan-created-success");
                                    messages.sendMessage(player, "clan-created-name", "%name%", name);
                                    messages.sendMessage(player, "clan-created-tag", "%tag%", clanManager.translateColors(tag));
                                    messages.sendMessage(player, "clan-created-leader", "%leader%", player.getName());
                                } else {
                                    messages.sendMessage(player, "clan-creation-failed");
                                }
                            });
                        });
                        break;
                }
            });
        });
    }

    private void handleInfoCommand(Player player, String[] args) {
        Clan clan = clanManager.getPlayerClan(player.getUniqueId());
        if (clan == null) {
            messages.sendMessage(player, "no-clan");
            return;
        }

        String cleanTag = clanManager.getCleanTag(clan.getTag());
        String expandedTag = clanManager.translateColors(clan.getTag());

        messages.sendMessage(player, "info-header");
        messages.sendMessage(player, "info-name", "%name%", clan.getName());
        messages.sendMessage(player, "info-tag", "%tag%", expandedTag);
        messages.sendMessage(player, "info-clean-tag", "%clean_tag%", cleanTag);
        messages.sendMessage(player, "info-expanded-size", "%length%", String.valueOf(expandedTag.length()));
        messages.sendMessage(player, "info-created-at", "%date%", clan.getCreatedAt().toString());
        messages.sendMessage(player, "info-footer");
    }

    private void handleVerCommand(Player player, String[] args) {
        if (args.length < 2) {
            messages.sendMessage(player, "create-usage");
            return;
        }

        String tag = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        String expandedTag = clanManager.translateColors(tag);
        String cleanTag = clanManager.getCleanTag(tag);

        messages.sendMessage(player, "ver-header");
        messages.sendMessage(player, "ver-original-tag", "%tag%", tag);
        messages.sendMessage(player, "ver-rendered-tag", "%tag_rendered%", expandedTag);
        messages.sendMessage(player, "info-clean-tag", "%clean_tag%", cleanTag);
        messages.sendMessage(player, "info-expanded-size", "%length%", String.valueOf(expandedTag.length()));

        if (clanManager.isValidClanTag(tag)) {
            messages.sendMessage(player, "ver-valid");
        } else {
            messages.sendMessage(player, "ver-invalid");
            if (clanManager.isTagTooLong(tag)) {
                messages.sendMessage(player, "ver-invalid-reason-long");
            } else {
                messages.sendMessage(player, "ver-invalid-reason-content");
            }
        }
        messages.sendMessage(player, "ver-footer");
    }

    private void sendHelp(Player player) {
        messages.sendMessage(player, "help-header");
        messages.sendMessage(player, "help-line-create");
        messages.sendMessage(player, "help-line-info");
        messages.sendMessage(player, "help-line-ver");
        messages.sendMessage(player, "help-footer");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("criar", "info", "ver"));
        }

        return completions;
    }
}