package com.br.b12clans.listeners;

import com.br.b12clans.Main;
import com.br.b12clans.managers.ClanManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    private final Main plugin;
    private final ClanManager clanManager;

    // O construtor volta a receber "Main" para podermos aceder ao Bukkit Scheduler
    public PlayerListener(Main plugin) {
        this.plugin = plugin;
        this.clanManager = plugin.getClanManager();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Inicia uma única tarefa assíncrona para lidar com tudo
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            // 1. Atualiza o nome do jogador no banco de dados
            plugin.getDatabaseManager().updatePlayerName(player.getUniqueId(), player.getName());

            // 2. Carrega os dados do clã do jogador para o cache
            clanManager.loadPlayerClan(player.getUniqueId());
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Remover dados do cache para economizar memória
        clanManager.unloadPlayerClan(event.getPlayer().getUniqueId());
    }
}
