package com.br.b12clans.placeholders;

import com.br.b12clans.B12Clans;
import com.br.b12clans.managers.ClanManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

public class ClanPlaceholder extends PlaceholderExpansion {
    
    private final B12Clans plugin;
    private final ClanManager clanManager;
    
    public ClanPlaceholder(B12Clans plugin) {
        this.plugin = plugin;
        this.clanManager = plugin.getClanManager();
    }
    
    @Override
    public String getIdentifier() {
        return "b12clans";
    }
    
    @Override
    public String getAuthor() {
        return "Eduardo12B";
    }
    
    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }
    
    @Override
    public boolean persist() {
        return true;
    }
    
    @Override
    public String onPlaceholderRequest(Player player, String params) {
        if (player == null) {
            return "";
        }
        
        switch (params.toLowerCase()) {
            case "tag":
                return clanManager.getPlayerClanTag(player.getUniqueId());
            case "name":
                return clanManager.getPlayerClanName(player.getUniqueId());
            case "has_clan":
                return clanManager.getPlayerClan(player.getUniqueId()) != null ? "true" : "false";
            default:
                return null;
        }
    }
}
