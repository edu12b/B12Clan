// ARQUIVO: src/main/java/com/br/b12clans/commands/subcommands/DeletarCommand.java
package com.br.b12clans.commands.subcommands;

import com.br.b12clans.Main;
import com.br.b12clans.managers.ClanManager;
import com.br.b12clans.models.Clan;
import com.br.b12clans.utils.MessagesManager;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class DeletarCommand implements SubCommand {

    private final Main plugin;
    private final ClanManager clanManager;
    private final MessagesManager messages;

    public DeletarCommand(Main plugin) {
        this.plugin = plugin;
        this.clanManager = plugin.getClanManager();
        this.messages = plugin.getMessagesManager();
    }

    @Override
    public String getName() {
        return "deletar";
    }

    @Override
    public String getPermission() {
        return null; // Apenas o dono pode deletar, verificado internamente.
    }

    @Override
    public void execute(Player player, String[] args) {
        Clan clan = clanManager.getPlayerClan(player.getUniqueId());
        if (clan == null) {
            messages.sendMessage(player, "no-clan");
            return;
        }

        // A verificação de cargo e a exclusão são feitas de forma assíncrona
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            String role = plugin.getDatabaseManager().getMemberRole(clan.getId(), player.getUniqueId());

            // Apenas o DONO pode deletar o clã
            if (role == null || !role.equals("OWNER")) {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    messages.sendMessage(player, "delete-no-permission");
                });
                return;
            }

            // Procede com a exclusão
            boolean success = plugin.getDatabaseManager().deleteClan(clan.getId());

            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (success) {
                    // Descarrega o clã da memória para todos os membros online
                    clanManager.unloadClanFromAllMembers(clan);
                    messages.sendMessage(player, "clan-deleted-success", "target_%clan%", clan.getName());

                    // Lógica do Discord
                    if (plugin.getConfig().getBoolean("discord.enabled", false)) {
                        plugin.getDiscordManager().onClanDisbanded(clan);
                    }
                } else {
                    messages.sendMessage(player, "error-deleting-clan");
                }
            });
        });
    }

    @Override
    public List<String> onTabComplete(Player player, String[] args) {
        // O comando /clan deletar não tem argumentos
        return Collections.emptyList();
    }
}