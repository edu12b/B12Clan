package com.br.b12clans.commands;

import com.br.b12clans.Main;
import com.br.b12clans.chat.DiscordManager;
import com.br.b12clans.utils.MessagesManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DiscordCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;
    private final DiscordManager discordManager;
    private final MessagesManager messages;

    public DiscordCommand(Main plugin) {
        this.plugin = plugin;
        this.discordManager = plugin.getDiscordManager();
        this.messages = plugin.getMessagesManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            messages.sendMessage(sender, "player-only");
            return true;
        }

        Player player = (Player) sender;

        if (!plugin.getConfig().getBoolean("discord.enabled", false)) {
            messages.sendMessage(player, "discord-disabled");
            return true;
        }

        if (label.equalsIgnoreCase("vincular") ||
                (args.length > 0 && args[0].equalsIgnoreCase("vincular"))) {

            if (discordManager.isPlayerVerified(player.getUniqueId())) {
                messages.sendMessage(player, "discord-already-verified");
                return true;
            }

            String code = discordManager.generateVerificationCode(player.getUniqueId());
            if (code == null) {
                messages.sendMessage(player, "discord-already-verified");
                return true;
            }

            messages.sendMessage(player, "discord-verification-code", "%code%", code);
            messages.sendMessage(player, "discord-verification-instructions-slash", "%code%", code);

            return true;
        }

        if (label.equalsIgnoreCase("desvincular") ||
                (args.length > 0 && args[0].equalsIgnoreCase("desvincular"))) {

            if (!discordManager.isPlayerVerified(player.getUniqueId())) {
                messages.sendMessage(player, "discord-not-verified");
                return true;
            }

            boolean success = discordManager.unlinkPlayer(player.getUniqueId());
            if (success) {
                messages.sendMessage(player, "discord-unlinked");
            } else {
                messages.sendMessage(player, "discord-unlink-failed");
            }

            return true;
        }

        if (args.length == 0) {
            sendDiscordHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "status":
                if (discordManager.isPlayerVerified(player.getUniqueId())) {
                    messages.sendMessage(player, "discord-verified");
                } else {
                    messages.sendMessage(player, "discord-not-verified");
                }
                break;
            default:
                sendDiscordHelp(player);
                break;
        }

        return true;
    }

    private void sendDiscordHelp(Player player) {
        messages.sendMessage(player, "discord-help-header");
        messages.sendMessage(player, "discord-help-vincular");
        messages.sendMessage(player, "discord-help-desvincular");
        messages.sendMessage(player, "discord-help-status");
        messages.sendMessage(player, "discord-help-slash");
        messages.sendMessage(player, "discord-help-footer");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("vincular", "desvincular", "status");
        }
        return Collections.emptyList();
    }
}
