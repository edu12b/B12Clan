// ARQUIVO: src/main/java/com/br/b12clans/commands/subcommands/SairCommand.java
package com.br.b12clans.commands.subcommands;

import com.br.b12clans.Main;
import com.br.b12clans.managers.ClanManager;
import com.br.b12clans.models.Clan;
import com.br.b12clans.utils.MessagesManager;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class SairCommand implements SubCommand {

    private final Main plugin;
    private final ClanManager clanManager;
    private final MessagesManager messages;

    public SairCommand(Main plugin) {
        this.plugin = plugin;
        this.clanManager = plugin.getClanManager();
        this.messages = plugin.getMessagesManager();
    }

    @Override
    public String getName() {
        return "sair";
    }

    @Override
    public String getPermission() {
        return null; // Qualquer jogador pode usar este comando.
    }

    @Override
    public void execute(Player player, String[] args) {
        Clan playerClan = clanManager.getPlayerClan(player.getUniqueId());
        if (playerClan == null) {
            messages.sendMessage(player, "no-clan");
            return;
        }

        // Verifica se o jogador é o dono do clã
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            String role = plugin.getDatabaseManager().getMemberRole(playerClan.getId(), player.getUniqueId());

            if (role != null && role.equals("OWNER")) {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    messages.sendMessage(player, "owner-cannot-leave");
                    messages.sendMessage(player, "owner-must-delete");
                });
                return;
            }

            // Procede com a saída do clã
            boolean success = plugin.getDatabaseManager().removeClanMember(playerClan.getId(), player.getUniqueId());

            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (success) {
                    clanManager.unloadPlayerClan(player.getUniqueId());
                    messages.sendMessage(player, "you-left-clan", "%clan_name%", playerClan.getName());

                    // Notifica os outros membros
                    clanManager.broadcastToClan(playerClan, "member-left", "%player_name%", player.getName());

                    // Lógica do Discord
                    if (plugin.getConfig().getBoolean("discord.enabled", false)) {
                        plugin.getDiscordManager().onMemberLeft(playerClan, player);
                    }
                } else {
                    messages.sendMessage(player, "error-leaving-clan");
                }
            });
        });
    }

    @Override
    public List<String> onTabComplete(Player player, String[] args) {
        // O comando /clan sair não tem argumentos
        return Collections.emptyList();
    }
}