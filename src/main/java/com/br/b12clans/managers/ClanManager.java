package com.br.b12clans.managers;

import com.br.b12clans.B12Clans;
import com.br.b12clans.models.Clan;
import org.bukkit.ChatColor;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class ClanManager {
    
    private final B12Clans plugin;
    private final Map<UUID, Clan> playerClans;
    
    // Padrões de validação
    private static final Pattern HEX_PATTERN = Pattern.compile("&#[a-fA-F0-9]{6}");
    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,16}$");
    private static final Pattern TAG_PATTERN = Pattern.compile("^[a-zA-Z0-9]{2,6}$");
    
    public ClanManager(B12Clans plugin) {
        this.plugin = plugin;
        this.playerClans = new ConcurrentHashMap<>();
    }
    
    public boolean isValidClanName(String name) {
        return name != null && NAME_PATTERN.matcher(name).matches();
    }
    
    public boolean isValidClanTag(String tag) {
        if (tag == null) return false;
        
        // Remove cores para validar apenas o conteúdo
        String cleanTag = ChatColor.stripColor(translateHexColors(tag));
        return TAG_PATTERN.matcher(cleanTag).matches();
    }
    
    public String translateHexColors(String message) {
        if (message == null) return null;
        
        // Traduzir cores hexadecimais &#RRGGBB para formato Bukkit
        return HEX_PATTERN.matcher(message).replaceAll(match -> {
            String hex = match.group().substring(2); // Remove &#
            StringBuilder magic = new StringBuilder("§x");
            for (char c : hex.toCharArray()) {
                magic.append("§").append(c);
            }
            return magic.toString();
        });
    }
    
    public String translateColors(String message) {
        if (message == null) return null;
        
        // Primeiro traduzir hex colors, depois cores normais
        String hexTranslated = translateHexColors(message);
        return ChatColor.translateAlternateColorCodes('&', hexTranslated);
    }
    
    public void loadPlayerClan(UUID playerUuid) {
        plugin.getDatabaseManager().getClanByPlayer(playerUuid)
            .thenAccept(clan -> {
                if (clan != null) {
                    playerClans.put(playerUuid, clan);
                }
            });
    }
    
    public void unloadPlayerClan(UUID playerUuid) {
        playerClans.remove(playerUuid);
    }
    
    public Clan getPlayerClan(UUID playerUuid) {
        return playerClans.get(playerUuid);
    }
    
    public void updatePlayerClan(UUID playerUuid, Clan clan) {
        if (clan != null) {
            playerClans.put(playerUuid, clan);
        } else {
            playerClans.remove(playerUuid);
        }
    }
    
    public String getPlayerClanTag(UUID playerUuid) {
        Clan clan = getPlayerClan(playerUuid);
        return clan != null ? translateColors(clan.getTag()) : "";
    }
    
    public String getPlayerClanName(UUID playerUuid) {
        Clan clan = getPlayerClan(playerUuid);
        return clan != null ? clan.getName() : "";
    }
}
