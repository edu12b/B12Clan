// ARQUIVO NOVO
package com.br.b12clans.commands.subcommands;

import com.br.b12clans.Main;
import com.br.b12clans.managers.ClanManager;
import com.br.b12clans.models.Clan;
import com.br.b12clans.utils.MessagesManager;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class AjudaCommand implements SubCommand {

    private final Main plugin;
    private final MessagesManager messages;
    private final ClanManager clanManager;

    public AjudaCommand(Main plugin) {
        this.plugin = plugin;
        this.messages = plugin.getMessagesManager();
        this.clanManager = plugin.getClanManager();
    }

    @Override
    public String getName() {
        return "ajuda";
    }

    @Override
    public String getPermission() {
        return null; // Comando de ajuda é livre para todos.
    }

    @Override
    public void execute(Player player, String[] args) {
        messages.sendMessage(player, "help-header");

        Clan clan = clanManager.getPlayerClan(player.getUniqueId());

        // Comandos que todos podem ver, estando em um clã ou não.
        messages.sendMessage(player, "help-line-create");
        messages.sendMessage(player, "help-line-info");
        messages.sendMessage(player, "help-line-ver");

        if (clan == null) {
            // Comandos para quem NÃO está em um clã.
            messages.sendMessage(player, "help-line-convite-accept");
            messages.sendMessage(player, "help-line-convite-deny");
            messages.sendMessage(player, "help-footer");
        } else {
            // Comandos para quem ESTÁ em um clã.
            // Precisamos buscar o cargo do jogador para mostrar a ajuda correta.
            plugin.getDatabaseManager().getMemberRoleAsync(clan.getId(), player.getUniqueId())
                    .thenAccept(role -> {
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            // --- Seção de Membros ---
                            messages.sendMessage(player, "help-section-member");
                            messages.sendMessage(player, "help-line-home");
                            messages.sendMessage(player, "help-line-kdr");
                            messages.sendMessage(player, "help-line-sair");
                            messages.sendMessage(player, "help-line-banco-saldo");
                            messages.sendMessage(player, "help-line-banco-depositar");

                            // --- Seção de Vice-Líderes/Admins ---
                            if (role.equals("OWNER") || role.equals("VICE_LEADER") || role.equals("ADMIN")) {
                                messages.sendMessage(player, "help-section-vice-leader");
                                messages.sendMessage(player, "help-line-convite-add");
                                messages.sendMessage(player, "help-line-expulsar");
                                messages.sendMessage(player, "help-line-titulo");
                                messages.sendMessage(player, "help-line-description");
                                messages.sendMessage(player, "help-line-home-set");
                                messages.sendMessage(player, "help-line-ally");
                                messages.sendMessage(player, "help-line-rival");
                                messages.sendMessage(player, "help-line-banco-sacar");
                            }

                            // --- Seção de Líderes ---
                            if (role.equals("OWNER")) {
                                messages.sendMessage(player, "help-section-leader");
                                messages.sendMessage(player, "help-line-cargo-promover");
                                messages.sendMessage(player, "help-line-cargo-rebaixar");
                                messages.sendMessage(player, "help-line-deletar");
                                messages.sendMessage(player, "help-line-config-tag");
                                messages.sendMessage(player, "help-line-home-clear");
                                messages.sendMessage(player, "help-line-config-fee");
                                messages.sendMessage(player, "help-line-config-banner");
                            }

                            messages.sendMessage(player, "help-footer");
                        });
                    });
        }
    }

    @Override
    public List<String> onTabComplete(Player player, String[] args) {
        return Collections.emptyList();
    }
}