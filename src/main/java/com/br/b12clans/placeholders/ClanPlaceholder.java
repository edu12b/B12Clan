package com.br.b12clans.placeholders;

import com.br.b12clans.Main;
import com.br.b12clans.managers.ClanManager;
import com.br.b12clans.models.PlayerData; // <-- IMPORTAÇÃO ADICIONADA
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

public class ClanPlaceholder extends PlaceholderExpansion {

    private final Main plugin;
    private final ClanManager clanManager;

    public ClanPlaceholder(Main plugin) {
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

        // Pega os dados do jogador diretamente do cache
        PlayerData cachedData = clanManager.getPlayerData(player.getUniqueId());

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

            // ### LÓGICA MODIFICADA ###
            case "kdr":
                if (cachedData == null) return "0.00";
                return String.format("%.2f", cachedData.getKdr()); // Lê do cache
            case "kills":
                if (cachedData == null) return "0";
                return String.valueOf(cachedData.getKills()); // Lê do cache
            case "deaths":
                if (cachedData == null) return "0";
                return String.valueOf(cachedData.getDeaths()); // Lê do cache

            // Estes ainda usam o banco de dados, mas podem ser otimizados depois
            case "description":
                return getPlayerClanDescription(player);
            case "bank_balance":
            case "bank":
                return getPlayerClanBankBalance(player);
            default:
                return null;
        }
    }

    // Os métodos getPlayerKDR, getPlayerKills e getPlayerDeaths podem ser REMOVIDOS
    // pois a lógica deles agora está no cache.

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