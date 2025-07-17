package com.br.b12clans.listeners;

import com.br.b12clans.B12Clans;
import com.br.b12clans.managers.ClanManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {
    
    private final B12Clans plugin;
    private final ClanManager clanManager;
    
    public PlayerListener(B12Clans plugin) {
        this.plugin = plugin;
        this.clanManager = plugin.getClanManager();
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Carregar dados do clã do jogador de forma assíncrona
        clanManager.loadPlayerClan(event.getPlayer().getUniqueId());
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Remover dados do cache para economizar memória
        clanManager.unloadPlayerClan(event.getPlayer().getUniqueId());
    }
}
