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
    
    // Padrões de validação mais flexíveis
    private static final Pattern HEX_PATTERN = Pattern.compile("&#[a-fA-F0-9]{6}");
    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{2,32}$");
    private static final Pattern TAG_CLEAN_PATTERN = Pattern.compile("^[a-zA-Z0-9\\[\\]\\$\\$\\-_]{1,16}$");
    
    public ClanManager(B12Clans plugin) {
        this.plugin = plugin;
        this.playerClans = new ConcurrentHashMap<>();
    }
    
    public boolean isValidClanName(String name) {
        return name != null && NAME_PATTERN.matcher(name).matches();
    }
    
    public boolean isValidClanTag(String tag) {
        if (tag == null || tag.trim().isEmpty()) return false;
        
        // Remove cores para validar apenas o conteúdo
        String cleanTag = ChatColor.stripColor(translateHexColors(tag));
        
        // Verificar se o conteúdo limpo está dentro dos limites mais flexíveis
        if (!TAG_CLEAN_PATTERN.matcher(cleanTag).matches()) {
            return false;
        }
        
        // Verificar se a tag expandida não é excessivamente longa (limite de 1000 caracteres)
        String expandedTag = translateColors(tag);
        return expandedTag.length() <= 1000;
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

    public boolean isTagTooLong(String tag) {
        if (tag == null) return false;
        String expandedTag = translateColors(tag);
        return expandedTag.length() > 1000;
    }

    public int getExpandedTagLength(String tag) {
        if (tag == null) return 0;
        return translateColors(tag).length();
    }

    public String getCleanTag(String tag) {
        if (tag == null) return "";
        return ChatColor.stripColor(translateHexColors(tag));
    }
}
