package com.br.b12clans.commands;

import com.br.b12clans.Main;
import com.br.b12clans.chat.ClanChatManager;
import com.br.b12clans.managers.ClanManager;
import com.br.b12clans.models.Clan;
import com.br.b12clans.utils.MessagesManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ClanChatCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;
    private final ClanManager clanManager;
    private final ClanChatManager chatManager;
    private final MessagesManager messages;

    public ClanChatCommand(Main plugin) {
        this.plugin = plugin;
        this.clanManager = plugin.getClanManager(); // <--- Add this line
        this.chatManager = plugin.getClanChatManager();
        this.messages = plugin.getMessagesManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            messages.sendMessage(sender, "player-only");
            return true;
        }

        Player player = (Player) sender;
        Clan clan = clanManager.getPlayerClan(player.getUniqueId());
        
        if (clan == null) {
            messages.sendMessage(player, "no-clan");
            return true;
        }

        if (args.length == 0) {
            messages.sendMessage(player, "clan-chat-usage");
            return true;
        }

        String firstArg = args[0].toLowerCase();

        switch (firstArg) {
            case "join":
                chatManager.setPlayerChannel(player.getUniqueId(), ClanChatManager.ChatChannel.CLAN);
                messages.sendMessage(player, "clan-chat-joined");
                break;
            case "leave":
                chatManager.setPlayerChannel(player.getUniqueId(), ClanChatManager.ChatChannel.NONE);
                messages.sendMessage(player, "clan-chat-left");
                break;
            case "mute":
                chatManager.toggleMute(player.getUniqueId());
                if (chatManager.isMuted(player.getUniqueId())) {
                    messages.sendMessage(player, "clan-chat-muted");
                } else {
                    messages.sendMessage(player, "clan-chat-unmuted");
                }
                break;
            default:
                // Enviar mensagem para o chat do clã
                String message = String.join(" ", args);
                chatManager.sendClanMessage(player, message);
                break;
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("join", "leave", "mute");
        }
        return Collections.emptyList();
    }
}
