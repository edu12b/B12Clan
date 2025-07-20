package com.br.b12clans.chat;

import com.br.b12clans.Main;
import com.br.b12clans.managers.ClanManager;
import com.br.b12clans.models.Clan;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ClanChatManager {

    private final Main plugin;
    private final Map<UUID, ChatChannel> playerChannels;
    private final Set<UUID> mutedPlayers;

    public ClanChatManager(Main plugin) {
        this.plugin = plugin;
        this.playerChannels = new ConcurrentHashMap<>();
        this.mutedPlayers = ConcurrentHashMap.newKeySet();
    }

    public enum ChatChannel {
        CLAN, ALLY, NONE
    }

    public void setPlayerChannel(UUID playerUuid, ChatChannel channel) {
        if (channel == ChatChannel.NONE) {
            playerChannels.remove(playerUuid);
        } else {
            playerChannels.put(playerUuid, channel);
        }
    }

    public ChatChannel getPlayerChannel(UUID playerUuid) {
        return playerChannels.getOrDefault(playerUuid, ChatChannel.NONE);
    }

    public void toggleMute(UUID playerUuid) {
        if (mutedPlayers.contains(playerUuid)) {
            mutedPlayers.remove(playerUuid);
        } else {
            mutedPlayers.add(playerUuid);
        }
    }

    public boolean isMuted(UUID playerUuid) {
        return mutedPlayers.contains(playerUuid);
    }

    public void sendClanMessage(Player sender, String message) {
        Clan senderClan = plugin.getClanManager().getPlayerClan(sender.getUniqueId());
        if (senderClan == null) {
            plugin.getMessagesManager().sendMessage(sender, "no-clan");
            return;
        }

        String formattedMessage = formatClanMessage(sender, message);
        
        // Enviar para todos os membros online do clã
        for (Player player : Bukkit.getOnlinePlayers()) {
            Clan playerClan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
            if (playerClan != null && playerClan.getId() == senderClan.getId()) {
                if (!isMuted(player.getUniqueId())) {
                    player.sendMessage(formattedMessage);
                }
            }
        }

        // Enviar para Discord se habilitado
        if (plugin.getConfig().getBoolean("discord.enabled", false)) {
            plugin.getDiscordManager().sendClanMessage(senderClan, sender.getName(), message);
        }
    }

    public void sendAllyMessage(Player sender, String message) {
        Clan senderClan = plugin.getClanManager().getPlayerClan(sender.getUniqueId());
        if (senderClan == null) {
            plugin.getMessagesManager().sendMessage(sender, "no-clan");
            return;
        }

        String formattedMessage = formatAllyMessage(sender, message);
        
        // TODO: Implementar sistema de alianças
        // Por enquanto, apenas envia para o próprio clã
        for (Player player : Bukkit.getOnlinePlayers()) {
            Clan playerClan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
            if (playerClan != null && playerClan.getId() == senderClan.getId()) {
                if (!isMuted(player.getUniqueId())) {
                    player.sendMessage(formattedMessage);
                }
            }
        }
    }

    private String formatClanMessage(Player sender, String message) {
        String format = plugin.getConfig().getString("chat.clan-format", "&8[&6CLÃN&8] &7%player%&8: &f%message%");
        Clan clan = plugin.getClanManager().getPlayerClan(sender.getUniqueId());
        String clanTag = clan != null ? plugin.getClanManager().translateColors(clan.getTag()) : "";
        
        return plugin.getClanManager().translateColors(format
                .replace("%player%", sender.getName())
                .replace("%clan_tag%", clanTag)
                .replace("%clan_name%", clan != null ? clan.getName() : "")
                .replace("%message%", message));
    }

    private String formatAllyMessage(Player sender, String message) {
        String format = plugin.getConfig().getString("chat.ally-format", "&8[&aALIADO&8] &7%player%&8: &f%message%");
        Clan clan = plugin.getClanManager().getPlayerClan(sender.getUniqueId());
        String clanTag = clan != null ? plugin.getClanManager().translateColors(clan.getTag()) : "";
        
        return plugin.getClanManager().translateColors(format
                .replace("%player%", sender.getName())
                .replace("%clan_tag%", clanTag)
                .replace("%clan_name%", clan != null ? clan.getName() : "")
                .replace("%message%", message));
    }

    public void handlePlayerLeave(UUID playerUuid) {
        playerChannels.remove(playerUuid);
        mutedPlayers.remove(playerUuid);
    }
}
