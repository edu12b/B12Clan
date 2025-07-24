package com.br.b12clans.commands;

import com.br.b12clans.Main;
import com.br.b12clans.chat.ClanChatManager;
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
    private final ClanChatManager chatManager;
    private final MessagesManager messages;

    public AllyChatCommand(Main plugin) {
        this.plugin = plugin;
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
        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());

        if (clan == null) {
            messages.sendMessage(player, "no-clan");
            return true;
        }

        // ##### NOVA VERIFICAÇÃO AQUI #####
        plugin.getDatabaseManager().hasAlliesAsync(clan.getId()).thenAccept(hasAllies -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (!hasAllies) {
                    messages.sendMessage(player, "ally-chat-no-allies");
                    return;
                }

                // Se tiver aliados, continua com a lógica do comando
                if (args.length == 0) {
                    messages.sendMessage(player, "ally-chat-usage");
                    return;
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
                        // A lógica de mute pode ser repensada, mas por enquanto funciona
                        chatManager.toggleMute(player.getUniqueId());
                        if (chatManager.isMuted(player.getUniqueId())) {
                            messages.sendMessage(player, "ally-chat-muted");
                        } else {
                            messages.sendMessage(player, "ally-chat-unmuted");
                        }
                        break;
                    default:
                        String message = String.join(" ", args);
                        chatManager.sendAllyMessage(player, message);
                        break;
                }
            });
        });

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