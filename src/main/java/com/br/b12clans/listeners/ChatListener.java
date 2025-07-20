package com.br.b12clans.listeners;

import com.br.b12clans.Main;
import com.br.b12clans.chat.ClanChatManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListener implements Listener {

    private final Main plugin;
    private final ClanChatManager chatManager;

    public ChatListener(Main plugin) {
        this.plugin = plugin;
        this.chatManager = plugin.getClanChatManager();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        ClanChatManager.ChatChannel channel = chatManager.getPlayerChannel(player.getUniqueId());

        if (channel == ClanChatManager.ChatChannel.CLAN) {
            event.setCancelled(true);
            // Executar no thread principal
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                chatManager.sendClanMessage(player, event.getMessage());
            });
        } else if (channel == ClanChatManager.ChatChannel.ALLY) {
            event.setCancelled(true);
            // Executar no thread principal
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                chatManager.sendAllyMessage(player, event.getMessage());
            });
        }
    }
}
