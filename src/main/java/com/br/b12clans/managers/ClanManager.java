// ARQUIVO: src/main/java/com/br/b12clans/managers/ClanManager.java
package com.br.b12clans.managers;

import com.br.b12clans.Main;
import com.br.b12clans.database.DatabaseManager;
import com.br.b12clans.models.Clan;
import com.br.b12clans.models.PlayerData;
import com.br.b12clans.utils.MessagesManager;
import com.br.b12clans.utils.SmallTextConverter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class ClanManager {

    private final Main plugin;
    private final MessagesManager messages;
    private final DatabaseManager databaseManager; // <-- Adicionado para fácil acesso
    private final Map<UUID, Clan> playerClans;
    private final Map<UUID, PlayerData> playerDataCache;
    private final Map<UUID, PendingRequest> pendingInvites;
    private final Map<Integer, PendingRequest> pendingAllianceRequests;
    private final Map<UUID, Long> pendingDeletions;
    private final Map<Integer, Boolean> friendlyFireDisabledCache = new ConcurrentHashMap<>();


    // ##### NOVOS CACHES PARA RELACIONAMENTOS #####
    private final Map<Integer, List<Integer>> allyCache = new ConcurrentHashMap<>();
    private final Map<Integer, List<Integer>> rivalCache = new ConcurrentHashMap<>();

    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{2,32}$");
    private static final Pattern TAG_CLEAN_PATTERN = Pattern.compile("^[a-zA-Z0-9\\[\\]\\(\\)-_&]{1,16}$");

    public void invalidateFriendlyFireCache(int clanId) {
        friendlyFireDisabledCache.remove(clanId);
    }

    public CompletableFuture<Boolean> isFriendlyFireDisabled(int clanId) {
        if (friendlyFireDisabledCache.containsKey(clanId)) {
            return CompletableFuture.completedFuture(friendlyFireDisabledCache.get(clanId));
        }
        return databaseManager.isFriendlyFireDisabledAsync(clanId).thenApply(isDisabled -> {
            friendlyFireDisabledCache.put(clanId, isDisabled);
            return isDisabled;
        });
    }
    public Boolean isFriendlyFireDisabledFromCache(int clanId) {
        // Retorna o valor do cache se existir, ou null se não estiver no cache.
        return friendlyFireDisabledCache.get(clanId);
    }

    public List<Integer> getClanAlliesFromCache(int clanId) {
        // Retorna a lista de aliados do cache se existir, ou null.
        return allyCache.get(clanId);
    }

    private static class PendingRequest {
        final int sourceId;
        final long timestamp;
        PendingRequest(int sourceId) {
            this.sourceId = sourceId;
            this.timestamp = System.currentTimeMillis();
        }
    }

    public ClanManager(Main plugin) {
        this.plugin = plugin;
        this.messages = plugin.getMessagesManager();
        this.databaseManager = plugin.getDatabaseManager(); // <-- Inicialização
        this.playerClans = new ConcurrentHashMap<>();
        this.playerDataCache = new ConcurrentHashMap<>();
        this.pendingInvites = new ConcurrentHashMap<>();
        this.pendingAllianceRequests = new ConcurrentHashMap<>();
        this.pendingDeletions = new ConcurrentHashMap<>();
    }

    // ##### NOVO MÉTODO PARA INVALIDAR O CACHE #####
    public void invalidateRelationshipCache(int clanId) {
        allyCache.remove(clanId);
        rivalCache.remove(clanId);
        plugin.getLogger().info("Cache de relacionamento invalidado para o clã ID: " + clanId);
    }

    // ##### NOVO MÉTODO PARA BUSCAR ALIADOS COM CACHE #####
    public CompletableFuture<List<Integer>> getClanAlliesAsync(int clanId) {
        if (allyCache.containsKey(clanId)) {
            return CompletableFuture.completedFuture(allyCache.get(clanId));
        }
        return databaseManager.getAllAllyIdsAsync(clanId).thenApply(allyIds -> {
            allyCache.put(clanId, allyIds);
            return allyIds;
        });
    }

    // ##### NOVO MÉTODO PARA BUSCAR RIVAIS COM CACHE #####
    public CompletableFuture<List<Integer>> getClanRivalsAsync(int clanId) {
        if (rivalCache.containsKey(clanId)) {
            return CompletableFuture.completedFuture(rivalCache.get(clanId));
        }
        return databaseManager.getAllRivalIdsAsync(clanId).thenApply(rivalIds -> {
            rivalCache.put(clanId, rivalIds);
            return rivalIds;
        });
    }

    public void cleanupExpiredRequests() {
        // ... (lógica de limpeza da sugestão anterior)
        long expirationTime = TimeUnit.MINUTES.toMillis(10);
        long now = System.currentTimeMillis();

        pendingInvites.entrySet().removeIf(entry -> (now - entry.getValue().timestamp) > expirationTime);
        pendingAllianceRequests.entrySet().removeIf(entry -> (now - entry.getValue().timestamp) > expirationTime);
    }

    // O restante da classe ClanManager continua igual...
    // ...
    // (addInvite, getPendingInvite, etc.)
    public void addInvite(UUID invitedPlayer, int clanId) {
        pendingInvites.put(invitedPlayer, new PendingRequest(clanId));
    }

    public Integer getPendingInvite(UUID invitedPlayer) {
        PendingRequest request = pendingInvites.get(invitedPlayer);
        return (request != null) ? request.sourceId : null;
    }

    public void removeInvite(UUID invitedPlayer) {
        pendingInvites.remove(invitedPlayer);
    }

    public void addAllianceRequest(int targetClanId, int sourceClanId) {
        pendingAllianceRequests.put(targetClanId, new PendingRequest(sourceClanId));
    }

    public Integer getPendingAllianceRequest(int targetClanId) {
        PendingRequest request = pendingAllianceRequests.get(targetClanId);
        return (request != null) ? request.sourceId : null;
    }

    public void removeAllianceRequest(int targetClanId) {
        pendingAllianceRequests.remove(targetClanId);
    }
    public void addPendingDeletion(UUID playerUuid) {
        pendingDeletions.put(playerUuid, System.currentTimeMillis());
    }

    public boolean hasPendingDeletion(UUID playerUuid) {
        if (!pendingDeletions.containsKey(playerUuid)) {
            return false;
        }
        long timeOfRequest = pendingDeletions.get(playerUuid);
        return (System.currentTimeMillis() - timeOfRequest) < 30000;
    }

    public void removePendingDeletion(UUID playerUuid) {
        pendingDeletions.remove(playerUuid);
    }

    public PlayerData getPlayerData(UUID playerUuid) {
        return playerDataCache.get(playerUuid);
    }

    public void cachePlayerData(UUID playerUuid, PlayerData data) {
        playerDataCache.put(playerUuid, data);
    }

    public void uncachePlayerData(UUID playerUuid) {
        playerDataCache.remove(playerUuid);
    }
    public void broadcastToClan(Clan clan, String messageKey, String... placeholders) {
        if (clan == null) return;
        String rawMessage = messages.getMessage(messageKey, placeholders);
        String coloredMessage = translateColors(rawMessage);

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            Clan playerClan = getPlayerClan(onlinePlayer.getUniqueId());
            if (playerClan != null && playerClan.getId() == clan.getId()) {
                onlinePlayer.sendMessage(coloredMessage);
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

    public CompletableFuture<Clan> getClanById(int clanId) {
        return CompletableFuture.supplyAsync(() -> plugin.getDatabaseManager().getClanById(clanId), plugin.getThreadPool());
    }

    public Clan getClanByTag(String tag) {
        return plugin.getDatabaseManager().getClanByTag(tag);
    }

    public CompletableFuture<Clan> getClanByTagAsync(String tag) {
        return CompletableFuture.supplyAsync(() -> plugin.getDatabaseManager().getClanByTag(tag), plugin.getThreadPool());
    }

    public boolean isValidClanName(String name) {
        return name != null && NAME_PATTERN.matcher(name).matches();
    }

    public boolean isValidClanTag(String tag) {
        if (tag == null || tag.trim().isEmpty()) return false;
        String cleanTag = ChatColor.stripColor(translateColors(tag));
        if (!TAG_CLEAN_PATTERN.matcher(cleanTag).matches()) {
            return false;
        }
        String expandedTag = translateColors(tag);
        return expandedTag.length() <= 1000;
    }

    public String translateColors(String message) {
        return plugin.getMessagesManager().translateColors(message);
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

    public String getPlayerClanTag(UUID playerUuid) {
        Clan clan = getPlayerClan(playerUuid);
        return clan != null ? translateColors(clan.getTag()) : "";
    }

    public String getPlayerClanName(UUID playerUuid) {
        Clan clan = getPlayerClan(playerUuid);
        return clan != null ? formatDisplayName(clan.getName()) : "";
    }

    public String getCleanTag(String tag) {
        if (tag == null) return "";
        return ChatColor.stripColor(translateColors(tag));
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
    public String getCommandFromAlias(String alias) {
        if (alias.equalsIgnoreCase("ally")) {
            return "ally";
        }
        if (alias.equalsIgnoreCase("rival")) {
            return "rival";
        }
        return "";
    }
    public boolean isTagTooLong(String tag) {
        if (tag == null) return false;
        String expandedTag = translateColors(tag);
        return expandedTag.length() > 1000;
    }

    private String getPlayerRole(UUID playerUuid) {
        PlayerData data = getPlayerData(playerUuid);
        // Se o cache existir e tiver um cargo, retorna o cargo.
        if (data != null && data.getRole() != null) {
            return data.getRole();
        }
        // Se o jogador não tem clã ou o cache ainda não carregou, retorna um valor padrão.
        return "MEMBER";
    }


    private String[] getBracketsForRole(String role) {
        String leftBracket, rightBracket;
        // Agora usamos o cargo exato do banco ('OWNER', 'VICE_LEADER', etc.)
        switch (role.toUpperCase()) {
            case "OWNER":
            case "VICE_LEADER":
                leftBracket = plugin.getConfig().getString("settings.placeholder-colors.leader.left-bracket", "&4[");
                rightBracket = plugin.getConfig().getString("settings.placeholder-colors.leader.right-bracket", "&4]");
                break;
            default: // ADMIN e MEMBER cairão aqui
                leftBracket = plugin.getConfig().getString("settings.placeholder-colors.member.left-bracket", "&7[");
                rightBracket = plugin.getConfig().getString("settings.placeholder-colors.member.right-bracket", "&7]");
                break;
        }
        // O fallback para 'default' é uma boa prática caso algo dê errado.
        if (leftBracket == null || rightBracket == null) {
            leftBracket = plugin.getConfig().getString("settings.placeholder-colors.default.left-bracket", "&8[");
            rightBracket = plugin.getConfig().getString("settings.placeholder-colors.default.right-bracket", "&8]");
        }
        return new String[]{translateColors(leftBracket), translateColors(rightBracket)};
    }
}