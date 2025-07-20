package com.br.b12clans.listeners;

import com.br.b12clans.Main;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class KDRListener implements Listener {

    private final Main plugin;

    public KDRListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        // Atualizar deaths da vítima
        if (plugin.getClanManager().getPlayerClan(victim.getUniqueId()) != null) {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                plugin.getDatabaseManager().updatePlayerDeaths(victim.getUniqueId(), 1);
            });
        }

        // Atualizar kills do assassino (se for um jogador)
        if (killer != null && plugin.getClanManager().getPlayerClan(killer.getUniqueId()) != null) {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                plugin.getDatabaseManager().updatePlayerKills(killer.getUniqueId(), 1);
            });
        }
    }
}
