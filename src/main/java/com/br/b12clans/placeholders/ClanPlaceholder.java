package com.br.b12clans.placeholders;

import com.br.b12clans.Main;
import com.br.b12clans.managers.ClanManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

public class ClanPlaceholder extends PlaceholderExpansion {

    private final Main plugin;
    private final ClanManager clanManager;

    // Construtor corrigido para receber as duas dependências
    public ClanPlaceholder(Main plugin, ClanManager clanManager) {
        this.plugin = plugin;
        this.clanManager = clanManager;
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
            case "tag_label":
            case "tag_labels":
                return clanManager.getPlayerClanTagWithLabels(player.getUniqueId());
            case "tag_small":
                return clanManager.getPlayerClanTagSmall(player.getUniqueId());
            case "tag_small_label":
            case "tag_small_labels":
                return clanManager.getPlayerClanTagSmallWithLabels(player.getUniqueId());
            case "name":
                return clanManager.getPlayerClanName(player.getUniqueId());
            case "has_clan":
                return clanManager.getPlayerClan(player.getUniqueId()) != null ? "Sim" : "Não";
            default:
                return null;
        }
    }
}
