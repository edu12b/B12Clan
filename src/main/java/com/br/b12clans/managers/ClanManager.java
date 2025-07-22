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
    private final MessagesManager messages;
    private final Map<UUID, Clan> playerClans;
    private final Map<UUID, Integer> pendingInvites;
    private final Map<Integer, Integer> pendingAllianceRequests; // clanId_alvo -> clanId_autor

    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{2,32}$");
    private static final Pattern TAG_CLEAN_PATTERN = Pattern.compile("^[a-zA-Z0-9\\[\\]\\(\\)-_&]{1,16}$");

    public ClanManager(Main plugin) {
        this.plugin = plugin;
        this.messages = plugin.getMessagesManager();
        this.playerClans = new ConcurrentHashMap<>();
        this.pendingInvites = new ConcurrentHashMap<>();
        this.pendingAllianceRequests = new ConcurrentHashMap<>(); // <-- INICIALIZAÇÃO ADICIONADA
    }

    // ##### NOVOS MÉTODOS PARA PEDIDOS DE ALIANÇA #####
    public void addAllianceRequest(int targetClanId, int sourceClanId) {
        pendingAllianceRequests.put(targetClanId, sourceClanId);
    }

    public Integer getPendingAllianceRequest(int targetClanId) {
        return pendingAllianceRequests.get(targetClanId);
    }

    public void removeAllianceRequest(int targetClanId) {
        pendingAllianceRequests.remove(targetClanId);
    }
    // #################################################

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

    public Integer getPendingInvite(UUID invitedPlayer) {
        return pendingInvites.get(invitedPlayer);
    }

    public void removeInvite(UUID invitedPlayer) {
        pendingInvites.remove(invitedPlayer);
    }

    public CompletableFuture<Clan> getClanById(int clanId) {
        return CompletableFuture.supplyAsync(() -> plugin.getDatabaseManager().getClanById(clanId), plugin.getThreadPool());
    }

    public Clan getClanByTag(String tag) {
        // Este método busca de forma síncrona, útil em alguns casos específicos
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
        // Este é um método simples para ajudar o onTabComplete a saber
        // se o jogador digitou /clan ally ou /clan rival.
        // No futuro, isso pode ser melhorado se você adicionar mais aliases.
        if (alias.equalsIgnoreCase("ally")) {
            return "ally";
        }
        if (alias.equalsIgnoreCase("rival")) {
            return "rival";
        }
        return ""; // Retorna vazio se não for um alias conhecido
    }

    private String getPlayerRole(UUID playerUuid) {
        Clan clan = getPlayerClan(playerUuid);
        if (clan == null) return "MEMBER";
        if (clan.getOwnerUuid().equals(playerUuid)) return "LEADER";

        // Esta é uma operação de banco de dados, o ideal seria fazê-la de forma assíncrona
        // Mas para a lógica de cores do placeholder, uma chamada síncrona pode ser aceitável
        // se não for chamada com muita frequência (ex: a cada mensagem no chat).
        String dbRole = plugin.getDatabaseManager().getMemberRole(clan.getId(), playerUuid);
        if ("ADMIN".equalsIgnoreCase(dbRole) || "VICE_LEADER".equalsIgnoreCase(dbRole)) return "LEADER";

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
}