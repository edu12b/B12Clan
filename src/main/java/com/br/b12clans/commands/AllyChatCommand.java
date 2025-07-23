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

public class AllyChatCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;
    private final ClanManager clanManager;
    private final ClanChatManager chatManager;
    private final MessagesManager messages;

    public AllyChatCommand(Main plugin) {
        this.plugin = plugin;
        this.chatManager = plugin.getClanChatManager();
        this.clanManager = plugin.getClanManager(); // <--- Add this line
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
            messages.sendMessage(player, "ally-chat-usage");
            return true;
        }

        String firstArg = args[0].toLowerCase();

        switch (firstArg) {
            case "join":
                chatManager.setPlayerChannel(player.getUniqueId(), ClanChatManager.ChatChannel.ALLY);
                messages.sendMessage(player, "ally-chat-joined");
                break;
            case "leave":
                chatManager.setPlayerChannel(player.getUniqueId(), ClanChatManager.ChatChannel.NONE);
                messages.sendMessage(player, "ally-chat-left");
                break;
            case "mute":
                chatManager.toggleMute(player.getUniqueId());
                if (chatManager.isMuted(player.getUniqueId())) {
                    messages.sendMessage(player, "ally-chat-muted");
                } else {
                    messages.sendMessage(player, "ally-chat-unmuted");
                }
                break;
            default:
                // Enviar mensagem para o chat dos aliados
                String message = String.join(" ", args);
                chatManager.sendAllyMessage(player, message);
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
