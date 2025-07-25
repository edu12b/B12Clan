// ARQUIVO: src/main/java/com/br/b12clans/chat/ClanChatManager.java
package com.br.b12clans.chat;

import com.br.b12clans.Main;
import com.br.b12clans.managers.ClanManager;
import com.br.b12clans.models.Clan;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.List;
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

        for (Player player : Bukkit.getOnlinePlayers()) {
            Clan playerClan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
            if (playerClan != null && playerClan.getId() == senderClan.getId()) {
                if (!isMuted(player.getUniqueId())) {
                    player.sendMessage(formattedMessage);
                }
            }
        }

        if (plugin.getConfig().getBoolean("discord.enabled", false)) {
            plugin.getDiscordManager().sendClanMessage(senderClan, sender.getName(), message);
        }
    }

    // ##### MÉTODO CORRIGIDO #####
    public void sendAllyMessage(Player sender, String message) {
        Clan senderClan = plugin.getClanManager().getPlayerClan(sender.getUniqueId());
        if (senderClan == null) {
            plugin.getMessagesManager().sendMessage(sender, "no-clan");
            return;
        }

        String formattedMessage = formatAllyMessage(sender, message);

        // Usamos o novo método com cache do ClanManager
        plugin.getClanManager().getClanAlliesAsync(senderClan.getId()).thenAccept(allyIds -> {
            Set<Integer> recipientClanIds = new HashSet<>(allyIds);
            recipientClanIds.add(senderClan.getId()); // Adiciona o próprio clã

            // Volta para a thread principal para enviar as mensagens
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    Clan playerClan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
                    // Verifica se o clã do jogador está na nossa lista de destinatários
                    if (playerClan != null && recipientClanIds.contains(playerClan.getId())) {
                        if (!isMuted(player.getUniqueId())) {
                            player.sendMessage(formattedMessage);
                        }
                    }
                }
            });
        });
    }
    private String formatClanMessage(Player sender, String message) {
        String format = plugin.getConfig().getString("chat.clan-format", "&8[&6CLÃ&8] &7%player%&8: &f%message%");

        StringBuilder sb = new StringBuilder(format);

        replace(sb, "%player%", sender.getName());
        replace(sb, "%message%", message);

        return plugin.getMessagesManager().translateColors(sb.toString());
    }

    private String formatAllyMessage(Player sender, String message) {
        String format = plugin.getConfig().getString("chat.ally-format", "&8[&aALIADO&8] &7%player%&8: &f%message%");
        Clan clan = plugin.getClanManager().getPlayerClan(sender.getUniqueId());
        String clanTag = clan != null ? plugin.getClanManager().getCleanTag(clan.getTag()) : "";

        StringBuilder sb = new StringBuilder(format);

        replace(sb, "%player%", sender.getName());
        replace(sb, "%clan_tag%", clanTag);
        replace(sb, "%message%", message);

        return plugin.getMessagesManager().translateColors(sb.toString());
    }

    // Crie este método auxiliar dentro de ClanChatManager para evitar repetição
    private void replace(StringBuilder sb, String placeholder, String value) {
        int index;
        while ((index = sb.indexOf(placeholder)) != -1) {
            sb.replace(index, index + placeholder.length(), value);
        }
    }

    public void handlePlayerLeave(UUID playerUuid) {
        playerChannels.remove(playerUuid);
        mutedPlayers.remove(playerUuid);
    }
}