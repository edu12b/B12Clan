package com.br.b12clans.listeners;

import com.br.b12clans.Main;
import com.br.b12clans.managers.ClanManager;
import com.br.b12clans.models.Clan;
import com.br.b12clans.models.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    private final Main plugin;
    private final ClanManager clanManager;

    public PlayerListener(Main plugin) {
        this.plugin = plugin;
        this.clanManager = plugin.getClanManager();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getDatabaseManager().updatePlayerName(player.getUniqueId(), player.getName());

            Object[] data = plugin.getDatabaseManager().getMemberDataForCache(player.getUniqueId());
            if (data != null) {
                PlayerData playerData = new PlayerData((int) data[0], (int) data[1], (String) data[2]);
                clanManager.cachePlayerData(player.getUniqueId(), playerData);
            }

            Clan clan = plugin.getDatabaseManager().getClanByPlayer(player.getUniqueId());
            if (clan != null) {
                clanManager.cachePlayerClan(player.getUniqueId(), clan);
                clanManager.isFriendlyFireDisabled(clan.getId());
                clanManager.getClanAlliesAsync(clan.getId());
            }
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Clan clan = clanManager.getPlayerClan(player.getUniqueId());

        if (clan != null) {
            // Lógica corrigida para ser mais segura
            boolean isLastMemberOnline = plugin.getServer().getOnlinePlayers().stream()
                    .map(p -> clanManager.getPlayerClan(p.getUniqueId()))
                    .filter(c -> c != null && c.getId() == clan.getId())
                    .count() <= 1; // Se só tem 1 ou 0 (o que está saindo), então ele é o último

            if (isLastMemberOnline) {
                clanManager.invalidateRelationshipCache(clan.getId());
                clanManager.invalidateFriendlyFireCache(clan.getId());
            }
        }

        clanManager.unloadPlayerClan(player.getUniqueId());
        clanManager.uncachePlayerData(player.getUniqueId());
        plugin.getClanChatManager().handlePlayerLeave(player.getUniqueId());
    }
}