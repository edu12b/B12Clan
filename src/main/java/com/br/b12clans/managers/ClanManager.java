// ARQUIVO: src/main/java/com/br/b12clans/managers/ClanManager.java
package com.br.b12clans.managers;

import com.br.b12clans.Main;
import com.br.b12clans.models.Clan;
import com.br.b12clans.utils.MessagesManager;
import com.br.b12clans.utils.SmallTextConverter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class ClanManager {

    private final Main plugin;
    private final MessagesManager messages; // <-- LINHA ADICIONADA
    private final Map<UUID, Clan> playerClans;
    private final Map<UUID, Integer> pendingInvites;

    private static final Pattern HEX_PATTERN = Pattern.compile("&#[a-fA-F0-9]{6}");
    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{2,32}$");
    private static final Pattern TAG_CLEAN_PATTERN = Pattern.compile("^[a-zA-Z0-9\\[\\]\\(\\)-_&]{1,16}$");

    public ClanManager(Main plugin) {
        this.plugin = plugin;
        this.messages = plugin.getMessagesManager(); // <-- LINHA ADICIONADA
        this.playerClans = new ConcurrentHashMap<>();
        this.pendingInvites = new ConcurrentHashMap<>();
    }

    public void broadcastToClan(Clan clan, String messageKey, String... placeholders) {
        if (clan == null) return;
        String messageToSend = messages.getMessage(messageKey, placeholders);
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            Clan playerClan = getPlayerClan(onlinePlayer.getUniqueId());
            if (playerClan != null && playerClan.getId() == clan.getId()) {
                onlinePlayer.sendMessage(messageToSend);
            }
        }
    }

    public void unloadClanFromAllMembers(Clan clan) {
        if (clan == null) return;
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            Clan playerClan = getPlayerClan(onlinePlayer.getUniqueId());
            if (playerClan != null && playerClan.getId() == clan.getId()) {
                unloadPlayerClan(onlinePlayer.getUniqueId());
            }
        }
    }

    public void addInvite(UUID invitedPlayer, int clanId) {
        pendingInvites.put(invitedPlayer, clanId);
    }

    // Método corrigido para buscar convite por nome do clã
    public Integer getInvite(UUID playerUUID, String clanName) {
        Integer clanId = pendingInvites.get(playerUUID);
        if (clanId != null) {
            Clan clan = plugin.getDatabaseManager().getClanById(clanId);
            if (clan != null && clan.getName().equalsIgnoreCase(clanName)) {
                return clanId;
            }
        }
        return null;
    }

    // Método para o tab-complete
    public List<String> getPendingInvitesForPlayer(UUID playerUUID) {
        List<String> clanNames = new ArrayList<>();
        Integer clanId = pendingInvites.get(playerUUID);
        if (clanId != null) {
            Clan clan = plugin.getDatabaseManager().getClanById(clanId);
            if (clan != null) {
                clanNames.add(clan.getName());
            }
        }
        return clanNames;
    }

    public void removeInvite(UUID invitedPlayer, String clanName) {
        // Lógica para remover o convite específico
        pendingInvites.remove(invitedPlayer);
    }

    public Integer getPendingInvite(UUID invitedPlayer) {
        return pendingInvites.get(invitedPlayer);
    }

    public void removeInvite(UUID invitedPlayer) {
        pendingInvites.remove(invitedPlayer);
    }

    public CompletableFuture<Clan> getClanById(int clanId) {
        return CompletableFuture.supplyAsync(() -> plugin.getDatabaseManager().getClanById(clanId));
    }

    public void broadcastToClan(int clanId, String message) {
        // Este método pode ser removido ou atualizado para usar o novo sistema.
        // Por enquanto, vamos deixá-lo vazio para não causar erros.
    }

    public boolean isValidClanName(String name) {
        return name != null && NAME_PATTERN.matcher(name).matches();
    }

    public boolean isValidClanTag(String tag) {
        if (tag == null || tag.trim().isEmpty()) return false;
        String cleanTag = ChatColor.stripColor(translateHexColors(tag));
        if (!TAG_CLEAN_PATTERN.matcher(cleanTag).matches()) {
            return false;
        }
        String expandedTag = translateColors(tag);
        return expandedTag.length() <= 1000;
    }

    public String translateHexColors(String message) {
        if (message == null) return null;
        return HEX_PATTERN.matcher(message).replaceAll(match -> {
            String hex = match.group().substring(2);
            StringBuilder magic = new StringBuilder("§x");
            for (char c : hex.toCharArray()) {
                magic.append("§").append(c);
            }
            return magic.toString();
        });
    }

    public String translateColors(String message) {
        if (message == null) return null;
        String hexTranslated = HEX_PATTERN.matcher(message).replaceAll(match -> {
            String hex = match.group().substring(2);
            StringBuilder magic = new StringBuilder("§x");
            for (char c : hex.toCharArray()) {
                magic.append("§").append(Character.toLowerCase(c));
            }
            return magic.toString();
        });
        return ChatColor.translateAlternateColorCodes('&', hexTranslated);
    }

    public String formatDisplayName(String name) {
        if (name == null) return null;
        return name.replace("_", " ");
    }

    public void loadPlayerClan(UUID playerUuid) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            Clan clan = plugin.getDatabaseManager().getClanByPlayer(playerUuid);
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
        return clan != null ? formatDisplayName(clan.getName()) : "";
    }

    public boolean isTagTooLong(String tag) {
        if (tag == null) return false;
        return getExpandedTagLength(tag) > 1000;
    }

    public int getExpandedTagLength(String tag) {
        if (tag == null) return 0;
        return translateColors(tag).length();
    }

    public String getCleanTag(String tag) {
        if (tag == null) return "";
        return ChatColor.stripColor(translateHexColors(tag));
    }

    public String getPlayerClanTagWithLabels(UUID playerUuid) {
        Clan clan = getPlayerClan(playerUuid);
        if (clan == null) return "";
        String role = getPlayerRole(playerUuid);
        String[] brackets = getBracketsForRole(role);
        String translatedTag = translateColors(clan.getTag());
        return brackets[0] + translatedTag + brackets[1];
    }

    public String getPlayerClanTagSmall(UUID playerUuid) {
        Clan clan = getPlayerClan(playerUuid);
        if (clan == null) return "";
        String translatedTag = translateColors(clan.getTag());
        return SmallTextConverter.toSmallCapsPreservingColors(translatedTag);
    }

    public String getPlayerClanTagSmallWithLabels(UUID playerUuid) {
        Clan clan = getPlayerClan(playerUuid);
        if (clan == null) return "";
        String role = getPlayerRole(playerUuid);
        String[] brackets = getBracketsForRole(role);
        String translatedTag = translateColors(clan.getTag());
        String smallTag = SmallTextConverter.toSmallCapsPreservingColors(translatedTag);
        return brackets[0] + smallTag + brackets[1];
    }

    private String getPlayerRole(UUID playerUuid) {
        Clan clan = getPlayerClan(playerUuid);
        if (clan == null) return "MEMBER";
        if (clan.getOwnerUuid().equals(playerUuid)) return "LEADER";
        String dbRole = plugin.getDatabaseManager().getMemberRole(clan.getId(), playerUuid);
        if ("ADMIN".equalsIgnoreCase(dbRole)) return "LEADER";
        return "MEMBER";
    }

    private String[] getBracketsForRole(String role) {
        String leftBracket, rightBracket;
        switch (role != null ? role.toUpperCase() : "MEMBER") {
            case "LEADER":
                leftBracket = plugin.getConfig().getString("settings.placeholder-colors.leader.left-bracket", "&4[");
                rightBracket = plugin.getConfig().getString("settings.placeholder-colors.leader.right-bracket", "&4]");
                break;
            default:
                leftBracket = plugin.getConfig().getString("settings.placeholder-colors.member.left-bracket", "&7[");
                rightBracket = plugin.getConfig().getString("settings.placeholder-colors.member.right-bracket", "&7]");
                break;
        }
        if (leftBracket == null || rightBracket == null) {
            leftBracket = plugin.getConfig().getString("settings.placeholder-colors.default.left-bracket", "&8[");
            rightBracket = plugin.getConfig().getString("settings.placeholder-colors.default.right-bracket", "&8]");
        }
        return new String[]{translateColors(leftBracket), translateColors(rightBracket)};
    }

    public void debugColorTranslation(String input) {
        plugin.getLogger().info("=== DEBUG CORES ===");
        plugin.getLogger().info("Input: " + input);
        plugin.getLogger().info("Hex Translated: " + translateHexColors(input));
        plugin.getLogger().info("Full Translated: " + translateColors(input));
        plugin.getLogger().info("Clean Tag: " + getCleanTag(input));
        plugin.getLogger().info("==================");
    }
}