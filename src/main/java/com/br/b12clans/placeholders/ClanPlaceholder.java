package com.br.b12clans.placeholders;

import com.br.b12clans.Main;
import com.br.b12clans.managers.ClanManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

public class ClanPlaceholder extends PlaceholderExpansion {

    private final Main plugin;
    private final ClanManager clanManager;

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
                return clanManager.formatDisplayName(clanManager.getPlayerClanName(player.getUniqueId()));
            case "has_clan":
                return clanManager.getPlayerClan(player.getUniqueId()) != null ? "Sim" : "Não";
            case "kdr":
                return getPlayerKDR(player);
            case "kills":
                return String.valueOf(getPlayerKills(player));
            case "deaths":
                return String.valueOf(getPlayerDeaths(player));
            case "description":
                return getPlayerClanDescription(player);
            case "bank_balance":
            case "bank":
                return getPlayerClanBankBalance(player);
            default:
                return null;
        }
    }

    private String getPlayerKDR(Player player) {
        int[] kdr = plugin.getDatabaseManager().getPlayerKDR(player.getUniqueId());
        int kills = kdr[0];
        int deaths = kdr[1];

        if (deaths == 0) {
            return kills > 0 ? String.valueOf(kills) : "0.00";
        }

        double ratio = (double) kills / deaths;
        return String.format("%.2f", ratio);
    }

    private int getPlayerKills(Player player) {
        int[] kdr = plugin.getDatabaseManager().getPlayerKDR(player.getUniqueId());
        return kdr[0];
    }

    private int getPlayerDeaths(Player player) {
        int[] kdr = plugin.getDatabaseManager().getPlayerKDR(player.getUniqueId());
        return kdr[1];
    }

    private String getPlayerClanDescription(Player player) {
        com.br.b12clans.models.Clan clan = clanManager.getPlayerClan(player.getUniqueId());
        if (clan == null) {
            return "";
        }

        String description = plugin.getDatabaseManager().getClanDescription(clan.getId());
        if (description == null || description.trim().isEmpty()) {
            return "Sem descrição";
        }

        return clanManager.translateColors(description);
    }

    private String getPlayerClanBankBalance(Player player) {
        com.br.b12clans.models.Clan clan = clanManager.getPlayerClan(player.getUniqueId());
        if (clan == null) {
            return "0.00";
        }

        double balance = plugin.getDatabaseManager().getClanBankBalance(clan.getId());
        return String.format("%.2f", balance);
    }
}
